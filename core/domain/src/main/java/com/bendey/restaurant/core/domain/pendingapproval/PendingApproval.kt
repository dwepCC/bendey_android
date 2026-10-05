package com.bendey.restaurant.core.domain.pendingapproval

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

/** Item de una ronda del cliente (QR) que espera revision. */
data class PendingApprovalItem(
    val id: Int,
    val productName: String,
    val quantity: Double,
    val notes: String?,
    /** Modificadores ya formateados ("+ Sin cebolla", "Grande"...), uno por linea. */
    val modifierLines: List<String>,
    val preparationArea: String?,
)

/** Ronda pedida por un cliente desde la carta digital que espera revision del personal (R10.1). */
data class PendingApprovalOrder(
    val orderId: Int,
    val orderNumber: Int,
    val sessionId: Int,
    val tableName: String,
    /** `dine_in`, `takeaway`, `delivery`... tal cual lo manda el backend. */
    val orderType: String,
    val notes: String?,
    /** ISO-8601 con zona (el backend lo manda en UTC). Vacio si no vino. */
    val createdAt: String,
    val customerName: String?,
    val customerPhone: String?,
    val total: Double,
    val items: List<PendingApprovalItem>,
)

interface PendingApprovalRepository {
    suspend fun listPending(): AppResult<List<PendingApprovalOrder>>
    suspend fun approve(orderId: Int): AppResult<Unit>
    suspend fun reject(orderId: Int, reason: String): AppResult<Unit>
}

/**
 * Decisiones PURAS de "pedido por revisar" (R10.1/R10.2). Mismas reglas y mismos textos que
 * `src/utils/pendingApproval.ts` y la cola de Tauri: no cambies uno sin cambiar el otro.
 *
 * Contrato (backend): `restaurant.order.pending_approval` con `{session_id, order_id, table_name?,
 * items_count}` llega a caja/administracion/cocina de la sucursal cuando un pedido del QR entra como
 * `por_aprobar`. `menu.order.created` NO suena mientras sea por_aprobar (regla R7).
 */
object PendingApprovalLogic {
    const val EVENT = "restaurant.order.pending_approval"

    /** Respaldo lento: el evento en tiempo real es la via normal; esto solo cubre un evento perdido. */
    const val BACKUP_REFRESH_MS = 60_000L

    const val PERM_ADMIN = RestaurantPermissions.PERM_ADMIN
    const val PERM_CHARGE = RestaurantPermissions.PERM_ORDERS_CHARGE
    const val PERM_KITCHEN = RestaurantPermissions.PERM_COMANDAS
    const val PERM_TABLES_OPEN = RestaurantPermissions.PERM_MESA
    const val PERM_TABLES_VIEW = RestaurantPermissions.PERM_SALAS

    private fun any(permissions: List<String>?, vararg wanted: String): Boolean =
        !permissions.isNullOrEmpty() && wanted.any { it in permissions }

    /** Ver la cola (backend: t.v | o.ch | k.v); el administrador (s.m) siempre. */
    fun canView(permissions: List<String>?): Boolean =
        any(permissions, PERM_ADMIN, PERM_TABLES_VIEW, PERM_CHARGE, PERM_KITCHEN)

    /** Enviar a cocina (backend: t.o | o.ch | k.v). */
    fun canApprove(permissions: List<String>?): Boolean =
        any(permissions, PERM_ADMIN, PERM_TABLES_OPEN, PERM_CHARGE, PERM_KITCHEN)

    /** Rechazar (backend: t.o | o.ch). Cocina (k.v) NO rechaza. */
    fun canReject(permissions: List<String>?): Boolean =
        any(permissions, PERM_ADMIN, PERM_TABLES_OPEN, PERM_CHARGE)

    /** ¿Debe ESTE evento hacer sonar el "nuevo pedido" en este equipo? */
    fun shouldPlaySound(eventType: String, permissions: List<String>?): Boolean =
        eventType == EVENT && canView(permissions)

    /** ¿Este evento obliga a volver a pedir la cola (conteo/badge)? */
    fun shouldRefreshOnEvent(eventType: String): Boolean = eventType == EVENT

    /** Texto del badge: vacio si no hay pendientes (el badge no se pinta). */
    fun badgeLabel(count: Int): String = when {
        count <= 0 -> ""
        count > 99 -> "99+"
        else -> count.toString()
    }

    /** Conteo que muestra la barra: 0 si el usuario no puede revisar pedidos (no ve badge ajeno). */
    fun visibleCount(permissions: List<String>?, count: Int): Int =
        if (canView(permissions)) count.coerceAtLeast(0) else 0

    fun badgeContentDescription(count: Int): String = when {
        count <= 0 -> PendingApprovalCopy.BELL_NONE
        count == 1 -> "1 pedido del cliente por revisar"
        else -> "$count pedidos del cliente por revisar"
    }

    fun arrivedMessage(tableName: String?): String {
        val t = tableName?.trim().orEmpty()
        return if (t.isNotEmpty()) "Pedido del cliente por revisar · Mesa $t" else "Pedido del cliente por revisar"
    }

    fun headerTitle(count: Int): String =
        if (count == 1) "1 pedido del cliente por revisar" else "$count pedidos del cliente por revisar"

    fun orderTitle(tableName: String?, orderNumber: Int): String {
        val t = tableName?.trim().orEmpty()
        return if (t.isNotEmpty()) "Mesa $t · Comanda #$orderNumber" else "Pedido sin mesa · Comanda #$orderNumber"
    }

    /** Tipo de pedido legible; null si es mesa (ya sale en el titulo). */
    fun orderTypeLabel(orderType: String): String? = when (orderType.trim().lowercase()) {
        "takeaway", "para_llevar", "to_go" -> "Para llevar"
        "delivery" -> "Delivery"
        else -> null
    }

    /** Minutos que lleva esperando (>= 0), o null si la hora no se pudo leer. */
    fun waitingMinutes(createdAtEpochMs: Long?, nowEpochMs: Long): Int? {
        if (createdAtEpochMs == null) return null
        return ((nowEpochMs - createdAtEpochMs) / 60_000L).toInt().coerceAtLeast(0)
    }

    fun waitingLabel(minutes: Int?): String? = when {
        minutes == null -> null
        minutes < 1 -> "Recién llegó"
        minutes == 1 -> "Hace 1 min"
        else -> "Hace $minutes min"
    }
}

/** Textos de la cola de pedidos por revisar. Mismos textos que Tauri (`PendingApprovalPanel.tsx`). */
object PendingApprovalCopy {
    const val BELL_NONE = "Pedidos del cliente"
    const val SHEET_TITLE = "Pedidos del cliente por revisar"
    const val APPROVE = "Enviar a cocina"
    const val REJECT = "Rechazar"
    const val APPROVED_OK = "Pedido enviado a cocina"
    const val REJECTED_OK = "Pedido rechazado"
    const val REJECT_TITLE = "Rechazar pedido"
    const val REJECT_PLACEHOLDER = "¿Por qué se rechaza este pedido?"
    const val REJECT_REASON_REQUIRED = "Indica el motivo del rechazo"
    const val CANCEL = "Cancelar"
    const val RETRY = "Reintentar"
    const val STALE_ERROR = "No pudimos actualizar la lista. Puede haber pedidos nuevos."
    const val LOAD_ERROR = "No pudimos revisar los pedidos del QR. Puede haber pedidos esperando."
    const val EMPTY_TITLE = "Sin pedidos por revisar"
    const val EMPTY_DESCRIPTION =
        "Cuando un cliente pida desde la carta del QR, su pedido aparecerá aquí para que lo envíes a cocina."
    const val NOTE_PREFIX = "Nota"
    const val LOADING = "Buscando pedidos del cliente…"
    const val NO_PERMISSION_REJECT = "Tu usuario puede enviar a cocina, pero no rechazar. Pídelo a caja o al administrador."
}
