package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.kitchen.kdsElapsedLabel
import com.bendey.restaurant.core.domain.kitchen.kdsElapsedMinutes
import com.bendey.restaurant.core.domain.kitchen.kdsParseInstant
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import java.time.Instant

/**
 * Lógica PURA de la vista Delivery (D1): semáforos, umbrales, etiquetas, orden y atención. Sin red ni
 * Android. Gemelo de `src/utils/deliveryBoardLogic.ts` de Tauri: MISMOS umbrales y MISMOS textos. La tabla
 * clave -> texto/valor está en `docs/DELIVERY_BOARD_COPY.md` y `DeliveryBoardCopyDocTest` falla si este
 * archivo y el documento se desalinean.
 *
 * Contrato del tablero: `GET /api/restaurant/delivery/board` (D_COMMON.md §1).
 */

// ---------------------------------------------------------------------------------------------
// Contrato (dominio, ya parseado y tolerante a campos ausentes)
// ---------------------------------------------------------------------------------------------

/** Orden de las columnas/pestañas, de izquierda a derecha. */
enum class DeliverySection(val key: String) {
    UNASSIGNED("unassigned"),
    ASSIGNED("assigned"),
    IN_TRANSIT("in_transit"),
    INCIDENTS("incidents"),
    DELIVERED_TODAY("delivered_today"),
}

data class DeliveryPerson(val id: Int, val name: String, val phone: String)

data class DeliveryIncident(
    /** failed | rejected */
    val kind: String,
    val reason: String,
    val at: String?,
)

data class DeliveryCard(
    val sessionId: Int,
    val assignmentId: Int?,
    /** staff | digital_menu | marketplace */
    val source: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val reference: String,
    val itemsCount: Int,
    val totalAmount: Double,
    val orderStatus: String,
    val createdAt: String?,
    val sentToKitchenAt: String? = null,
    val readyAt: String? = null,
    val estimatedMinutes: Int? = null,
    val driver: DeliveryPerson? = null,
    val assignmentStatus: String? = null,
    val assignedAt: String? = null,
    val acceptedAt: String? = null,
    val pickedUpAt: String? = null,
    val onTheWayAt: String? = null,
    val deliveredAt: String? = null,
    val incident: DeliveryIncident? = null,
    val paid: Boolean = false,
    /** Tarifa de delivery del pedido (D2.0); null si no tiene. `totalAmount` ya la incluye. */
    val deliveryFee: Double? = null,
    /** Pago registrado del pedido (D2b, efectivo contra entrega); null si la sesión no tiene pago. */
    val payment: SessionPayment? = null,
)

data class DeliveryBoardDriver(
    val id: Int,
    val name: String,
    val phone: String,
    val vehicleType: String,
    val isAvailable: Boolean,
    val activeCount: Int,
)

data class DeliveryBoardCounts(
    val unassigned: Int = 0,
    val assigned: Int = 0,
    val inTransit: Int = 0,
    val incidents: Int = 0,
    val deliveredToday: Int = 0,
) {
    operator fun get(section: DeliverySection): Int = when (section) {
        DeliverySection.UNASSIGNED -> unassigned
        DeliverySection.ASSIGNED -> assigned
        DeliverySection.IN_TRANSIT -> inTransit
        DeliverySection.INCIDENTS -> incidents
        DeliverySection.DELIVERED_TODAY -> deliveredToday
    }
}

data class DeliveryBoardData(
    val generatedAt: String = "",
    val counts: DeliveryBoardCounts = DeliveryBoardCounts(),
    val unassigned: List<DeliveryCard> = emptyList(),
    val assigned: List<DeliveryCard> = emptyList(),
    val inTransit: List<DeliveryCard> = emptyList(),
    val incidents: List<DeliveryCard> = emptyList(),
    val deliveredToday: List<DeliveryCard> = emptyList(),
    val drivers: List<DeliveryBoardDriver> = emptyList(),
) {
    fun cards(section: DeliverySection): List<DeliveryCard> = when (section) {
        DeliverySection.UNASSIGNED -> unassigned
        DeliverySection.ASSIGNED -> assigned
        DeliverySection.IN_TRANSIT -> inTransit
        DeliverySection.INCIDENTS -> incidents
        DeliverySection.DELIVERED_TODAY -> deliveredToday
    }
}

// ---------------------------------------------------------------------------------------------
// Umbrales
// ---------------------------------------------------------------------------------------------

/** Mismos valores que `DELIVERY_THRESHOLDS` de Tauri (nombres con la misma clave en el documento). */
object DeliveryThresholds {
    /** Con `estimated_minutes`: verde por debajo de esta fracción del estimado. */
    const val ESTIMATED_AMBER_FROM = 0.6

    /** Con `estimated_minutes`: rojo cuando se pasa de esta fracción (100 %). */
    const val ESTIMATED_RED_ABOVE = 1.0

    /** Sin estimado: ámbar desde estos minutos... */
    const val FALLBACK_AMBER_MIN = 20

    /** ...y rojo por encima de estos. */
    const val FALLBACK_RED_MIN = 35

    /** "Asignados": ámbar si el repartidor no acepta en estos minutos... */
    const val ACCEPT_AMBER_MIN = 5

    /** ...y rojo a estos. */
    const val ACCEPT_RED_MIN = 10

    /** Respaldo: refrescar cada 60 s SOLO si el tiempo real está caído. */
    const val BACKUP_POLL_MS = 60_000L

    /** Ventana de coalescencia de los eventos en tiempo real. */
    const val REFETCH_DEBOUNCE_MS = 400L

    /** Largo del motivo de cancelación / fallo. */
    const val REASON_MIN = 3
    const val REASON_MAX = 255
}

// ---------------------------------------------------------------------------------------------
// Textos (clave -> texto, idénticos a `DELIVERY_COPY` de Tauri)
// ---------------------------------------------------------------------------------------------

object DeliveryCopy {
    /** Todas las claves. `DeliveryBoardCopyDocTest` las compara con `docs/DELIVERY_BOARD_COPY.md`. */
    val entries: Map<String, String> = linkedMapOf(
        "title" to "Entregas",
        "subtitle" to "Pedidos para llevar a domicilio: quién los lleva y cómo van.",
        "section.unassigned" to "Por asignar",
        "section.assigned" to "Asignados",
        "section.in_transit" to "En camino",
        "section.incidents" to "Incidencias",
        "section.delivered_today" to "Entregados hoy",
        "empty.unassigned" to "No hay pedidos por asignar.",
        "empty.assigned" to "Ningún pedido espera respuesta de un repartidor.",
        "empty.in_transit" to "No hay pedidos en camino.",
        "empty.incidents" to "Sin incidencias. Todo en orden.",
        "empty.delivered_today" to "Todavía no se entregó ningún pedido hoy.",
        "source.staff" to "POS",
        "source.digital_menu" to "Menú digital",
        "source.marketplace" to "Marketplace",
        "kitchen.draft" to "Sin enviar",
        "kitchen.pending" to "Por confirmar",
        "kitchen.sent_to_kitchen" to "En cocina",
        "kitchen.preparing" to "Preparando",
        "kitchen.ready" to "Listo",
        "kitchen.on_the_way" to "En camino",
        "kitchen.delivered" to "Entregado",
        "kitchen.cancelled" to "Cancelado",
        "assignment.assigned" to "Esperando que acepte",
        "assignment.accepted" to "Aceptado",
        "assignment.picked_up" to "Recogido",
        "assignment.on_the_way" to "En camino",
        "assignment.delivered" to "Entregado",
        "assignment.failed" to "Incidencia",
        "assignment.rejected" to "Rechazado",
        "assignment.cancelled" to "Cancelado",
        "chip.paid" to "Pagado",
        "chip.no_phone" to "Sin teléfono",
        "incident.failed" to "No se pudo entregar",
        "incident.rejected" to "El repartidor lo rechazó",
        "alert.accept_amber" to "Sin aceptar",
        "alert.accept_red" to "Sin aceptar hace rato",
        "action.assign" to "Asignar",
        "action.reassign" to "Reasignar",
        "action.call" to "Llamar",
        "action.cancel" to "Cancelar pedido",
        "action.delivered" to "Marcar entregado",
        "action.failed" to "Marcar fallido",
        "action.refresh" to "Actualizar",
        "assign.title" to "Elegir repartidor",
        "assign.none" to "No hay repartidores registrados. Créalos en Mi negocio → Repartidores.",
        "driver.available" to "Disponible",
        "driver.unavailable" to "No disponible",
        "driver.unavailable_reason" to "Marcado como no disponible",
        "driver.active_zero" to "Sin pedidos activos",
        "driver.active_one" to "1 pedido activo",
        "driver.active_many" to "{n} pedidos activos",
        "cancel.title" to "Cancelar pedido",
        "cancel.reason_required" to "Escribe el motivo de la cancelación (mínimo 3 letras).",
        "cancel.reason_other" to "Otro",
        "cancel.reason_1" to "El cliente canceló",
        "cancel.reason_2" to "No hay stock",
        "cancel.reason_3" to "Dirección fuera de zona",
        "cancel.reason_4" to "No contesta",
        "failed.title" to "Marcar como fallido",
        "failed.reason_required" to "Escribe el motivo por el que no se pudo entregar (mínimo 3 letras).",
        "failed.reason_1" to "El cliente no contesta",
        "failed.reason_2" to "No encontramos la dirección",
        "failed.reason_3" to "El cliente rechazó el pedido",
        "delivered.confirm" to "Se marcará como entregado. El cliente ya recibió su pedido.",
        "ok.assigned" to "Repartidor asignado.",
        "ok.cancelled" to "Pedido cancelado.",
        "ok.delivered" to "Pedido marcado como entregado.",
        "ok.failed" to "Pedido marcado como fallido.",
        "drivers.title" to "Repartidores",
        "drivers.empty" to "No hay repartidores activos.",
        "readonly.hint" to "Solo puedes ver los pedidos. Para asignar o cambiar su estado pide permiso al administrador.",
        "load_error" to "No se pudo cargar Delivery. Revisa tu conexión e intenta de nuevo.",
        "badge.aria_one" to "1 pedido de delivery por asignar",
        "badge.aria_many" to "{n} pedidos de delivery por asignar",
        "toast.new_unassigned" to "Nuevo pedido de delivery por asignar",
        // D2b: efectivo contra entrega. Tauri alinea estas claves después (mismas claves y textos).
        "chip.cod" to "Contra entrega",
        "chip.collected" to "Cobrado",
        "payment.collect" to "Cobrar {amount}",
        "payment.tendered" to "Paga con {amount}",
        "payment.change" to "Vuelto {amount}",
        "payment.exact" to "Paga justo",
        "payment.insufficient" to "El total subió: revisa con cuánto paga",
        "action.collect" to "Marcar cobrado",
        "collect.title" to "Marcar cobrado",
        "collect.confirm" to "Confirma que ya recibiste el efectivo del cliente.",
        "ok.collected" to "Pedido marcado como cobrado.",
        "force.title" to "Aún no está cobrado",
        "force.hint" to "Este pedido es de pago contra entrega y todavía no se marcó como cobrado. Si igual vas a marcar la entrega, escribe el motivo.",
        "force.reason_label" to "Motivo",
        "force.reason_required" to "Escribe el motivo (mínimo 3 letras).",
        "force.confirm" to "Marcar entregado igual",
        "cod.title" to "Pago contra entrega",
        "cod.switch" to "Permitir pago en efectivo contra entrega",
        "cod.help" to "Tus clientes del marketplace podrán elegir pagar en efectivo al recibir. El repartidor cobra y tú lo ves en Entregas. El registro del efectivo en caja se hará en una próxima actualización; el comprobante lo sigues emitiendo tú.",
        "cod.confirm_title" to "¿Activar el pago contra entrega?",
        "cod.confirm_text" to "Desde ahora tus clientes del marketplace podrán elegir pagar en efectivo al recibir. El pedido entra directo a cocina y el repartidor cobra al entregar.",
        "cod.confirm_action" to "Activar",
        "cod.read_only" to "Solo el administrador puede cambiar el pago contra entrega.",
        "cod.saved_on" to "Pago contra entrega activado.",
        "cod.saved_off" to "Pago contra entrega desactivado.",
        "cod.on" to "Encendido",
        "cod.off" to "Apagado",
        "pos.payment_label" to "Pago",
        "pos.payment_none" to "Sin definir",
        "pos.payment_cod" to "Efectivo contra entrega",
        "pos.tendered_label" to "El cliente paga con (S/)",
        "pos.tendered_low" to "Debe cubrir el total {amount}",
        "pos.tendered_invalid" to "Revisa el monto: usa solo números, con hasta 2 decimales.",
        "pos.change_estimate" to "Vuelto estimado {amount}",
    )

    /** Valores numéricos (umbrales) en el mismo formato clave -> texto que usa el documento. */
    val thresholds: Map<String, String> = linkedMapOf(
        "estimatedAmberFrom" to numberText(DeliveryThresholds.ESTIMATED_AMBER_FROM),
        "estimatedRedAbove" to numberText(DeliveryThresholds.ESTIMATED_RED_ABOVE),
        "fallbackAmberMin" to DeliveryThresholds.FALLBACK_AMBER_MIN.toString(),
        "fallbackRedMin" to DeliveryThresholds.FALLBACK_RED_MIN.toString(),
        "acceptAmberMin" to DeliveryThresholds.ACCEPT_AMBER_MIN.toString(),
        "acceptRedMin" to DeliveryThresholds.ACCEPT_RED_MIN.toString(),
        "backupPollMs" to DeliveryThresholds.BACKUP_POLL_MS.toString(),
        "refetchDebounceMs" to DeliveryThresholds.REFETCH_DEBOUNCE_MS.toString(),
        "reasonMin" to DeliveryThresholds.REASON_MIN.toString(),
        "reasonMax" to DeliveryThresholds.REASON_MAX.toString(),
    )

    /** Número como lo imprime Tauri (`1`, no `1.0`; `0.6`). */
    private fun numberText(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()

    fun text(key: String, n: Int? = null): String {
        val base = entries[key] ?: return ""
        return if (n != null) base.replace("{n}", n.toString()) else base
    }

    val cancelQuickReasons: List<String> = listOf(
        text("cancel.reason_1"), text("cancel.reason_2"), text("cancel.reason_3"), text("cancel.reason_4"),
    )
    val failedQuickReasons: List<String> = listOf(
        text("failed.reason_1"), text("failed.reason_2"), text("failed.reason_3"),
    )
}

fun deliverySectionLabel(section: DeliverySection): String = DeliveryCopy.text("section.${section.key}")

fun deliverySectionEmpty(section: DeliverySection): String = DeliveryCopy.text("empty.${section.key}")

fun deliverySourceLabel(source: String?): String =
    DeliveryCopy.entries["source.${source.orEmpty()}"] ?: DeliveryCopy.text("source.staff")

fun deliveryKitchenLabel(orderStatus: String?): String =
    DeliveryCopy.entries["kitchen.${orderStatus.orEmpty()}"] ?: DeliveryCopy.text("kitchen.sent_to_kitchen")

fun deliveryAssignmentLabel(status: String?): String =
    DeliveryCopy.entries["assignment.${status.orEmpty()}"].orEmpty()

/** Color del chip de estado de cocina (siempre con texto, nunca solo color). */
enum class DeliveryChipTone { NEUTRAL, INFO, WARNING, SUCCESS, DANGER, BRAND }

fun deliveryKitchenTone(orderStatus: String?): DeliveryChipTone = when (orderStatus) {
    "ready", "delivered" -> DeliveryChipTone.SUCCESS
    "preparing" -> DeliveryChipTone.WARNING
    "sent_to_kitchen" -> DeliveryChipTone.INFO
    "on_the_way" -> DeliveryChipTone.BRAND
    "cancelled" -> DeliveryChipTone.DANGER
    else -> DeliveryChipTone.NEUTRAL
}

// ---------------------------------------------------------------------------------------------
// Tiempo y semáforos
// ---------------------------------------------------------------------------------------------

enum class DeliveryTone { NONE, OK, WARN, DANGER }

fun worstTone(a: DeliveryTone, b: DeliveryTone): DeliveryTone = if (a.ordinal >= b.ordinal) a else b

/** Minutos desde `created_at` (null si la fecha no es válida). */
fun deliveryElapsedMinutes(card: DeliveryCard, now: Instant): Int? =
    kdsElapsedMinutes(kdsParseInstant(card.createdAt), now)

/**
 * Semáforo del reloj del pedido. Con `estimated_minutes`: verde < 60 %, ámbar 60-100 %, rojo > 100 %.
 * Sin estimado: verde < 20 min, ámbar 20-35, rojo > 35.
 */
fun deliveryElapsedTone(elapsedMin: Int?, estimatedMin: Int?): DeliveryTone {
    elapsedMin ?: return DeliveryTone.NONE
    if (estimatedMin != null && estimatedMin > 0) {
        val ratio = elapsedMin.toDouble() / estimatedMin
        return when {
            ratio > DeliveryThresholds.ESTIMATED_RED_ABOVE -> DeliveryTone.DANGER
            ratio >= DeliveryThresholds.ESTIMATED_AMBER_FROM -> DeliveryTone.WARN
            else -> DeliveryTone.OK
        }
    }
    return when {
        elapsedMin > DeliveryThresholds.FALLBACK_RED_MIN -> DeliveryTone.DANGER
        elapsedMin >= DeliveryThresholds.FALLBACK_AMBER_MIN -> DeliveryTone.WARN
        else -> DeliveryTone.OK
    }
}

/** Espera sin que el repartidor acepte (solo para asignaciones en estado `assigned`). */
fun deliveryAcceptWaitMinutes(card: DeliveryCard, now: Instant): Int? {
    if (card.assignmentStatus != "assigned") return null
    return kdsElapsedMinutes(kdsParseInstant(card.assignedAt), now)
}

/** Alerta de "Asignados": ámbar a los 5 min sin aceptar, roja a los 10. */
fun deliveryAcceptAlert(waitMin: Int?): DeliveryTone {
    waitMin ?: return DeliveryTone.NONE
    return when {
        waitMin >= DeliveryThresholds.ACCEPT_RED_MIN -> DeliveryTone.DANGER
        waitMin >= DeliveryThresholds.ACCEPT_AMBER_MIN -> DeliveryTone.WARN
        else -> DeliveryTone.NONE
    }
}

data class DeliveryCardClock(
    val elapsedMin: Int?,
    val label: String,
    val tone: DeliveryTone,
    /** Alerta de espera de aceptación (solo en Asignados); NONE si no aplica. */
    val accept: DeliveryTone,
    /** Peor de los dos: el color del borde de la tarjeta. */
    val overall: DeliveryTone,
)

/** Todo lo que la tarjeta necesita saber del tiempo. En "Entregados hoy" no hay semáforo. */
fun deliveryCardClock(card: DeliveryCard, section: DeliverySection, now: Instant): DeliveryCardClock {
    val elapsed = deliveryElapsedMinutes(card, now)
    val label = kdsElapsedLabel(elapsed)
    if (section == DeliverySection.DELIVERED_TODAY) {
        return DeliveryCardClock(elapsed, label, DeliveryTone.NONE, DeliveryTone.NONE, DeliveryTone.NONE)
    }
    val tone = deliveryElapsedTone(elapsed, card.estimatedMinutes)
    val accept = if (section == DeliverySection.ASSIGNED) {
        deliveryAcceptAlert(deliveryAcceptWaitMinutes(card, now))
    } else {
        DeliveryTone.NONE
    }
    return DeliveryCardClock(elapsed, label, tone, accept, worstTone(tone, accept))
}

/** Texto de la alerta de aceptación (vacío si no hay). */
fun deliveryAcceptAlertLabel(accept: DeliveryTone): String = when (accept) {
    DeliveryTone.DANGER -> DeliveryCopy.text("alert.accept_red")
    DeliveryTone.WARN -> DeliveryCopy.text("alert.accept_amber")
    else -> ""
}

// ---------------------------------------------------------------------------------------------
// Orden y atención
// ---------------------------------------------------------------------------------------------

private fun ts(iso: String?): Long = kdsParseInstant(iso)?.toEpochMilli() ?: 0L

/** Más antiguo primero; "Entregados hoy": el más reciente primero. Estable. No muta. */
fun deliverySortSection(section: DeliverySection, cards: List<DeliveryCard>): List<DeliveryCard> =
    if (section == DeliverySection.DELIVERED_TODAY) {
        cards.sortedByDescending { ts(it.deliveredAt ?: it.createdAt) }
    } else {
        cards.sortedBy { ts(it.createdAt) }
    }

fun deliverySectionCards(board: DeliveryBoardData, section: DeliverySection): List<DeliveryCard> =
    deliverySortSection(section, board.cards(section))

/**
 * ¿Pide atención humana ahora? Por asignar e Incidencias siempre; Asignados si pasó el umbral de aceptación
 * o se puso roja; En camino si se puso roja. Entregados nunca.
 */
fun deliveryNeedsAttention(card: DeliveryCard, section: DeliverySection, now: Instant): Boolean {
    if (section == DeliverySection.UNASSIGNED || section == DeliverySection.INCIDENTS) return true
    if (section == DeliverySection.DELIVERED_TODAY) return false
    val c = deliveryCardClock(card, section, now)
    return if (section == DeliverySection.ASSIGNED) {
        c.accept != DeliveryTone.NONE || c.tone == DeliveryTone.DANGER
    } else {
        c.tone == DeliveryTone.DANGER
    }
}

/** Conteos: usa los del servidor y cae a las longitudes de las listas si faltan (`counts` ausente). */
fun deliveryBoardCounts(board: DeliveryBoardData?, serverCounts: DeliveryBoardCounts? = board?.counts): DeliveryBoardCounts {
    board ?: return DeliveryBoardCounts()
    val c = serverCounts ?: DeliveryBoardCounts()
    fun pick(section: DeliverySection): Int = c[section].takeIf { it > 0 } ?: board.cards(section).size
    return DeliveryBoardCounts(
        unassigned = pick(DeliverySection.UNASSIGNED),
        assigned = pick(DeliverySection.ASSIGNED),
        inTransit = pick(DeliverySection.IN_TRANSIT),
        incidents = pick(DeliverySection.INCIDENTS),
        deliveredToday = pick(DeliverySection.DELIVERED_TODAY),
    )
}

/** Texto del badge de la barra (vacío si no hay nada por asignar). */
fun deliveryBadgeLabel(count: Int): String = when {
    count <= 0 -> ""
    count > 99 -> "99+"
    else -> count.toString()
}

fun deliveryBadgeDescription(count: Int): String = when {
    count <= 0 -> DeliveryCopy.text("title")
    count == 1 -> DeliveryCopy.text("badge.aria_one")
    else -> DeliveryCopy.text("badge.aria_many", count)
}

/**
 * Sesiones de "Por asignar" que NO estaban en el conjunto conocido. [known] == null es la PRIMERA carga (o la
 * de después de cambiar de sucursal): nada se considera nuevo, así no suena por pedidos que ya estaban.
 */
fun deliveryNewUnassignedIds(known: Set<Int>?, board: DeliveryBoardData): List<Int> {
    known ?: return emptyList()
    return board.unassigned.map { it.sessionId }.filter { it !in known }
}

// ---------------------------------------------------------------------------------------------
// Permisos
// ---------------------------------------------------------------------------------------------

/** Ver Delivery (barra, vista, badge, sonido). */
fun canViewDelivery(permissions: List<String>?): Boolean =
    permissions?.contains(RestaurantPermissions.PERM_REPARTIDORES) == true

/** Operar (asignar, cancelar, cambiar estado). Sin esto la vista es de solo lectura. */
const val PERM_DELIVERY_UPDATE = "d.u"

fun canAssignDelivery(permissions: List<String>?): Boolean =
    permissions?.contains(PERM_DELIVERY_UPDATE) == true

/** El repartidor (employee_type=driver) tiene `d.u` para sus propias entregas en Bendey Delivery, pero aquí solo mira. */
fun isDeliveryDriver(employeeType: String?): Boolean = employeeType.equals("driver", ignoreCase = true)

/** Operar el tablero como personal del local: `d.u` y no ser repartidor. */
fun canOperateDeliveryBoard(permissions: List<String>?, employeeType: String?): Boolean =
    canAssignDelivery(permissions) && !isDeliveryDriver(employeeType)

// ---------------------------------------------------------------------------------------------
// Acciones por tarjeta
// ---------------------------------------------------------------------------------------------

enum class DeliveryAction(val key: String) {
    ASSIGN("assign"),
    REASSIGN("reassign"),
    CALL("call"),
    CANCEL("cancel"),
    DELIVERED("delivered"),
    FAILED("failed"),

    /** D2b: "Marcar cobrado" (efectivo contra entrega). */
    COLLECT("collect"),
}

fun deliveryActionLabel(action: DeliveryAction): String = DeliveryCopy.text("action.${action.key}")

/** Qué botones lleva una tarjeta. `canAssign=false` => solo "Llamar" (lectura). */
fun deliveryCardActions(card: DeliveryCard, section: DeliverySection, canAssign: Boolean): List<DeliveryAction> {
    val out = mutableListOf<DeliveryAction>()
    if (section == DeliverySection.DELIVERED_TODAY) {
        return if (card.customerPhone.isNotBlank()) listOf(DeliveryAction.CALL) else emptyList()
    }
    if (card.customerPhone.isNotBlank()) out += DeliveryAction.CALL
    if (!canAssign) return out
    when (section) {
        DeliverySection.UNASSIGNED -> { out += DeliveryAction.ASSIGN; out += DeliveryAction.CANCEL }
        DeliverySection.ASSIGNED -> { out += DeliveryAction.REASSIGN; out += DeliveryAction.CANCEL }
        DeliverySection.IN_TRANSIT -> {
            // D2b: con efectivo contra entrega pendiente, "Marcar cobrado" va antes que "Marcar entregado".
            if (canCollectDelivery(card.payment, card.assignmentId, card.assignmentStatus)) out += DeliveryAction.COLLECT
            if (card.assignmentId != null) { out += DeliveryAction.DELIVERED; out += DeliveryAction.FAILED }
            out += DeliveryAction.REASSIGN
            out += DeliveryAction.CANCEL
        }
        DeliverySection.INCIDENTS -> { out += DeliveryAction.ASSIGN; out += DeliveryAction.CANCEL }
        DeliverySection.DELIVERED_TODAY -> Unit
    }
    return out
}

/** URI `tel:` (solo dígitos y +). Vacía si no hay teléfono utilizable (menos de 5 dígitos). */
fun deliveryTelUri(phone: String?): String {
    val digits = phone.orEmpty().filter { it.isDigit() || it == '+' }
    return if (digits.count { it.isDigit() } >= 5) "tel:$digits" else ""
}

// ---------------------------------------------------------------------------------------------
// Repartidores
// ---------------------------------------------------------------------------------------------

fun deliveryActiveCountLabel(n: Int): String = when {
    n <= 0 -> DeliveryCopy.text("driver.active_zero")
    n == 1 -> DeliveryCopy.text("driver.active_one")
    else -> DeliveryCopy.text("driver.active_many", n)
}

data class DeliveryDriverChoice(val disabled: Boolean, val reason: String, val activeLabel: String)

/** Cómo se muestra un repartidor en el diálogo de asignación. */
fun deliveryDriverChoice(d: DeliveryBoardDriver): DeliveryDriverChoice = DeliveryDriverChoice(
    disabled = !d.isAvailable,
    reason = if (d.isAvailable) DeliveryCopy.text("driver.available") else DeliveryCopy.text("driver.unavailable_reason"),
    activeLabel = deliveryActiveCountLabel(d.activeCount),
)

/** Disponibles primero, luego menos pedidos activos, luego por nombre. No muta. */
fun deliverySortDrivers(drivers: List<DeliveryBoardDriver>): List<DeliveryBoardDriver> =
    drivers.sortedWith(
        compareBy<DeliveryBoardDriver> { !it.isAvailable }
            .thenBy { it.activeCount }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
    )

// ---------------------------------------------------------------------------------------------
// Motivos (cancelar / fallido)
// ---------------------------------------------------------------------------------------------

/** Valor especial del selector para escribir el motivo a mano. */
const val DELIVERY_REASON_OTHER = "__other__"

/** Motivo final: el rápido elegido, o el texto escrito si eligió "Otro". Recortado. */
fun deliveryResolveReason(selected: String?, custom: String): String {
    selected ?: return ""
    return (if (selected == DELIVERY_REASON_OTHER) custom else selected).trim()
}

enum class DeliveryReasonKind { CANCEL, FAILED }

/** null si es válido; si no, el texto de error. Largo 3-255 (igual que el backend). */
fun deliveryReasonError(reason: String, kind: DeliveryReasonKind): String? {
    val len = reason.trim().length
    if (len in DeliveryThresholds.REASON_MIN..DeliveryThresholds.REASON_MAX) return null
    return DeliveryCopy.text(if (kind == DeliveryReasonKind.CANCEL) "cancel.reason_required" else "failed.reason_required")
}
