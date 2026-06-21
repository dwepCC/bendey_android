package com.bendey.restaurant.core.domain.billing

const val BILLING_NOT_ENABLED_MESSAGE =
    "La facturación electrónica no está habilitada. Use nota de venta o active SUNAT en configuración."

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
    if (!series.active) return@filter false
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

fun checkoutContactIsValid(
    contact: ContactBrief?,
    docType: String,
    sunatCode: String?,
): Boolean {
    if (contact == null) return false
    if (isFacturaDocType(docType, sunatCode)) return isRucContact(contact)
    return true
}

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
