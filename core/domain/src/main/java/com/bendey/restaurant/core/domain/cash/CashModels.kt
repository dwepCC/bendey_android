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
    val notes: String?,
)

data class CashMovement(
    val id: Int,
    val type: CashMovementType,
    val category: String,
    val reference: String,
    val amount: Double,
    val notes: String?,
    val createdAt: String?,
)

data class AddCashMovementInput(
    val type: CashMovementType,
    val category: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
)
