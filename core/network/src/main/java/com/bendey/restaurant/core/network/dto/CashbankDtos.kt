package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CashSessionDto(
    val id: Int,
    @SerialName("branch_id") val branchId: Int,
    @SerialName("branch_name") val branchName: String? = null,
    @SerialName("opened_by") val openedBy: Int = 0,
    @SerialName("opened_by_name") val openedByName: String? = null,
    @SerialName("opening_balance") val openingBalance: Double = 0.0,
    @SerialName("closing_balance") val closingBalance: Double? = null,
    @SerialName("expected_balance") val expectedBalance: Double? = null,
    @SerialName("current_balance") val currentBalance: Double? = null,
    @SerialName("total_income") val totalIncome: Double? = null,
    @SerialName("total_expense") val totalExpense: Double? = null,
    val difference: Double? = null,
    val status: String = "open",
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    val notes: String? = null,
    @SerialName("arqueo_json") val arqueoJson: String? = null,
)

@Serializable
data class CashSessionResponseDto(
    val data: CashSessionDto? = null,
)

@Serializable
data class OpenCashSessionRequestDto(
    @SerialName("branch_id") val branchId: Int,
    @SerialName("opening_balance") val openingBalance: Double,
    val notes: String? = null,
)

@Serializable
data class CloseCashSessionRequestDto(
    @SerialName("closing_balance") val closingBalance: Double? = null,
    val notes: String? = null,
    val arqueo: Map<String, Int>? = null,
)

@Serializable
data class SaveArqueoRequestDto(
    val arqueo: Map<String, Int>,
)

@Serializable
data class SaveArqueoResponseDto(
    val sum: Double? = null,
)

@Serializable
data class CashReportRowDto(
    val date: String = "",
    val type: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    val reference: String = "",
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
)

@Serializable
data class CashReportTotalsDto(
    @SerialName("total_income") val totalIncome: Double = 0.0,
    @SerialName("total_expense") val totalExpense: Double = 0.0,
    @SerialName("total_sales") val totalSales: Double = 0.0,
    @SerialName("final_balance") val finalBalance: Double = 0.0,
    @SerialName("total_net_sales") val totalNetSales: Double? = null,
    @SerialName("total_voided_sales") val totalVoidedSales: Double? = null,
    @SerialName("total_pending_refunds") val totalPendingRefunds: Double? = null,
)

@Serializable
data class CashMethodTotalDto(val method: String = "", val total: Double = 0.0)

@Serializable
data class CashCancelledSaleRowDto(
    val date: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    val reason: String = "",
)

/**
 * Venta anulada cobrada en esta caja a la que nadie registro la devolucion. Anular ya no saca plata
 * sola, asi que si el cajero la entrego y no lo registro, esto es el faltante que va a aparecer al
 * contar.
 */
@Serializable
data class CashPendingRefundRowDto(
    @SerialName("sale_id") val saleId: Int = 0,
    val date: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    val amount: Double = 0.0,
)

@Serializable
data class CashTotalsByMethodDto(
    val sales: List<CashMethodTotalDto> = emptyList(),
    val purchases: List<CashMethodTotalDto> = emptyList(),
    val movements: List<CashMethodTotalDto> = emptyList(),
)

@Serializable
data class CashSessionReportDto(
    val session: CashSessionDto,
    @SerialName("income_detail") val incomeDetail: List<CashReportRowDto> = emptyList(),
    @SerialName("expense_detail") val expenseDetail: List<CashReportRowDto> = emptyList(),
    @SerialName("cancelled_sales_detail") val cancelledSalesDetail: List<CashCancelledSaleRowDto> = emptyList(),
    @SerialName("pending_refunds") val pendingRefunds: List<CashPendingRefundRowDto> = emptyList(),
    @SerialName("totals_by_method") val totalsByMethod: CashTotalsByMethodDto? = null,
    @SerialName("non_cash_sales_by_method") val nonCashSalesByMethod: List<CashMethodTotalDto> = emptyList(),
    /**
     * Neto REAL por método no efectivo: ventas − compras − egresos + reversas. NO es lo mismo que
     * [nonCashSalesByMethod], que solo mira cobros de venta: un egreso pagado por Yape baja este número
     * y no toca aquél.
     */
    @SerialName("non_cash_by_method") val nonCashByMethod: List<CashMethodTotalDto> = emptyList(),
    val totals: CashReportTotalsDto = CashReportTotalsDto(),
)

@Serializable
data class CashSessionReportResponseDto(
    val data: CashSessionReportDto? = null,
)

@Serializable
data class CashMovementDto(
    val id: Int,
    @SerialName("session_id") val sessionId: Int? = null,
    @SerialName("cash_session_id") val cashSessionId: Int? = null,
    val type: String,
    val category: String = "",
    val reference: String = "",
    @SerialName("payment_method") val paymentMethod: String? = null,
    val amount: Double,
    val notes: String? = null,
    val titular: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AddCashMovementRequestDto(
    val type: String,
    val category: String,
    val reference: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val amount: Double,
    val notes: String? = null,
    /** A nombre de quién es el ingreso/egreso manual. */
    val titular: String? = null,
)

@Serializable
data class CashMovementResponseDto(
    val data: CashMovementDto? = null,
)

@Serializable
data class BankAccountDto(
    val id: Int,
    val name: String,
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    val currency: String = "PEN",
    val balance: Double = 0.0,
    val type: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    val active: Boolean = true,
)

@Serializable
data class BankAccountUpsertRequestDto(
    val name: String,
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_number") val accountNumber: String,
    val currency: String = "PEN",
    val type: String = "checking",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("initial_balance") val initialBalance: Double? = null,
    val active: Boolean? = null,
)

@Serializable
data class OperationalStatusDto(
    @SerialName("open_tables_count") val openTablesCount: Int = 0,
    @SerialName("open_sessions_count") val openSessionsCount: Int = 0,
    @SerialName("pending_billing_count") val pendingBillingCount: Int = 0,
    @SerialName("active_comandas_count") val activeComandasCount: Int = 0,
    @SerialName("has_active_operations") val hasActiveOperations: Boolean = false,
)

@Serializable
data class OperationalStatusResponseDto(
    val data: OperationalStatusDto? = null,
)

@Serializable
data class BankMovementDto(
    val id: Int,
    @SerialName("bank_account_id") val bankAccountId: Int = 0,
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val reference: String = "",
    val date: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AddBankMovementRequestDto(
    val type: String,
    val description: String,
    val reference: String? = null,
    val amount: Double,
    val date: String,
)

@Serializable
data class PaymentMethodUpsertRequestDto(
    val name: String,
    val code: String,
    @SerialName("destination_type") val destinationType: String = "cash",
    @SerialName("bank_account_id") val bankAccountId: Int? = null,
    val active: Boolean? = null,
)

@Serializable
data class MovementReportRowDto(
    val date: String = "",
    val type: String = "",
    @SerialName("doc_number") val docNumber: String = "",
    @SerialName("contact_name") val contactName: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("branch_name") val branchName: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    val amount: Double = 0.0,
    @SerialName("movement_id") val movementId: Int = 0,
    @SerialName("cash_session_id") val cashSessionId: Int = 0,
    val category: String? = null,
    @SerialName("cash_reference") val cashReference: String? = null,
    @SerialName("notes_detail") val notesDetail: String? = null,
    /** Anulado desde otra terminal: la fila sigue en la lista, pero su importe ya no es plata. */
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancel_reason") val cancelReason: String? = null,
)

@Serializable
data class MovementsReportSummaryDto(
    @SerialName("total_rows") val totalRows: Int = 0,
    @SerialName("sum_income") val sumIncome: Double = 0.0,
    @SerialName("sum_expense") val sumExpense: Double = 0.0,
    @SerialName("net_movement") val netMovement: Double = 0.0,
)

@Serializable
data class MovementsReportResponseDto(
    val data: List<MovementReportRowDto> = emptyList(),
    val total: Int = 0,
    val summary: MovementsReportSummaryDto? = null,
)

/** Totales de un metodo electronico en el periodo: las dos patas y el neto que se muestra. */
@Serializable
data class BankMethodTotalsDto(
    val method: String = "",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    /** Lo calcula el backend, no el cliente: es EL numero que el encargado lee, y dejarlo a cada
     *  frontend es como Android y el escritorio terminan mostrando cosas distintas. */
    val net: Double = 0.0,
    @SerialName("income_count") val incomeCount: Int = 0,
    @SerialName("expense_count") val expenseCount: Int = 0,
)

@Serializable
data class BankMovementsReportSummaryDto(
    @SerialName("total_rows") val totalRows: Int = 0,
    @SerialName("sum_income") val sumIncome: Double = 0.0,
    @SerialName("sum_expense") val sumExpense: Double = 0.0,
    @SerialName("net_movement") val netMovement: Double = 0.0,
    @SerialName("by_method") val byMethod: List<BankMethodTotalsDto> = emptyList(),
)

@Serializable
data class BankMovementReportRowDto(
    val id: Int = 0,
    val date: String = "",
    /** credit = entro a la cuenta; debit = salio. */
    val type: String = "",
    val amount: Double = 0.0,
    val method: String = "",
    @SerialName("account_name") val accountName: String = "",
    val description: String = "",
    val reference: String = "",
    @SerialName("user_name") val userName: String = "",
)

@Serializable
data class BankMovementsReportResponseDto(
    val data: List<BankMovementReportRowDto> = emptyList(),
    val total: Int = 0,
    val summary: BankMovementsReportSummaryDto? = null,
)

@Serializable
data class SessionProductSoldDto(
    @SerialName("product_id") val productId: Int? = null,
    val code: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val total: Double = 0.0,
)

/**
 * Plato que salió de cocina dentro de un combo. SIN importe: la plata ya está contada en la línea del
 * combo, en [SessionProductSoldDto]. Sumar las dos listas daría el doble de lo vendido.
 */
@Serializable
data class SessionComboComponentDto(
    @SerialName("product_id") val productId: Int = 0,
    val code: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
)

/**
 * La respuesta del reporte de productos de una sesión.
 *
 * Los platos de combos vienen en una clave APARTE de `data` a propósito: un combo se guarda como una
 * sola línea de venta y sus platos viven dentro de su snapshot, así que el reporte mostraba
 * «1x PROMOCION BRASERITO» y ni rastro del pollo o la papa. Meterlos dentro de `data` haría que el
 * total de unidades contara dos veces lo mismo.
 */
@Serializable
data class SessionProductsReportResponseDto(
    val data: List<SessionProductSoldDto> = emptyList(),
    @SerialName("combo_components") val comboComponents: List<SessionComboComponentDto> = emptyList(),
)

@Serializable
data class PaymentMethodSummaryDto(
    val method: String = "",
    val count: Int = 0,
    val total: Double = 0.0,
)

@Serializable
data class PaymentDetailRowDto(
    val date: String = "",
    @SerialName("sale_number") val saleNumber: String = "",
    @SerialName("order_code") val orderCode: String = "",
    @SerialName("order_type") val orderType: String = "",
    @SerialName("user_name") val userName: String? = null,
    val method: String = "",
    val amount: Double = 0.0,
    val reference: String? = null,
)

@Serializable
data class PaymentsReportResponseDto(
    @SerialName("by_method") val byMethod: List<PaymentMethodSummaryDto> = emptyList(),
    @SerialName("total_income") val totalIncome: Double = 0.0,
    @SerialName("total_count") val totalCount: Int = 0,
    val detail: List<PaymentDetailRowDto> = emptyList(),
)
