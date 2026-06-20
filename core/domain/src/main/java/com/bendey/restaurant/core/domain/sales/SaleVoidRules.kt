package com.bendey.restaurant.core.domain.sales

/** Pestañas del listado de ventas (paridad web). */
enum class VentasTab(val label: String) {
    NOTAS("Notas venta"),
    FACTURACION("Boletas/Facturas"),
    CREDITOS("Notas crédito"),
}

fun isSaleCancelled(status: String): Boolean =
    status.equals("cancelled", ignoreCase = true)

fun isNotaVenta(docType: String, sunatCode: String?): Boolean {
    val code = sunatCode?.trim().orEmpty()
    if (code == "00") return true
    return docType.contains("NOTA DE VENTA", ignoreCase = true)
}

fun isCreditNoteDoc(docType: String): Boolean =
    docType.contains("CREDITO", ignoreCase = true) ||
        docType.contains("CRÉDITO", ignoreCase = true)

fun isElectronicAccepted(billingStatus: String?): Boolean =
    billingStatus.equals("accepted", ignoreCase = true)

fun SaleDetail.canVoidWithCreditNote(): Boolean =
    !isSaleCancelled(status) &&
        isElectronicAccepted(billingStatus) &&
        !isCreditNoteDoc(docType) &&
        !isNotaVenta(docType, sunatCode)

fun SaleDetail.canCancelNotaVenta(): Boolean =
    !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        convertedTo.isNullOrBlank() &&
        electronicIssueSaleId == null

fun SaleSummary.canVoidWithCreditNote(): Boolean =
    !isSaleCancelled(status) &&
        isElectronicAccepted(billingStatus) &&
        !isCreditNoteDoc(docType) &&
        !isNotaVenta(docType, sunatCode)

fun SaleSummary.canCancelNotaVenta(): Boolean =
    !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        convertedTo.isNullOrBlank() &&
        electronicIssueSaleId == null
