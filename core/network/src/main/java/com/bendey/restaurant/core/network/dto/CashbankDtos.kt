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
