package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.Money
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder

val BILLING_NOT_ENABLED_MESSAGE: String
    get() =
        "La facturación electrónica no está habilitada. Use ${LabelKey.DOC_SALES_NOTE.text.lowercase()} o active ${LabelKey.TAX_AUTHORITY.text} en configuración."

private val RESTAURANT_CHECKOUT_SUNAT = setOf("00", "01", "03")

fun effectiveSunatCode(series: DocumentSeries): String {
    val code = series.sunatCode?.trim().orEmpty()
    if (code.isNotEmpty()) return code
    val doc = series.docType.lowercase()
        .replace(Regex("\\p{M}+"), "")
        .replace("\\s+".toRegex(), "")
    return when {
        doc.contains("nota") && doc.contains("venta") && !doc.contains("credito") -> "00"
        doc.contains("factura") && !doc.contains("credito") -> "01"
        doc.contains("boleta") -> "03"
        else -> ""
    }
}

fun isNotaVentaSeries(series: DocumentSeries): Boolean = effectiveSunatCode(series) == "00"

fun isElectronicBillingSunatCode(code: String?): Boolean {
    val c = code?.trim().orEmpty()
    return c == "01" || c == "03"
}

fun filterRestaurantCheckoutSeries(
    list: List<DocumentSeries>,
    sunatEnabled: Boolean = true,
): List<DocumentSeries> = list.filter { series ->
    // El régimen del tenant manda: un NRUS no emite factura. El backend la rechaza al reservar el
    // correlativo, o sea recien despues de que el mozo eligio el tipo y cobro.
    if (!series.emisible) return@filter false
    val cat = series.category.lowercase()
    if (cat.isNotEmpty() && cat != "venta") return@filter false
    val code = series.sunatCode?.trim().orEmpty()
    if (!sunatEnabled && code.isNotEmpty() && code != "00") return@filter false
    if (code.isNotEmpty() && code !in RESTAURANT_CHECKOUT_SUNAT) return@filter false
    val doc = series.docType.lowercase()
    if (doc.contains("credito") || doc.contains("crédito") || doc.contains("debito") || doc.contains("débito")) {
        return@filter false
    }
    if (doc.contains("guia") || doc.contains("guía") || doc.contains("retencion") || doc.contains("percepcion")) {
        return@filter false
    }
    true
}

/**
 * Series bloqueadas por plan: boleta/factura visibles con SUNAT configurado, pero el plan
 * del tenant no incluye el módulo `billing`. Se muestran atenuadas con candado en vez de
 * ocultarse, y seleccionarlas navega a la pantalla de suscripción.
 */
fun lockedCheckoutSeries(
    allSeries: List<DocumentSeries>,
    billingModuleEnabled: Boolean,
): List<DocumentSeries> {
    if (billingModuleEnabled) return emptyList()
    return filterRestaurantCheckoutSeries(allSeries, sunatEnabled = true)
        .filter { effectiveSunatCode(it) != "00" }
}

fun isRucContact(contact: ContactBrief): Boolean {
    val dt = contact.docType.trim()
    return dt == "6" || dt.equals("ruc", ignoreCase = true)
}

fun isFacturaDocType(docType: String, sunatCode: String?): Boolean {
    if (sunatCode?.trim() == "01") return true
    val key = docType.lowercase()
        .replace(Regex("\\p{M}+"), "")
        .replace("\\s+".toRegex(), "")
    return key.contains("factura")
}

/**
 * La factura de este país exige un cliente con RUC (reglas SUNAT, solo Perú). En Ecuador toda venta es
 * factura y admite cédula, pasaporte o consumidor final (con tope): el filtro de RUC no aplica.
 */
fun invoiceRequiresTaxId(docType: String, sunatCode: String?): Boolean =
    TenantLocaleHolder.current.usesSunatBuyerRules && isFacturaDocType(docType, sunatCode)

fun checkoutContactIsValid(
    contact: ContactBrief?,
    docType: String,
    sunatCode: String?,
): Boolean {
    if (contact == null) return false
    if (invoiceRequiresTaxId(docType, sunatCode)) return isRucContact(contact)
    return true
}

/**
 * Ecuador: el cliente es consumidor final (tipo «other», EC 07) o el genérico «Clientes Varios» del POS,
 * que conserva el documento peruano «0» (el backend lo trata igual ante el SRI). Con Perú siempre false.
 */
fun isFinalConsumerContact(docType: String?): Boolean {
    val locale = TenantLocaleHolder.current
    if (locale.usesSunatBuyerRules) return false
    val d = docType?.trim().orEmpty()
    if (d == "0") return true
    val code = locale.finalConsumerIdentity?.code ?: return false
    return d == code
}

/**
 * Validación del comprador en un país sin reglas SUNAT (Ecuador): solo el consumidor final tiene tope
 * (`final_consumer_max`, 50 USD). Devuelve el mensaje de error o null si pasa. Con Perú SIEMPRE null:
 * ahí validan [checkoutContactIsValid] y [exceedsSunatMaxMontoSinRuc]. Manda el backend igual.
 */
fun countryBuyerError(contact: ContactBrief?, total: Double): String? {
    val locale = TenantLocaleHolder.current
    val max = locale.finalConsumerMax ?: return null
    if (contact == null || !isFinalConsumerContact(contact.docType)) return null
    if (total <= max + 0.005) return null
    val finalConsumer = locale.finalConsumerIdentity?.label?.lowercase() ?: "consumidor final"
    return "A $finalConsumer el monto máximo es ${Money.format(max)}. " +
        "Identifique al cliente con su ${LabelKey.NATIONAL_ID.text} o ${LabelKey.TAX_ID.text}. " +
        "Total: ${Money.format(total)}"
}

/**
 * Ecuador: la venta fue a consumidor final (sin documento, con el 9999999999999 del SRI o con el
 * 99999999 del cliente genérico). Al anularla no hay nota de crédito sino devolución interna. Perú: false.
 */
fun isFinalConsumerSale(docNumber: String?): Boolean {
    if (TenantLocaleHolder.current.usesSunatBuyerRules) return false
    val n = docNumber?.trim().orEmpty()
    return n.isEmpty() || n == "99999999" || (n.length == 13 && n.all { it == '9' })
}

/** Monto máximo SUNAT para boleta con cliente doc. tipo 0 (paridad backend). */
const val SUNAT_MAX_MONTO_CLIENTE_SIN_RUC = 700.0

fun exceedsSunatMaxMontoSinRuc(
    total: Double,
    contact: ContactBrief?,
    sunatCode: String?,
): Boolean {
    // Regla SUNAT: solo Perú. En Ecuador el tope es el de consumidor final ([countryBuyerError]).
    if (!TenantLocaleHolder.current.usesSunatBuyerRules) return false
    val code = sunatCode?.trim().orEmpty()
    if (code != "03" && code != "00") return false
    if (contact == null || contact.docType.trim() != "0") return false
    return total > SUNAT_MAX_MONTO_CLIENTE_SIN_RUC
}

fun sunatMaxMontoSinRucMessage(total: Double): String =
    "Con cliente sin ${LabelKey.TAX_ID.text} (doc. tipo 0) el monto máximo permitido es S/ ${SUNAT_MAX_MONTO_CLIENTE_SIN_RUC.toInt()} " +
        "para ${LabelKey.DOC_RECEIPT.text.lowercase()}. Total actual: S/ ${"%.2f".format(total)}"

fun normalizePaymentMethodCodeForLookup(code: String): String =
    when (code.trim().lowercase()) {
        "efectivo" -> "cash"
        else -> code.trim().lowercase()
    }

fun findPaymentMethodRecord(methods: List<PaymentMethodOption>, code: String): PaymentMethodOption? {
    if (methods.isEmpty()) return null
    val want = normalizePaymentMethodCodeForLookup(code)
    return methods.firstOrNull { normalizePaymentMethodCodeForLookup(it.code) == want }
}

fun isPaymentMethodLinkedForSale(
    method: PaymentMethodOption,
    bankAccounts: List<BankAccountBrief>,
): Boolean {
    if (method.isCash) return true
    val bankId = method.bankAccountId ?: 0
    if (bankId > 0) return true
    val codeNorm = normalizePaymentMethodCodeForLookup(method.code)
    if (codeNorm.isEmpty()) return false
    return bankAccounts.any { account ->
        account.active && normalizePaymentMethodCodeForLookup(account.paymentMethod) == codeNorm
    }
}

fun needsCashSessionForPayments(
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentLine>,
): Boolean = payments.any { line ->
    val method = findPaymentMethodRecord(methods, line.method)
    method?.isCash == true ||
        (methods.isEmpty() && normalizePaymentMethodCodeForLookup(line.method) == "cash")
}
