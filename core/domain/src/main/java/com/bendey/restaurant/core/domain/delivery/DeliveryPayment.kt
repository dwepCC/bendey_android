package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import kotlin.math.round

/**
 * Efectivo contra entrega (D2b). Gemelo de la lógica de pago de Tauri: mismas reglas y mismos textos (los textos
 * viven en [DeliveryCopy], claves `chip.cod`, `payment.*`, `collect.*`, `force.*`, `cod.*` y `pos.*`).
 * Contrato: D2B_COMMON §1-§5. D2b solo REGISTRA el cobro: no toca caja, ledger ni facturación.
 *
 * Lógica PURA (sin red ni Android): la usan el tablero, el POS y Ajustes.
 */

/** Modos de pago que entiende el backend en D2b. */
object SessionPaymentMode {
    const val CASH_ON_DELIVERY = "cash_on_delivery"

    /** Solo se ENVÍA (quita el pago); el servidor devuelve `payment: null`. */
    const val NONE = "none"
}

/** Estados del pago en D2b (los demás del plan no se usan todavía). */
object SessionPaymentStatus {
    const val PENDING_COLLECTION = "pending_collection"
    const val COLLECTED = "collected"
    const val CANCELLED = "cancelled"
}

/** Quién marcó el cobro: `kind` = driver | staff. */
data class PaymentCollector(val id: Int, val name: String, val kind: String)

/**
 * Pago registrado de una sesión (mismo objeto en el tablero, el detalle de sesión y las respuestas de pago).
 * `null` en las pantallas = la sesión no tiene pago registrado.
 */
data class SessionPayment(
    val mode: String = "",
    val status: String = "",
    /** Total VIVO de la sesión mientras no esté cobrado; al cobrar se congela. */
    val expectedAmount: Double? = null,
    /** Con cuánto paga el cliente; null = paga justo. */
    val tenderedAmount: Double? = null,
    /** `max(tendered - expected, 0)`; null si paga justo o si [tenderedInsufficient]. */
    val changeAmount: Double? = null,
    val tenderedInsufficient: Boolean = false,
    val collectedAt: String? = null,
    val collectedBy: PaymentCollector? = null,
) {
    val isCashOnDelivery: Boolean get() = mode == SessionPaymentMode.CASH_ON_DELIVERY
    val isCollected: Boolean get() = isCashOnDelivery && status == SessionPaymentStatus.COLLECTED
    val isPendingCollection: Boolean get() = isCashOnDelivery && status == SessionPaymentStatus.PENDING_COLLECTION
}

fun SessionPayment?.isCodPending(): Boolean = this?.isPendingCollection == true

fun SessionPayment?.isCodCollected(): Boolean = this?.isCollected == true

/** Vuelto a mostrar: el que manda el servidor o, si no vino, el calculado con el total que se ve. */
fun paymentChange(payment: SessionPayment): Double? {
    if (payment.tenderedInsufficient) return null
    payment.changeAmount?.let { return it }
    val tendered = payment.tenderedAmount ?: return null
    val expected = payment.expectedAmount ?: return null
    return if (tendered >= expected) round((tendered - expected) * 100.0) / 100.0 else null
}

// ---------------------------------------------------------------------------------------------
// Permisos
// ---------------------------------------------------------------------------------------------

/** Forzar "Marcar entregado" sin cobro: caja (`o.ch`) o administración (`s.m`). Mozo y repartidor no. */
fun canForceDelivery(permissions: List<String>?): Boolean {
    val p = permissions.orEmpty()
    return RestaurantPermissions.hasPermission(p, RestaurantPermissions.PERM_ORDERS_CHARGE) ||
        RestaurantPermissions.hasPermission(p, RestaurantPermissions.PERM_ADMIN)
}

// ---------------------------------------------------------------------------------------------
// Tarjeta del tablero
// ---------------------------------------------------------------------------------------------

/** Rellena los marcadores `{amount}`, `{time}` y `{name}` de un texto de [DeliveryCopy]. */
fun deliveryCopyFill(key: String, vararg values: Pair<String, String>): String {
    var text = DeliveryCopy.text(key)
    for ((name, value) in values) text = text.replace("{$name}", value)
    return text
}

/**
 * Línea de cobro de la tarjeta (solo con pago contra entrega PENDIENTE):
 * `Cobrar S/ 83.00 · Paga con S/ 100.00 · Vuelto S/ 17.00` o `Cobrar S/ 83.00 · Paga justo`.
 * Con `tendered_insufficient` no hay vuelto: se avisa que el total subió. Vacía si no aplica.
 */
fun deliveryPaymentLine(payment: SessionPayment?): String {
    payment ?: return ""
    if (!payment.isPendingCollection) return ""
    val parts = mutableListOf<String>()
    payment.expectedAmount?.let { parts += deliveryCopyFill("payment.collect", "amount" to deliveryMoney(it)) }
    val tendered = payment.tenderedAmount
    if (tendered == null) {
        parts += DeliveryCopy.text("payment.exact")
    } else {
        parts += deliveryCopyFill("payment.tendered", "amount" to deliveryMoney(tendered))
        paymentChange(payment)?.let { parts += deliveryCopyFill("payment.change", "amount" to deliveryMoney(it)) }
    }
    return parts.joinToString(" · ")
}

/** Aviso bajo la línea de cobro cuando el total subió por encima de con cuánto paga el cliente. */
fun deliveryPaymentWarning(payment: SessionPayment?): String =
    if (payment?.isPendingCollection == true && payment.tenderedInsufficient) DeliveryCopy.text("payment.insufficient") else ""

/** Línea de "Cobrado" con la hora (ya formateada por quien pinta) y quién: `14:32 · Luis`. Vacía si no está cobrado. */
fun deliveryCollectedLine(payment: SessionPayment?, timeText: String?): String {
    payment ?: return ""
    if (!payment.isCollected) return ""
    return listOf(timeText.orEmpty(), payment.collectedBy?.name.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * ¿Se muestra "Marcar cobrado"? Con pago contra entrega pendiente y la asignación ya recogida o en camino
 * (el backend rechaza antes con `COLLECT_STATUS_INVALID`).
 */
fun canCollectDelivery(payment: SessionPayment?, assignmentId: Int?, assignmentStatus: String?): Boolean =
    payment.isCodPending() && assignmentId != null && assignmentStatus in COLLECTABLE_STATUSES

private val COLLECTABLE_STATUSES = setOf("picked_up", "on_the_way")

/** Motivo para forzar la entrega sin cobro: 3 a 255 letras (igual que el backend). */
fun forceReasonError(reason: String): String? {
    val len = reason.trim().length
    if (len in DeliveryThresholds.REASON_MIN..DeliveryThresholds.REASON_MAX) return null
    return DeliveryCopy.text("force.reason_required")
}

// ---------------------------------------------------------------------------------------------
// POS: formulario "Pago" del pedido de delivery
// ---------------------------------------------------------------------------------------------

/** Resultado de revisar "El cliente paga con (S/)" contra el total del pedido. */
sealed interface CashTenderedCheck {
    /** Campo vacío: paga justo. */
    data object Exact : CashTenderedCheck

    /** Alcanza para el total; [change] es el vuelto estimado (0 si paga justo ese monto). */
    data class Ok(val amount: Double, val change: Double) : CashTenderedCheck

    /** No alcanza: se muestra "Debe cubrir el total S/ X" y no se guarda. */
    data class TooLow(val amount: Double, val total: Double) : CashTenderedCheck

    /**
     * El monto es el que YA estaba guardado y el total subió después (se agregaron productos): no se bloquea,
     * se avisa "El total subió: revisa con cuánto paga". El repartidor cobra el total igual.
     */
    data class Stale(val amount: Double, val total: Double) : CashTenderedCheck

    /** Texto ilegible o con más de 2 decimales. */
    data object Invalid : CashTenderedCheck
}

/** Un monto ilegible o que no cubre el total impide guardar/enviar el pedido. */
val CashTenderedCheck.blocksSaving: Boolean
    get() = this is CashTenderedCheck.TooLow || this === CashTenderedCheck.Invalid

/** Convierte lo escrito (coma o punto, hasta 2 decimales) en un monto mayor que 0, o null si está vacío/ilegible. */
fun parseCashTendered(text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty() || !Regex("""^\d{1,7}(\.\d{0,2})?$""").matches(t)) return null
    val v = t.toDoubleOrNull() ?: return null
    return if (v > 0.0) round(v * 100.0) / 100.0 else null
}

fun checkCashTendered(text: String, total: Double): CashTenderedCheck {
    if (text.isBlank()) return CashTenderedCheck.Exact
    val amount = parseCashTendered(text) ?: return CashTenderedCheck.Invalid
    // Sin total conocido (carrito vacío) no hay contra qué comparar: el servidor revisa al crear el pedido.
    if (total > 0.0 && amount + 0.005 < total) return CashTenderedCheck.TooLow(amount, total)
    return CashTenderedCheck.Ok(amount, if (total > 0.0) round((amount - total).coerceAtLeast(0.0) * 100.0) / 100.0 else 0.0)
}

/**
 * Como [checkCashTendered], pero un monto igual al ya guardado en el servidor que dejó de cubrir el total
 * (porque el pedido creció) pasa a [CashTenderedCheck.Stale] en vez de bloquear.
 */
fun checkCashTenderedAgainstSaved(text: String, total: Double, saved: SessionPayment?): CashTenderedCheck {
    val check = checkCashTendered(text, total)
    if (check is CashTenderedCheck.TooLow && saved?.isCashOnDelivery == true && saved.tenderedAmount == check.amount) {
        return CashTenderedCheck.Stale(check.amount, check.total)
    }
    return check
}

/** Aviso del campo (vacío si está bien). */
fun cashTenderedMessage(check: CashTenderedCheck): String = when (check) {
    is CashTenderedCheck.Stale -> DeliveryCopy.text("payment.insufficient")
    is CashTenderedCheck.TooLow -> deliveryCopyFill("pos.tendered_low", "amount" to deliveryMoney(check.total))
    CashTenderedCheck.Invalid -> DeliveryCopy.text("pos.tendered_invalid")
    else -> ""
}

/** Vuelto estimado a mostrar bajo el campo (vacío si paga justo o no alcanza). */
fun cashChangeEstimate(check: CashTenderedCheck): String =
    if (check is CashTenderedCheck.Ok && check.change > 0.0) {
        deliveryCopyFill("pos.change_estimate", "amount" to deliveryMoney(check.change))
    } else {
        ""
    }

/** El formulario "Pago" solo existe en pedidos de delivery del POS y con `cod_enabled` (o si el pedido ya tiene pago contra entrega). */
fun showsPosPaymentForm(isDelivery: Boolean, settings: DeliverySettings?, payment: SessionPayment?): Boolean =
    isDelivery && (settings?.codEnabled == true || payment?.isCashOnDelivery == true)

/**
 * Qué hay que mandar al servidor tras crear/actualizar la sesión: `null` = nada (el servidor ya tiene lo mismo,
 * o el formulario no aplica). [codSelected] es la elección del cajero; [tendered] el monto ya validado (null = justo).
 */
sealed interface PaymentSync {
    data class SetCod(val tendered: Double?) : PaymentSync
    data object Clear : PaymentSync
}

fun paymentSyncNeeded(
    formShown: Boolean,
    codSelected: Boolean,
    tendered: Double?,
    current: SessionPayment?,
): PaymentSync? {
    if (!formShown) return null
    return if (codSelected) {
        val same = current?.isCashOnDelivery == true && current.tenderedAmount == tendered
        if (same) null else PaymentSync.SetCod(tendered)
    } else {
        // Sin pago elegido: solo se quita si el servidor tiene uno que aún no se cobró.
        if (current?.isPendingCollection == true) PaymentSync.Clear else null
    }
}
