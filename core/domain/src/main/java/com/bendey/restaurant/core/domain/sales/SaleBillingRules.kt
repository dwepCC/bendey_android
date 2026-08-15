package com.bendey.restaurant.core.domain.sales

fun isSaleConverted(convertedTo: String?, electronicIssueSaleId: Int?): Boolean =
    !convertedTo.isNullOrBlank() || electronicIssueSaleId != null

fun SaleSummary.isConverted(): Boolean = isSaleConverted(convertedTo, electronicIssueSaleId)

fun SaleDetail.isConverted(): Boolean = isSaleConverted(convertedTo, electronicIssueSaleId)

fun convertedToLabel(convertedTo: String?, electronicIssueSaleId: Int?): String? {
    convertedTo?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    electronicIssueSaleId?.let { return "#$it" }
    return null
}

fun normalizeBillingStatus(raw: String?): String {
    val s = raw?.trim()?.lowercase().orEmpty()
    val valid = setOf("pending", "sent", "accepted", "observed", "rejected", "error", "voided")
    return if (s in valid) s else "pending"
}

fun canSendToSunat(billingStatus: String?): Boolean =
    normalizeBillingStatus(billingStatus) == "pending"

fun canResendToSunat(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    return s == "error" || s == "sent" || s == "rejected"
}

/**
 * El texto del botón de envío según el estado.
 *
 * «Reenviar» afirma que hubo un envío previo, y con `error` eso puede ser falso: ese estado tapa dos
 * casos distintos —el comprobante que no llegó a armarse y nunca salió, y el que llegó a SUNAT y
 * falló—. A `negociacionescostaazulsac` le ofrecía «Reenviar» sobre 23 comprobantes que nunca habían
 * salido, y quien lo lee entiende que SUNAT ya los conoce, que es lo contrario de lo que pasaba.
 *
 * «Reintentar envío» es cierto en ambos casos: se reintenta la operación, no una transmisión que
 * quizá nunca ocurrió. Ver docs/BILLING-HALLAZGOS-2026-08-15.md (BUG-02).
 */
fun sunatSendActionLabel(billingStatus: String?): String =
    when (normalizeBillingStatus(billingStatus)) {
        "sent", "rejected" -> "Reenviar a SUNAT"
        else -> "Reintentar envío a SUNAT"
    }

fun canShowOfficialSunatPdf(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    if (s == "voided") return false
    return s == "sent" || s == "accepted" || s == "observed"
}

fun canShowCdr(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    if (s == "voided") return false
    return s == "accepted" || s == "observed" || s == "rejected"
}

fun canShowXmlSent(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    if (s == "voided") return false
    return s == "sent" || s == "accepted" || s == "observed" || s == "rejected"
}

fun canShowXmlGenerated(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    if (s == "voided") return false
    return s == "pending" || s == "error"
}

fun SaleDetail.canIssueElectronicFromNota(sunatEnabled: Boolean): Boolean =
    sunatEnabled &&
        !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        !isConverted()

fun SaleSummary.canIssueElectronicFromNota(sunatEnabled: Boolean): Boolean =
    sunatEnabled &&
        !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        !isConverted()

/** Filtros de estado SUNAT disponibles en pestaña facturación (paridad web). */
val BILLING_FILTER_STATUSES = listOf(
    "pending",
    "sent",
    "accepted",
    "rejected",
    "error",
    "voided",
)
