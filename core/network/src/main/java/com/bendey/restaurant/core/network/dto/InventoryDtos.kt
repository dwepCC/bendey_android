package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockByBranchDto(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("branch_id") val branchId: Int = 0,
    val quantity: Double = 0.0,
)

@Serializable
data class StockByBranchListResponseDto(
    val data: List<StockByBranchDto> = emptyList(),
)

@Serializable
data class StockMovementDto(
    val id: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("branch_id") val branchId: Int = 0,
    @SerialName("branch_name") val branchName: String? = null,
    val type: String = "",
    val quantity: Double = 0.0,
    val balance: Double? = null,
    @SerialName("unit_cost") val unitCost: Double? = null,
    val reference: String? = null,
    val notes: String? = null,
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class StockMovementListResponseDto(
    val data: List<StockMovementDto> = emptyList(),
    val total: Int? = null,
)

@Serializable
data class InventoryAdjustmentRequestDto(
    @SerialName("product_id") val productId: Int,
    @SerialName("branch_id") val branchId: Int,
    val type: String,
    val quantity: Double,
    val notes: String,
)

@Serializable
data class OkResponseDto(
    val ok: Boolean = true,
)
