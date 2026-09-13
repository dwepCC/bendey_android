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
    val payments: List<SalePaymentLine> = emptyList(),
    val sunatCode: String? = null,
    val convertedTo: String? = null,
    val electronicIssueSaleId: Int? = null,
    val branchId: Int? = null,
    val contactId: Int? = null,
    // Devolucion del dinero: la calcula el backend. `refundableAmount` es lo efectivamente cobrado
    // que se devolveria, no el total de la venta.
    val refundable: Boolean = false,
    val refundableAmount: Double = 0.0,
    val refundedAmount: Double = 0.0,
    // Mesa de la que salio la venta y como se atendio (dine_in, takeaway, delivery, quick_sale).
    // Vacio cuando la venta no salio del POS de restaurante.
    val tableName: String? = null,
    val orderType: String? = null,
    // "detailed" (default) | "consumption" — como se representa esta venta en el comprobante.
    // NUNCA cambia productos, stock, costos, caja ni Ledger. Ver internal/saledetail (backend).
    val detailMode: String? = null,
) {
    /** El repositorio ya lo compuso desde `series` y `correlative` al mapear la respuesta; aqui no
     *  queda nada que formatear. */
    val displayNumber: String get() = number
}

data class RefundResult(
    val saleId: Int,
    val total: Double,
    val cashRefunded: Double,
    val bankRefunded: Double,
    val reference: String,
)

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
    /**
     * Recargo al Consumo YA CALCULADO Y CONGELADO por el backend (TenantSaleCharge) — nunca se
     * recalcula acá. 0 cuando la venta no tiene RC. `total` ya lo incluye; este campo es solo
     * para desglosarlo en pantalla.
     */
    val serviceChargeAmount: Double = 0.0,
    /** Porcentaje de RC YA CONGELADO (ver serviceChargeAmount) — solo para el label ("RC 5%"). */
    val serviceChargeRate: Double = 0.0,
    val currency: String,
    val status: String,
    val billingStatus: String?,
    val sunatCode: String? = null,
    val convertedTo: String? = null,
    val electronicIssueSaleId: Int? = null,
    val branchId: Int? = null,
    val contactId: Int? = null,
    val refundable: Boolean = false,
    val refundableAmount: Double = 0.0,
    val refundedAmount: Double = 0.0,
    val contact: SaleContactBrief? = null,
    val items: List<SaleDetailLine>,
    val payments: List<SaleDetailPayment>,
    val printData: SalePrintData?,
    // "detailed" (default) | "consumption" — como se representa esta venta en el comprobante.
    // NUNCA cambia productos, stock, costos, caja ni Ledger. Ver internal/saledetail (backend).
    val detailMode: String? = null,
) {
    /** El repositorio ya lo compuso desde `series` y `correlative` al mapear la respuesta; aqui no
     *  queda nada que formatear. */
    val displayNumber: String get() = number
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

data class SalesByProductRow(
    val productId: Int,
    val productCode: String,
    val productName: String,
    val categoryName: String,
    val unit: String,
    val quantitySold: Double,
    val totalAmount: Double,
    val linesCount: Int,
    val salesCount: Int,
    val avgLineAmount: Double,
)

data class SalesByProductSummary(
    val totalAmount: Double = 0.0,
    val totalQuantity: Double = 0.0,
    val lineItems: Int = 0,
    val distinctSales: Int = 0,
    val productsCount: Int = 0,
)

data class SalesByProductPage(
    val rows: List<SalesByProductRow>,
    val summary: SalesByProductSummary,
)

interface SalesRepository {
    suspend fun listSales(
        from: String?,
        to: String?,
        tab: VentasTab = VentasTab.TODAS,
        page: Int = 1,
        perPage: Int = 25,
        query: String? = null,
        paymentMethod: String? = null,
        billingStatus: String? = null,
        orderType: String? = null,
    ): AppResult<SalesListPage>

    suspend fun listAllSalesForExport(
        from: String?,
        to: String?,
        tab: VentasTab,
        query: String? = null,
        paymentMethod: String? = null,
        billingStatus: String? = null,
        orderType: String? = null,
    ): AppResult<List<SaleSummary>>

    suspend fun getSaleDetail(saleId: Int): AppResult<SaleDetail>

    suspend fun cancelNotaVenta(saleId: Int, reason: String, pin: String): AppResult<CancelNotaResult>

    /**
     * Registra que el dinero de una venta volvio al cliente. El importe no se envia: lo deriva el
     * backend de lo que la venta cobro de verdad.
     */
    suspend fun refundSale(saleId: Int, reason: String): AppResult<RefundResult>

    suspend fun issueElectronicFromNota(
        saleId: Int,
        seriesId: Int,
        issueDate: String?,
        contactId: Int? = null,
        // "detailed" | "consumption" — eleccion del usuario, no el modo de la nota. El backend
        // revalida contra la sucursal igual que en una venta nueva.
        detailMode: String? = null,
    ): AppResult<IssueElectronicResult>

    suspend fun listSalesByProduct(
        from: String?,
        to: String?,
        branchId: Int? = null,
        categoryId: Int? = null,
    ): AppResult<SalesByProductPage>
}

