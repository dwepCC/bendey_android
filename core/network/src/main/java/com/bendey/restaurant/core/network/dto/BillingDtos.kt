package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class BillSessionRequestDto(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("doc_type") val docType: String,
    val currency: String = "PEN",
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("cash_session_id") val cashSessionId: Int? = null,
    @SerialName("close_session") val closeSession: Boolean = true,
    @SerialName("comanda_ids") val comandaIds: List<Int>,
    // Dividir por unidades (opcional; nulo = no viaja).
    @SerialName("comanda_quantities") val comandaQuantities: List<ComandaQuantityDto>? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val payments: List<BillPaymentDto>,
    // "detailed" (default) | "consumption". El backend revalida siempre contra la sucursal.
    @SerialName("detail_mode") val detailMode: String? = null,
    // Propina voluntaria (no va en el comprobante ni suma al total).
    @SerialName("tip_amount") val tipAmount: Double? = null,
    @SerialName("tip_method") val tipMethod: String? = null,
    @SerialName("tip_staff_id") val tipStaffId: Int? = null,
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
    // "detailed" (default) | "consumption". El backend revalida siempre contra la sucursal.
    @SerialName("detail_mode") val detailMode: String? = null,
    // Propina voluntaria (no va en el comprobante ni suma al total).
    @SerialName("tip_amount") val tipAmount: Double? = null,
    @SerialName("tip_method") val tipMethod: String? = null,
    @SerialName("tip_staff_id") val tipStaffId: Int? = null,
)

@Serializable
data class BillPaymentDto(
    val method: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
    /** Ecuador: «debit» | «credit» en un pago con tarjeta. */
    @SerialName("card_type") val cardType: String? = null,
)

@Serializable
data class BillSessionResponseDto(
    val success: Boolean = true,
    val data: BillSessionResultDto? = null,
    @SerialName("print_data") val printData: PrintDataDto? = null,
    /**
     * R6: el servidor lo manda `true` UNA vez por restaurante. Se lee como JsonElement para que un
     * valor raro (texto, número, null) nunca rompa la deserialización del cobro; ver [isFirstSale].
     */
    @SerialName("first_sale") val firstSale: JsonElement? = null,
    /** Texto si la propina quedó anotada pero su movimiento de caja falló (o el monto era inválido). */
    @SerialName("tip_warning") val tipWarning: String? = null,
) {
    /** Booleano estricto: solo el literal JSON `true` cuenta. */
    val isFirstSale: Boolean
        get() = (firstSale as? JsonPrimitive)?.let { !it.isString && it.content == "true" } == true
}

@Serializable
data class PrintDataDto(
    @SerialName("doc_type") val docType: String = "",
    @SerialName("sunat_code") val sunatCode: String = "",
    val series: String = "",
    /** Entero reservado al emitir. Con `series` compone el numero a mostrar. */
    val correlative: Int = 0,
    val number: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("issue_time") val issueTime: String? = null,
    val currency: String = "PEN",
    @SerialName("legend_text") val legendText: String? = null,
    val client: PrintClientDto? = null,
    val company: PrintCompanyDto? = null,
    val branch: PrintBranchDto? = null,
    val items: List<PrintItemDto> = emptyList(),
    /** Solo llegan cuando hubo vuelto: efectivo entregado y cambio devuelto. */
    @SerialName("amount_paid") val amountPaid: Double = 0.0,
    val change: Double = 0.0,
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    /** Recargo al Consumo YA CALCULADO por el backend — nunca se recalcula en el cliente. */
    @SerialName("service_charge_amount") val serviceChargeAmount: Double = 0.0,
    /** Porcentaje de RC YA CONGELADO (ver serviceChargeAmount) — solo para el label ("RC 5%"). */
    @SerialName("service_charge_rate") val serviceChargeRate: Double = 0.0,
    val payments: List<PrintPaymentDto> = emptyList(),
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("qr_data") val qrData: String = "",
    @SerialName("sunat_hash") val sunatHash: String? = null,
    @SerialName("shows_bendey_branding") val showsBendeyBranding: Boolean? = null,
    /** Base e impuesto por código de afectación (Perú 10/20/30/40; Ecuador la tarifa del SRI). */
    @SerialName("totals_by_affectation") val totalsByAffectation: Map<String, PrintAffectTotalDto> = emptyMap(),
    /** Ecuador: clave de acceso y ambiente del SRI (el comprobante no lleva QR ni hash). */
    @SerialName("access_key") val accessKey: String? = null,
    @SerialName("fiscal_environment") val fiscalEnvironment: String? = null,
    /** Ecuador: número de autorización del SRI (vacío mientras no autorice). */
    @SerialName("authorization_number") val authorizationNumber: String? = null,
    /** Ecuador: datos del RIDE que Perú no tiene (emisor, hora de autorización, formas de pago del SRI). */
    val ec: PrintEcDto? = null,
    /** Solo NC/ND: comprobante afectado, su fecha y el motivo. */
    @SerialName("affected_doc_type") val affectedDocType: String? = null,
    @SerialName("affected_doc_number") val affectedDocNumber: String? = null,
    @SerialName("affected_issue_date") val affectedIssueDate: String? = null,
    @SerialName("note_reason") val noteReason: String? = null,
    val notes: String? = null,
)

/** Ecuador: `print_data.ec` del backend. Todo opcional; ausente en Perú. */
@Serializable
data class PrintEcDto(
    /** dd/MM/yyyy HH:mm:ss; vacío mientras el SRI no autorice. */
    @SerialName("authorized_at") val authorizedAt: String? = null,
    @SerialName("matriz_address") val matrizAddress: String? = null,
    @SerialName("accounting_required") val accountingRequired: Boolean = false,
    @SerialName("special_contributor") val specialContributor: String? = null,
    @SerialName("withholding_agent") val withholdingAgent: String? = null,
    @SerialName("rimpe_legend") val rimpeLegend: String? = null,
    @SerialName("client_email") val clientEmail: String? = null,
    val payments: List<PrintEcPaymentDto> = emptyList(),
)

@Serializable
data class PrintEcPaymentDto(
    val code: String = "",
    val label: String = "",
    val amount: Double = 0.0,
)

@Serializable
data class PrintAffectTotalDto(
    val code: String = "",
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
)

@Serializable
data class PrintClientDto(
    @SerialName("doc_type") val docType: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    @SerialName("business_name") val businessName: String = "",
    val address: String? = null,
    val phone: String? = null,
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
    /** Nota adicional del comprobante (términos, política de devoluciones…). */
    @SerialName("additional_notes") val additionalNotes: String? = null,
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
    val discount: Double = 0.0,
    /** Base sin impuesto de la línea (el RIDE de Ecuador imprime este valor). */
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    /** Presentación y extras elegidos; el nombre comercial vive acá, no en [description]. */
    @SerialName("modifiers_json") val modifiersJson: String? = null,
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
    /** Comprobantes emitidos sobre la serie: ventas, guías y cotizaciones. Decide si hay que advertir. */
    @SerialName("documents_count") val documentsCount: Int? = null,
    /** Número más alto ya emitido, para avisar cuando el correlativo nuevo lo pisa. */
    @SerialName("last_correlative_used") val lastCorrelativeUsed: Int? = null,
    val category: String = "",
    val active: Boolean = true,
    @SerialName("sunat_code") val sunatCode: String? = null,
    /** Congela la IDENTIDAD de la serie (serie, tipo, código SUNAT, categoría). El correlativo NO. */
    val locked: Boolean? = null,
    @SerialName("can_delete") val canDelete: Boolean? = null,
    /** El régimen del tenant no permite emitir con esta serie: hoy, un NRUS no emite factura. */
    @SerialName("regime_blocked") val regimeBlocked: Boolean = false,
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
    /** PIN de operaciones del negocio: anular una venta no puede quedar a criterio de quien cobra. */
    val pin: String,
)

@Serializable
data class CancelSaleResponseDto(
    val success: Boolean = true,
    val message: String? = null,
)

@Serializable
data class VoidCreditNoteRequestDto(
    val reason: String,
    /** PIN de operaciones del negocio, igual que al anular una nota de venta. */
    val pin: String,
)

@Serializable
data class VoidCreditNoteResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    val async: Boolean = false,
    /**
     * Ecuador, factura a consumidor final: no hay nota de crédito; el backend registra una devolución
     * interna (dinero y stock vuelven) y la factura sigue vigente ante el SRI. Backends viejos no lo mandan.
     */
    @SerialName("internal_return") val internalReturn: Boolean = false,
)

@Serializable
data class IssueElectronicRequestDto(
    @SerialName("series_id") val seriesId: Int,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("contact_id") val contactId: Int? = null,
    // "detailed" | "consumption" — elección del usuario, no el modo de la nota (solo sugerencia
    // prellenada en la UI). El backend revalida contra la sucursal igual que en una venta nueva.
    @SerialName("detail_mode") val detailMode: String? = null,
    // Propina voluntaria (no va en el comprobante ni suma al total).
    @SerialName("tip_amount") val tipAmount: Double? = null,
    @SerialName("tip_method") val tipMethod: String? = null,
    @SerialName("tip_staff_id") val tipStaffId: Int? = null,
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
    /** Entero que el backend reserva al emitir. Con `series` compone el numero a mostrar; `number`
     *  es la concatenacion de ambos y no debe usarse para eso. */
    val correlative: Int = 0,
    val number: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("contact_name") val contactName: String? = null,
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    /** Recargo al Consumo YA CALCULADO por el backend — nunca se recalcula en el cliente. */
    @SerialName("service_charge_amount") val serviceChargeAmount: Double = 0.0,
    /** Porcentaje de RC YA CONGELADO (ver serviceChargeAmount) — solo para el label ("RC 5%"). */
    @SerialName("service_charge_rate") val serviceChargeRate: Double = 0.0,
    val currency: String = "PEN",
    val status: String = "",
    @SerialName("billing_status") val billingStatus: String? = null,
    @SerialName("sunat_code") val sunatCode: String? = null,
    @SerialName("converted_to") val convertedTo: String? = null,
    @SerialName("electronic_issue_sale_id") val electronicIssueSaleId: Int? = null,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("contact_id") val contactId: Int? = null,
    // Pedido del restaurante que originó la venta (habilita «Reabrir para cobrar de nuevo»).
    @SerialName("restaurant_session_id") val restaurantSessionId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Como se atendio la venta y en que mesa: los resuelve el backend desde la sesion del pedido.
    @SerialName("table_name") val tableName: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val payments: List<SalePaymentDto> = emptyList(),
    // Devolucion del dinero, calculada por el backend cruzando caja y banco. No se deduce de
    // `payments`: anular una venta y devolver la plata son hechos distintos.
    val refundable: Boolean = false,
    @SerialName("refundable_amount") val refundableAmount: Double = 0.0,
    @SerialName("refunded_amount") val refundedAmount: Double = 0.0,
    // "detailed" (default) | "consumption" — como se representa esta venta en el comprobante.
    // NUNCA cambia productos, stock, costos, caja ni Ledger. Ver internal/saledetail (backend).
    @SerialName("detail_mode") val detailMode: String? = null,
    @SerialName("detail_mode_description") val detailModeDescription: String? = null,
)

@Serializable
data class RefundSaleRequestDto(
    // El importe no viaja: lo deriva el backend de lo que la venta cobro de verdad.
    val confirmation: Boolean = true,
    val reason: String,
)

@Serializable
data class RefundSaleResultDto(
    @SerialName("sale_id") val saleId: Int = 0,
    val total: Double = 0.0,
    @SerialName("cash_refunded") val cashRefunded: Double = 0.0,
    @SerialName("bank_refunded") val bankRefunded: Double = 0.0,
    val reference: String = "",
)

@Serializable
data class RefundSaleResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    val data: RefundSaleResultDto? = null,
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

@Serializable
data class SalesByProductRowDto(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("category_name") val categoryName: String = "",
    val unit: String = "",
    @SerialName("quantity_sold") val quantitySold: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("lines_count") val linesCount: Int = 0,
    @SerialName("sales_count") val salesCount: Int = 0,
    @SerialName("avg_line_amount") val avgLineAmount: Double = 0.0,
)

@Serializable
data class SalesByProductSummaryDto(
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("total_quantity") val totalQuantity: Double = 0.0,
    @SerialName("line_items") val lineItems: Int = 0,
    @SerialName("distinct_sales") val distinctSales: Int = 0,
    @SerialName("products_count") val productsCount: Int = 0,
)

@Serializable
data class SalesByProductResponseDto(
    val data: List<SalesByProductRowDto> = emptyList(),
    val summary: SalesByProductSummaryDto? = null,
)

@Serializable
data class StuckVoidCreditNoteDto(
    @SerialName("original_sale_id") val originalSaleId: Int = 0,
    @SerialName("original_number") val originalNumber: String? = null,
    @SerialName("original_doc_type") val originalDocType: String? = null,
    @SerialName("original_total") val originalTotal: Double = 0.0,
    @SerialName("credit_note_id") val creditNoteId: Int = 0,
    @SerialName("credit_note_number") val creditNoteNumber: String? = null,
    @SerialName("sunat_message") val sunatMessage: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    /** true = no está rechazada, solo lleva demasiado tiempo sin resolverse. */
    val stalled: Boolean = false,
    /** La venta ya se anuló en el sistema (dinero y stock devueltos); falta solo que SUNAT acepte una NC. */
    @SerialName("original_cancelled") val originalCancelled: Boolean = false,
)

@Serializable
data class ComandaQuantityDto(
    @SerialName("comanda_id") val comandaId: Int,
    val quantity: Int,
)
