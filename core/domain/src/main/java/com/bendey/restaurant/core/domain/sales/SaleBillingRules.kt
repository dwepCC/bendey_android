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

fun canShowOfficialSunatPdf(billingStatus: String?): Boolean {
    val s = normalizeBillingStatus(billingStatus)
    if (s == "voided") return false
    return s == "sent" || s == "accepted" || s == "observed"
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
