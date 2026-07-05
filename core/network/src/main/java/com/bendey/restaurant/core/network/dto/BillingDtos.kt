package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillSessionRequestDto(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("doc_type") val docType: String,
    val currency: String = "PEN",
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("cash_session_id") val cashSessionId: Int? = null,
    @SerialName("close_session") val closeSession: Boolean = true,
    @SerialName("comanda_ids") val comandaIds: List<Int>,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val payments: List<BillPaymentDto>,
)

@Serializable
data class BillQuickSaleRequestDto(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("doc_type") val docType: String,
    val currency: String = "PEN",
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("cash_session_id") val cashSessionId: Int? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val notes: String? = null,
    val items: List<OrderItemInputDto>,
    val payments: List<BillPaymentDto>,
)

@Serializable
data class BillPaymentDto(
    val method: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
)

@Serializable
data class BillSessionResponseDto(
    val success: Boolean = true,
    val data: BillSessionResultDto? = null,
    @SerialName("print_data") val printData: PrintDataDto? = null,
)

@Serializable
data class PrintDataDto(
    @SerialName("doc_type") val docType: String = "",
    @SerialName("sunat_code") val sunatCode: String = "",
    val series: String = "",
    val number: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("issue_time") val issueTime: String? = null,
    val currency: String = "PEN",
    @SerialName("legend_text") val legendText: String? = null,
    val client: PrintClientDto? = null,
    val company: PrintCompanyDto? = null,
    val branch: PrintBranchDto? = null,
    val items: List<PrintItemDto> = emptyList(),
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val payments: List<PrintPaymentDto> = emptyList(),
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("qr_data") val qrData: String = "",
    @SerialName("sunat_hash") val sunatHash: String? = null,
)

@Serializable
data class PrintClientDto(
    @SerialName("doc_type") val docType: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    @SerialName("business_name") val businessName: String = "",
    val address: String? = null,
)

@Serializable
data class PrintCompanyDto(
    val ruc: String = "",
    @SerialName("business_name") val businessName: String = "",
    @SerialName("trade_name") val tradeName: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
)

@Serializable
data class PrintBranchDto(
    val name: String = "",
    val address: String? = null,
)

@Serializable
data class PrintItemDto(
    val code: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val total: Double = 0.0,
)

@Serializable
data class PrintPaymentDto(
    val method: String = "",
    val amount: Double = 0.0,
    val reference: String? = null,
)

@Serializable
data class BillSessionResultDto(
    val id: Int,
    val number: String,
    val total: Double,
)

@Serializable
data class DocumentSeriesDto(
    val id: Int,
    @SerialName("branch_id") val branchId: Int,
    @SerialName("branch_name") val branchName: String? = null,
    @SerialName("doc_type") val docType: String,
    val series: String,
    @SerialName("current_number") val currentNumber: Int = 0,
    val correlative: Int? = null,
    @SerialName("sales_count") val salesCount: Int? = null,
    val category: String = "",
    val active: Boolean = true,
    @SerialName("sunat_code") val sunatCode: String? = null,
    val locked: Boolean? = null,
    @SerialName("can_delete") val canDelete: Boolean? = null,
)

@Serializable
data class SeriesCreateRequestDto(
    @SerialName("branch_id") val branchId: Int,
    @SerialName("doc_type") val docType: String,
    val series: String,
    val category: String = "venta",
    @SerialName("sunat_code") val sunatCode: String = "00",
)

@Serializable
data class SeriesUpdateRequestDto(
    val series: String,
    val active: Boolean = true,
    @SerialName("doc_type") val docType: String,
    @SerialName("sunat_code") val sunatCode: String = "00",
    val category: String = "venta",
    val correlative: Int? = null,
)

@Serializable
data class ContactDto(
    val id: Int,
    val type: String = "customer",
    @SerialName("doc_type") val docType: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    @SerialName("business_name") val businessName: String = "",
    @SerialName("trade_name") val tradeName: String = "",
    val address: String? = null,
    val ubigeo: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val active: Boolean = true,
)

@Serializable
data class PaymentMethodDto(
    val id: Int,
    val name: String,
    val code: String,
    @SerialName("destination_type") val destinationType: String,
    @SerialName("bank_account_id") val bankAccountId: Int? = null,
    val active: Boolean = true,
)

@Serializable
data class CancelSaleRequestDto(
    val reason: String,
)

@Serializable
data class CancelSaleResponseDto(
    val success: Boolean = true,
    val message: String? = null,
)

@Serializable
data class VoidCreditNoteRequestDto(
    val reason: String,
)

@Serializable
data class VoidCreditNoteResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    val async: Boolean = false,
)

@Serializable
data class IssueElectronicRequestDto(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("contact_id") val contactId: Int? = null,
)

@Serializable
data class IssueElectronicResponseDto(
    val sale: SaleDto? = null,
)

@Serializable
data class BillingActionResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    val async: Boolean = false,
    @SerialName("billing_status") val billingStatus: String? = null,
    @SerialName("sunat_message") val sunatMessage: String? = null,
)

@Serializable
data class SaleContactDto(
    val id: Int? = null,
    @SerialName("doc_type") val docType: String? = null,
    @SerialName("doc_number") val docNumber: String? = null,
    @SerialName("business_name") val businessName: String? = null,
)

@Serializable
data class SaleDto(
    val id: Int,
    @SerialName("doc_type") val docType: String = "",
    val series: String = "",
    val number: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("contact_name") val contactName: String? = null,
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "PEN",
    val status: String = "",
    @SerialName("billing_status") val billingStatus: String? = null,
    @SerialName("sunat_code") val sunatCode: String? = null,
    @SerialName("converted_to") val convertedTo: String? = null,
    @SerialName("electronic_issue_sale_id") val electronicIssueSaleId: Int? = null,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
)

@Serializable
data class SalePaymentTotalDto(
    val method: String = "",
    val total: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class SaleListSummaryDto(
    @SerialName("sum_total") val sumTotal: Double = 0.0,
    @SerialName("sum_active") val sumActive: Double = 0.0,
    @SerialName("count_active") val countActive: Int = 0,
    @SerialName("payment_totals") val paymentTotals: List<SalePaymentTotalDto> = emptyList(),
)

@Serializable
data class SalesListResponseDto(
    val data: List<SaleDto> = emptyList(),
    val total: Int = 0,
    val summary: SaleListSummaryDto? = null,
)

@Serializable
data class SaleItemDto(
    val id: Int = 0,
    val code: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val total: Double = 0.0,
)

@Serializable
data class SalePaymentDto(
    val id: Int = 0,
    val method: String = "",
    val amount: Double = 0.0,
    val reference: String? = null,
)

@Serializable
data class SaleDetailResponseDto(
    val sale: SaleDto? = null,
    val items: List<SaleItemDto> = emptyList(),
    val payments: List<SalePaymentDto> = emptyList(),
    @SerialName("print_data") val printData: PrintDataDto? = null,
    val contact: SaleContactDto? = null,
)
