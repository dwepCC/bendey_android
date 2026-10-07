package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import kotlin.math.round

/**
 * Tarifa de delivery (D2.0). Gemelo de `deliveryFee.ts` de Tauri: mismos textos y mismas reglas.
 * Contrato: `GET/PUT /api/restaurant/delivery/settings` (D2_COMMON §1-2).
 *
 * Lógica PURA (sin red ni Android): la usan el ViewModel de Ajustes, la hoja de asignar y el POS.
 */

/** Ajustes de delivery del restaurante. Los campos de contra entrega / pago manual son de solo lectura en D2.0. */
data class DeliverySettings(
    val feeEnabled: Boolean = false,
    val deliveryFee: Double = 0.0,
    /** `10` gravado con IGV (por defecto), `20` exonerado, `30` inafecto. */
    val feeIgvAffectation: String = DeliveryFeeRules.AFFECTATION_TAXED,
    val codEnabled: Boolean = false,
    val manualPaymentEnabled: Boolean = false,
    val paymentReviewMinutes: Int = 15,
)

/** Cambio parcial de los ajustes: lo que sea null no se envía. */
data class DeliverySettingsUpdate(
    val feeEnabled: Boolean? = null,
    val deliveryFee: Double? = null,
    val feeIgvAffectation: String? = null,
)

object DeliveryFeeRules {
    const val MIN = 0.0
    const val MAX = 999.99
    const val AFFECTATION_TAXED = "10"
    const val AFFECTATION_EXEMPT = "20"
    const val AFFECTATION_UNAFFECTED = "30"

    /** Código de la línea "Servicio de delivery" que crea el backend (D2_COMMON: código fijo "DELIVERY"). */
    const val LINE_CODE = "DELIVERY"
    const val LINE_NAME = "Servicio de delivery"

    val affectations: List<Pair<String, String>> = listOf(
        AFFECTATION_TAXED to "Gravado con IGV",
        AFFECTATION_EXEMPT to "Exonerado",
        AFFECTATION_UNAFFECTED to "Inafecto",
    )

    fun affectationLabel(code: String?): String =
        affectations.firstOrNull { it.first == code }?.second ?: affectations.first().second
}

/** Textos (idénticos a `DELIVERY_FEE_COPY` de Tauri). */
object DeliveryFeeCopy {
    const val SECTION_TITLE = "Tarifa de delivery"
    const val SWITCH_LABEL = "Cobrar tarifa de delivery"
    const val AMOUNT_LABEL = "Tarifa de delivery (S/)"
    const val FISCAL_TITLE = "Opciones fiscales"
    const val AFFECTATION_LABEL = "Afectación al IGV"
    const val FISCAL_HINT = "Consulta con tu contador antes de activar la tarifa: se emite como una línea del comprobante."
    const val NEW_ORDERS_NOTICE = "La tarifa nueva se aplica a los pedidos nuevos; los pedidos ya creados mantienen la suya."
    const val READ_ONLY_HINT = "Solo el administrador puede cambiar la tarifa de delivery."
    const val ASSIGN_LOCKED = "Solo caja o administración puede cambiar la tarifa"
    const val CARD_INCLUDES = "Incluye envío"
    const val CHECKOUT_LINE = "Servicio de delivery"
    const val SAVED = "Tarifa de delivery guardada."
    const val OFF = "Apagada"
    const val AMOUNT_REQUIRED_TO_ENABLE = "Escribe una tarifa mayor que 0 para encender la opción."
    const val INVALID_AMOUNT = "Revisa la tarifa: usa un monto entre S/ 0 y S/ 999.99."
}

fun deliveryMoney(value: Double): String = String.format(java.util.Locale.US, "S/ %.2f", value)

/**
 * Convierte lo que el usuario escribió en un monto válido (0 a 999.99, máximo 2 decimales) o null.
 * Acepta coma como separador decimal. Vacío = null (el llamador decide si es "sin tarifa").
 */
fun parseDeliveryFeeInput(text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return null
    if (!Regex("""^\d{0,3}(\.\d{0,2})?$""").matches(t) || t == ".") return null
    val v = t.toDoubleOrNull() ?: return null
    return if (v in DeliveryFeeRules.MIN..DeliveryFeeRules.MAX) round(v * 100.0) / 100.0 else null
}

/** Cómo se muestra un monto en el campo de texto: sin ceros de más ("5" -> "5.00" se deja a quien lo pinta). */
fun deliveryFeeFieldText(value: Double?): String =
    if (value == null || value <= 0.0) "" else String.format(java.util.Locale.US, "%.2f", value)

/** Resultado de validar el formulario de la tarifa en Ajustes. */
sealed interface DeliveryFeeFormResult {
    data class Ok(val update: DeliverySettingsUpdate) : DeliveryFeeFormResult
    data class Invalid(val message: String) : DeliveryFeeFormResult
}

/**
 * Valida el formulario de Ajustes > Operación y arma el cuerpo parcial del PUT.
 * Reglas: monto 0 a 999.99 con 2 decimales; no se enciende con 0; afectación 10/20/30.
 */
fun validateDeliveryFeeForm(enabled: Boolean, amountText: String, affectation: String): DeliveryFeeFormResult {
    if (affectation !in DeliveryFeeRules.affectations.map { it.first }) {
        return DeliveryFeeFormResult.Invalid(DeliveryFeeCopy.INVALID_AMOUNT)
    }
    val amount: Double = if (amountText.isBlank()) {
        0.0
    } else {
        parseDeliveryFeeInput(amountText) ?: return DeliveryFeeFormResult.Invalid(DeliveryFeeCopy.INVALID_AMOUNT)
    }
    if (enabled && amount <= 0.0) return DeliveryFeeFormResult.Invalid(DeliveryFeeCopy.AMOUNT_REQUIRED_TO_ENABLE)
    return DeliveryFeeFormResult.Ok(
        DeliverySettingsUpdate(feeEnabled = enabled, deliveryFee = amount, feeIgvAffectation = affectation),
    )
}

/** Cambiar la tarifa de un pedido: caja (`o.ch`) o administración (`s.m`). El repartidor y el mozo no. */
fun canEditDeliveryFee(permissions: List<String>?): Boolean {
    val p = permissions.orEmpty()
    return RestaurantPermissions.hasPermission(p, RestaurantPermissions.PERM_ORDERS_CHARGE) ||
        RestaurantPermissions.hasPermission(p, RestaurantPermissions.PERM_ADMIN)
}

/** Cambiar los ajustes del restaurante (interruptor, monto, afectación): solo `s.m`. */
fun canManageDeliverySettings(permissions: List<String>?): Boolean =
    RestaurantPermissions.isRestaurantAdmin(permissions.orEmpty())

/** ¿La hoja de asignar muestra el campo de tarifa? Si la tarifa está encendida o el pedido ya tiene una. */
fun showsAssignFeeField(settings: DeliverySettings?, cardFee: Double?): Boolean =
    settings?.feeEnabled == true || (cardFee != null && cardFee > 0.0)

/** Valor inicial del campo en la hoja de asignar: la tarifa del pedido o, si es null, la de ajustes. */
fun assignFeeInitialText(cardFee: Double?, settings: DeliverySettings?): String =
    deliveryFeeFieldText(cardFee ?: settings?.deliveryFee)

/**
 * Lo que se envía con el asignar: `null` si el campo quedó igual que el valor inicial (o es ilegible),
 * el monto si cambió. Así un cajero que no tocó nada no pisa la tarifa del pedido.
 */
fun assignFeeToSend(initialText: String, currentText: String): Double? {
    val current = if (currentText.isBlank()) 0.0 else parseDeliveryFeeInput(currentText) ?: return null
    val initial = if (initialText.isBlank()) 0.0 else parseDeliveryFeeInput(initialText) ?: 0.0
    return if (current == initial) null else current
}

/** ¿El texto del campo es un monto válido (vacío cuenta como 0, que quita la tarifa)? */
fun isValidAssignFeeText(text: String): Boolean = text.isBlank() || parseDeliveryFeeInput(text) != null

/**
 * Detecta la línea "Servicio de delivery" que el backend agrega a la sesión (comanda sin producto con código
 * fijo `DELIVERY`). Se acepta también el nombre por si el código no llegara.
 */
fun isDeliveryFeeLine(productCode: String?, productName: String?): Boolean =
    productCode.equals(DeliveryFeeRules.LINE_CODE, ignoreCase = true) ||
        (productCode.isNullOrBlank() && productName?.trim().equals(DeliveryFeeRules.LINE_NAME, ignoreCase = true))
