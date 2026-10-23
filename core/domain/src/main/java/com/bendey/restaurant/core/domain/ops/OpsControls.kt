package com.bendey.restaurant.core.domain.ops

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

/**
 * Control de descuentos y precios. [discountControl] es del restaurante (apagado por defecto);
 * [canDiscount] es de quien llama (administrador, supervisor, rol Administrador, `s.m` o el interruptor de su ficha).
 */
data class OpsControls(
    val discountControl: Boolean = false,
    val canDiscount: Boolean = true,
    /** «Mostrar stock en el POS» (del restaurante, apagado por defecto): ícono de stock en la tarjeta del producto. */
    val showPosStock: Boolean = false,
)

object OpsControlsCopy {
    const val SWITCH_LABEL = "Exigir permiso para dar descuentos y cambiar precios"
    const val SWITCH_HELP =
        "Encendido, solo administradores, supervisores y quienes tengan \"Puede dar descuentos\" pueden dar descuentos o cambiar precios al tomar el pedido."
    const val SECTION_TITLE = "Control de descuentos"
    const val STAFF_SWITCH_LABEL = "Puede dar descuentos y cambiar precios"

    const val POS_STOCK_TITLE = "Stock en el POS"
    const val POS_STOCK_LABEL = "Mostrar stock en el POS"
    const val POS_STOCK_HELP =
        "Encendido, las tarjetas de los productos que controlan stock muestran un ícono para consultar cuánto queda, en el POS y al tomar el pedido de una mesa. Lo ve todo el personal."

    /** Motivo que se muestra donde el descuento o el cambio de precio queda bloqueado. */
    const val LOCKED_HINT = "Pídele a un administrador"
}

object OpsControlsRules {
    /** Solo `s.m` cambia el interruptor del restaurante. */
    fun canEdit(permissions: List<String>?): Boolean =
        RestaurantPermissions.canManageRestaurantSettings(permissions.orEmpty())

    /**
     * ¿Se bloquea el descuento / el precio libre? Solo con el control encendido Y sin permiso personal.
     * Sin dato (aún no cargó o falló la consulta) NO bloquea: el servidor decide y responde DISCOUNT_FORBIDDEN.
     */
    fun discountsLocked(controls: OpsControls?): Boolean =
        controls != null && controls.discountControl && !controls.canDiscount
}

interface OpsRepository {
    /** Último valor conocido sin red, o null. */
    fun peekOpsControls(): OpsControls?

    /** `GET /api/restaurant/ops-controls`; deja el valor en la caché compartida. */
    suspend fun getOpsControls(forceRefresh: Boolean = false): AppResult<OpsControls>

    /** `PUT` (s.m) del interruptor del restaurante. */
    suspend fun setDiscountControl(enabled: Boolean): AppResult<OpsControls>

    /** `PUT` (s.m) de «Mostrar stock en el POS». */
    suspend fun setShowPosStock(enabled: Boolean): AppResult<OpsControls>

    suspend fun getBusinessHours(): AppResult<BusinessHours>

    /** `PUT` que REEMPLAZA todo el horario (lista vacía = siempre abierto). */
    suspend fun saveBusinessHours(intervals: List<BusinessInterval>): AppResult<BusinessHours>

    /** `POST /api/restaurant/sales/:id/reopen-session` (o.ch + PIN de operaciones). */
    suspend fun reopenSession(saleId: Int, pin: String, reason: String): AppResult<ReopenResult>
}

data class ReopenResult(val sessionId: Int, val releasedComandas: Int, val reopened: Boolean)

/** Anular un pedido con platos ya servidos: el servidor pide confirmar (409) y se reenvía con `confirm_served`. */
object ServedItemsRules {
    const val CODE = "SESSION_HAS_SERVED_ITEMS"
    const val CONFIRM_LABEL = "Anular igual"
    const val CANCEL_LABEL = "Cancelar"
    const val TITLE = "Platos ya servidos"

    /** ¿Este fallo de la anulación se resuelve con el diálogo «Anular igual»? */
    fun needsConfirmation(code: String?): Boolean = code == CODE
}

/** Reglas de «Reabrir para cobrar de nuevo». */
object ReopenRules {
    const val MIN_REASON = 3
    const val ACTION_LABEL = "Reabrir para cobrar de nuevo"
    const val SUCCESS_MESSAGE = "Pedido reabierto"

    /** Solo ventas ya anuladas que vienen de un pedido del restaurante. */
    fun isOffered(cancelled: Boolean, restaurantSessionId: Int?, canCharge: Boolean): Boolean =
        canCharge && cancelled && restaurantSessionId != null && restaurantSessionId > 0

    /** Mensaje de validación local o null si se puede enviar. */
    fun validate(reason: String, pin: String): String? = when {
        reason.trim().length < MIN_REASON -> "Indica el motivo para reabrir el pedido (mínimo $MIN_REASON caracteres)."
        pin.isBlank() -> "Ingresa el PIN de autorización"
        else -> null
    }
}
