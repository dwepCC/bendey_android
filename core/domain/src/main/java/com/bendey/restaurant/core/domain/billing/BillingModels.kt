package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.model.AppResult

data class DocumentSeries(
    val id: Int,
    val branchId: Int,
    val docType: String,
    val series: String,
    val category: String,
    val sunatCode: String?,
    val active: Boolean,
    val currentNumber: Int = 0,
    val locked: Boolean = false,
    val canDelete: Boolean = true,
) {
    val displayLabel: String get() = "$docType · $series"
}

data class ContactBrief(
    val id: Int,
    val docType: String,
    val docNumber: String,
    val businessName: String,
    val active: Boolean,
) {
    val displayLabel: String get() = businessName.ifBlank { docNumber }.ifBlank { "#$id" }
}

data class BankAccountBrief(
    val id: Int,
    val paymentMethod: String,
    val active: Boolean,
)

data class PaymentMethodOption(
    val id: Int,
    val name: String,
    val code: String,
    val destinationType: String,
    val bankAccountId: Int? = null,
    val active: Boolean,
) {
    val isCash: Boolean get() = destinationType == "cash" || code.equals("cash", ignoreCase = true)
}

data class CheckoutPaymentLine(
    val method: String,
    val amount: Double,
    val reference: String = "",
)

data class CheckoutMeta(
    val series: List<DocumentSeries>,
    val contacts: List<ContactBrief>,
    val paymentMethods: List<PaymentMethodOption>,
    val bankAccounts: List<BankAccountBrief> = emptyList(),
    val sunatEnabled: Boolean = false,
    val taxRate: Double = DEFAULT_TAX_RATE_PERCENT,
    val igvRegime: String = "standard",
    val taxBenefitZone: Boolean = false,
)

data class BillSessionInput(
    val seriesId: Int,
    val docType: String,
    val contactId: Int?,
    val cashSessionId: Int?,
    val closeSession: Boolean = true,
    val comandaIds: List<Int>,
    val discountAmount: Double? = null,
    val payments: List<CheckoutPaymentLine>,
)

data class BillSessionResult(
    val saleId: Int,
    val number: String,
    val total: Double,
    val printData: SalePrintData? = null,
)

data class VoidCreditNoteResult(
    val message: String?,
    val async: Boolean,
)

data class BillingActionResult(
    val message: String?,
    val billingStatus: String?,
    val async: Boolean,
)

data class SalePrintData(
    val docType: String,
    val sunatCode: String = "",
    val number: String,
    val issueDate: String,
    val issueTime: String? = null,
    val companyName: String,
    val companyLegalName: String? = null,
    val companyRuc: String,
    val companyAddress: String?,
    val companyPhone: String? = null,
    val companyEmail: String? = null,
    val companyWebsite: String? = null,
    val companyLogoUrl: String? = null,
    val branchName: String?,
    val clientName: String?,
    val clientDocNumber: String?,
    val items: List<SalePrintLine>,
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
    val currency: String,
    val payments: List<SalePrintPayment>,
    val legendText: String?,
    val qrData: String? = null,
    val sunatHash: String? = null,
)

data class SalePrintLine(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
)

data class SalePrintPayment(
    val method: String,
    val amount: Double,
)

enum class BillingDocumentKind(
    val pathSegment: String,
    val defaultFileName: String,
    val mimeType: String,
) {
    XML("xml", "comprobante-enviado.xml", "application/xml"),
    XML_GENERATED("xml-generated", "comprobante-generado.xml", "application/xml"),
    CDR("cdr", "comprobante.cdr.zip", "application/zip"),
    PDF("pdf", "comprobante.pdf", "application/pdf"),
}

data class BillQuickSaleInput(
    val seriesId: Int,
    val docType: String,
    val contactId: Int?,
    val cashSessionId: Int?,
    val discountAmount: Double? = null,
    val notes: String? = null,
    val items: List<com.bendey.restaurant.core.domain.restaurant.OrderItemInput>,
    val payments: List<CheckoutPaymentLine>,
)

interface BillingRepository {
    suspend fun loadCheckoutMeta(branchId: Int): AppResult<CheckoutMeta>
    suspend fun refreshCheckoutMeta(branchId: Int): AppResult<CheckoutMeta>
    suspend fun billSession(sessionId: Int, input: BillSessionInput): AppResult<BillSessionResult>
    suspend fun billQuickSale(input: BillQuickSaleInput): AppResult<BillSessionResult>
    suspend fun voidWithCreditNote(saleId: Int, reason: String): AppResult<VoidCreditNoteResult>
    suspend fun sendToSunat(saleId: Int): AppResult<BillingActionResult>
    suspend fun resendToSunat(saleId: Int): AppResult<BillingActionResult>
    suspend fun downloadOfficialPdf(saleId: Int): AppResult<java.io.File>
    suspend fun downloadBillingDocument(saleId: Int, kind: BillingDocumentKind): AppResult<java.io.File>
    suspend fun loadBillingDocumentText(saleId: Int, kind: BillingDocumentKind): AppResult<String>
}
