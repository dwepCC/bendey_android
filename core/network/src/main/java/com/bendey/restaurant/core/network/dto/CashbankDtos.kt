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
    @SerialName("totals_by_method") val totalsByMethod: CashTotalsByMethodDto? = null,
    @SerialName("non_cash_sales_by_method") val nonCashSalesByMethod: List<CashMethodTotalDto> = emptyList(),
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
