package com.bendey.restaurant.core.domain.sales

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.model.AppResult

data class SaleSummary(
    val id: Int,
    val docType: String,
    val number: String,
    val issueDate: String,
    val contactName: String?,
    val total: Double,
    val currency: String,
    val status: String,
    val billingStatus: String?,
    val paymentMethod: String?,
    val sunatCode: String? = null,
    val convertedTo: String? = null,
    val electronicIssueSaleId: Int? = null,
    val branchId: Int? = null,
    val contactId: Int? = null,
) {
    val displayNumber: String get() = formatSaleDocumentNumber(number)
}

data class CancelNotaResult(
    val message: String?,
)

data class IssueElectronicResult(
    val saleId: Int,
    val docType: String,
    val number: String,
    val message: String?,
)

data class SaleContactBrief(
    val id: Int?,
    val docType: String?,
    val docNumber: String?,
    val businessName: String?,
) {
    fun hasValidRuc(): Boolean {
        if (docType?.trim() != "6") return false
        val digits = docNumber?.replace(Regex("\\D"), "").orEmpty()
        return digits.length == 11
    }
}

data class SaleDetailLine(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
)

data class SaleDetailPayment(
    val method: String,
    val amount: Double,
    val reference: String?,
)

data class SaleDetail(
    val id: Int,
    val number: String,
    val docType: String,
    val issueDate: String,
    val contactName: String?,
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
    val currency: String,
    val status: String,
    val billingStatus: String?,
    val sunatCode: String? = null,
    val convertedTo: String? = null,
    val electronicIssueSaleId: Int? = null,
    val branchId: Int? = null,
    val contactId: Int? = null,
    val contact: SaleContactBrief? = null,
    val items: List<SaleDetailLine>,
    val payments: List<SaleDetailPayment>,
    val printData: SalePrintData?,
) {
    val displayNumber: String get() = formatSaleDocumentNumber(number)
}

data class SalePaymentTotal(
    val method: String,
    val total: Double,
    val count: Int = 0,
)

data class SaleListSummary(
    val sumTotal: Double = 0.0,
    val sumActive: Double = 0.0,
    val countActive: Int = 0,
    val paymentTotals: List<SalePaymentTotal> = emptyList(),
)

data class SalesListPage(
    val sales: List<SaleSummary>,
    val total: Int,
    val summary: SaleListSummary = SaleListSummary(),
)

interface SalesRepository {
    suspend fun listSales(
        from: String?,
        to: String?,
        tab: VentasTab = VentasTab.NOTAS,
        page: Int = 1,
        perPage: Int = 25,
        query: String? = null,
        paymentMethod: String? = null,
        billingStatus: String? = null,
    ): AppResult<SalesListPage>

    suspend fun listAllSalesForExport(
        from: String?,
        to: String?,
        tab: VentasTab,
        query: String? = null,
        paymentMethod: String? = null,
        billingStatus: String? = null,
    ): AppResult<List<SaleSummary>>

    suspend fun getSaleDetail(saleId: Int): AppResult<SaleDetail>

    suspend fun cancelNotaVenta(saleId: Int, reason: String): AppResult<CancelNotaResult>

    suspend fun issueElectronicFromNota(
        saleId: Int,
        seriesId: Int,
        issueDate: String?,
        contactId: Int? = null,
    ): AppResult<IssueElectronicResult>
}

fun formatSaleDocumentNumber(number: String): String {
    val num = number.trim()
    return if (num.isBlank()) "—" else num
}
