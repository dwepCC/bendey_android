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
    /** Congela la IDENTIDAD de la serie (serie, tipo, código SUNAT, categoría). El correlativo NO. */
    val locked: Boolean = false,
    /** Comprobantes emitidos: ventas, guías y cotizaciones. Decide si hay que advertir al renumerar. */
    val documentsCount: Int = 0,
    /** Número más alto ya emitido. Bajar por debajo repite comprobantes y SUNAT los rechaza. */
    val lastCorrelativeUsed: Int = 0,
    val canDelete: Boolean = true,
    /**
     * El régimen del tenant no permite emitir con esta serie: hoy, un NRUS no emite factura.
     *
     * La serie igual llega —en Ajustes hay que poder verla, y vuelve a servir si cambia de régimen—
     * pero el checkout no debe ofrecerla: el backend la rechaza al reservar el correlativo, o sea
     * recien despues de que el mozo eligio el tipo y cobro.
     */
    val regimeBlocked: Boolean = false,
) {
    val displayLabel: String get() = "$docType · $series"

    /** Se puede emitir con esta serie HOY: ni desactivada ni prohibida por el régimen. */
    val emisible: Boolean get() = active && !regimeBlocked
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
    val isNRUS: Boolean = false,
    val hasAmazonBenefit: Boolean = false,
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
    val series: String,
    /** Entero reservado al emitir. Con `series` compone el numero a mostrar. */
    val correlative: Int,
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
    /**
     * Recargo al Consumo YA CALCULADO Y CONGELADO por el backend (TenantSaleCharge) — nunca se
     * recalcula acá. 0 cuando la venta no tiene RC. `total` ya lo incluye; este campo es solo
     * para desglosarlo en pantalla/ticket.
     */
    val serviceChargeAmount: Double = 0.0,
    val currency: String,
    val payments: List<SalePrintPayment>,
    /** Solo con valor cuando hubo vuelto: efectivo entregado y cambio devuelto. */
    val amountPaid: Double = 0.0,
    val change: Double = 0.0,
    val legendText: String?,
    val qrData: String? = null,
    val sunatHash: String? = null,
    val showsBendeyBranding: Boolean = true,
)

data class SalePrintLine(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val discount: Double = 0.0,
    /** Presentación y extras elegidos; el nombre comercial vive acá, no en [description]. */
    val modifiersJson: String? = null,
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
    suspend fun voidWithCreditNote(saleId: Int, reason: String, pin: String): AppResult<VoidCreditNoteResult>
    suspend fun sendToSunat(saleId: Int): AppResult<BillingActionResult>
    suspend fun resendToSunat(saleId: Int): AppResult<BillingActionResult>
    suspend fun downloadOfficialPdf(saleId: Int): AppResult<java.io.File>
    suspend fun downloadBillingDocument(saleId: Int, kind: BillingDocumentKind): AppResult<java.io.File>
    suspend fun loadBillingDocumentText(saleId: Int, kind: BillingDocumentKind): AppResult<String>
}
