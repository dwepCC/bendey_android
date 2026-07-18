package com.bendey.restaurant.core.domain.cash

enum class CashMovementType(val apiValue: String, val label: String) {
    INCOME("income", "Ingreso"),
    EXPENSE("expense", "Egreso"),
    ;

    companion object {
        fun fromApi(value: String): CashMovementType =
            entries.firstOrNull { it.apiValue == value } ?: INCOME
    }
}

enum class CashSessionStatus(val apiValue: String) {
    OPEN("open"),
    CLOSED("closed"),
    ;

    companion object {
        fun fromApi(value: String): CashSessionStatus =
            entries.firstOrNull { it.apiValue == value } ?: OPEN
    }
}

data class CashSession(
    val id: Int,
    val branchId: Int,
    val branchName: String?,
    val openedByName: String?,
    val openingBalance: Double,
    val expectedBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val status: CashSessionStatus,
    val openedAt: String?,
    val closedAt: String? = null,
    val closingBalance: Double? = null,
    val difference: Double? = null,
    val notes: String?,
    val arqueoJson: String? = null,
)

data class CashSessionBrief(
    val id: Int,
    val branchName: String?,
    val openedByName: String?,
    val openingBalance: Double,
    val closingBalance: Double?,
    val expectedBalance: Double,
    val status: CashSessionStatus,
    val openedAt: String?,
    val closedAt: String?,
)

data class CashReportRow(
    val date: String,
    val type: String,
    val docNumber: String,
    val reference: String,
    val amount: Double,
    val paymentMethod: String,
)

data class CashSessionReport(
    val session: CashSessionBrief,
    val incomeDetail: List<CashReportRow>,
    val expenseDetail: List<CashReportRow>,
    val cancelledSalesDetail: List<CashCancelledSaleRow> = emptyList(),
    val salesByMethod: List<CashMethodTotal> = emptyList(),
    val nonCashSalesByMethod: List<CashMethodTotal> = emptyList(),
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSales: Double,
    val finalBalance: Double,
    val totalNetSales: Double,
    val totalVoidedSales: Double,
)

data class CashMovement(
    val id: Int,
    val type: CashMovementType,
    val category: String,
    val reference: String,
    val amount: Double,
    val notes: String?,
    val titular: String?,
    val createdAt: String?,
)

data class AddCashMovementInput(
    val type: CashMovementType,
    val category: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
    val paymentMethod: String = "cash",
    /** A nombre de quién es el ingreso/egreso manual. */
    val titular: String = "",
)

data class CashPaymentMethod(
    val id: Int,
    val name: String,
    val code: String,
    val destinationType: String,
    val bankAccountId: Int?,
    val active: Boolean,
)

data class CashBankAccount(
    val id: Int,
    val name: String,
    val bankName: String,
    val accountNumber: String,
    val currency: String,
    val balance: Double,
    val type: String,
    val paymentMethod: String,
    val active: Boolean,
)

data class CashBankMovement(
    val id: Int,
    val type: String,
    val amount: Double,
    val description: String,
    val reference: String,
    val date: String,
)

data class CashMethodTotal(val method: String, val total: Double)

data class CashCancelledSaleRow(
    val date: String,
    val docNumber: String,
    val amount: Double,
    val paymentMethod: String,
    val reason: String,
)

data class CashMovementReportRow(
    val date: String,
    val type: String,
    val docNumber: String,
    val contactName: String,
    val userName: String,
    val branchName: String,
    val paymentMethod: String,
    val amount: Double,
    val movementId: Int,
    val cashSessionId: Int,
    val category: String?,
    val cashReference: String?,
    val notesDetail: String?,
)

data class CashMovementsReportSummary(
    val totalRows: Int = 0,
    val sumIncome: Double = 0.0,
    val sumExpense: Double = 0.0,
    val netMovement: Double = 0.0,
)

data class CashMovementsReportPage(
    val rows: List<CashMovementReportRow>,
    val total: Int,
    val summary: CashMovementsReportSummary,
)

data class CashMovementsReportQuery(
    val branchId: Int?,
    val userId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val sessionId: Int? = null,
    val type: String? = null,
    val paymentMethod: String? = null,
    val page: Int = 1,
    val perPage: Int = 25,
)

data class CashPaymentDetailRow(
    val date: String,
    val saleNumber: String,
    val orderCode: String,
    val orderType: String,
    val userName: String?,
    val method: String,
    val amount: Double,
    val reference: String?,
)

data class CashPaymentsReport(
    val byMethod: List<CashMethodTotalWithCount>,
    val totalIncome: Double,
    val totalCount: Int,
    val detail: List<CashPaymentDetailRow>,
)

data class CashMethodTotalWithCount(
    val method: String,
    val total: Double,
    val count: Int,
)

data class CashSessionProductSold(
    val productId: Int?,
    val code: String,
    val description: String,
    val quantity: Double,
    val total: Double,
)

data class CashFilterUser(
    val userId: Int,
    val name: String,
)
