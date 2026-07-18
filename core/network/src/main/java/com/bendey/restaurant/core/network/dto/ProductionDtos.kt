package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val notes: String? = null,
    val active: Boolean = true,
)

@Serializable
data class RecipeItemDto(
    val id: Int? = null,
    @SerialName("product_id") val productId: Int,
    val quantity: Double,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class RecipeDetailDto(
    val recipe: RecipeDto,
    val items: List<RecipeItemDto> = emptyList(),
)

@Serializable
data class RecipeDetailResponseDto(
    val data: RecipeDetailDto? = null,
)

@Serializable
data class UpsertRecipeRequestDto(
    val notes: String = "",
    val items: List<RecipeItemDto> = emptyList(),
)

@Serializable
data class RecipeResponseDto(
    val data: RecipeDto,
)

@Serializable
data class RecipeCostDto(
    @SerialName("product_id") val productId: Int,
    @SerialName("branch_id") val branchId: Int,
    val cost: Double,
)

@Serializable
data class RecipeCostResponseDto(
    val data: RecipeCostDto,
)

@Serializable
data class LowStockInsumoDto(
    @SerialName("product_id") val productId: Int,
    val code: String = "",
    val name: String,
    val unit: String = "",
    val quantity: Double = 0.0,
    @SerialName("min_stock") val minStock: Double = 0.0,
)

@Serializable
data class LowStockInsumosResponseDto(
    val data: List<LowStockInsumoDto> = emptyList(),
)

@Serializable
data class PlateMarginRowDto(
    @SerialName("product_id") val productId: Int,
    val name: String,
    @SerialName("qty_sold") val qtySold: Double = 0.0,
    val revenue: Double = 0.0,
    val cost: Double = 0.0,
    val margin: Double = 0.0,
)

@Serializable
data class PlateMarginResponseDto(
    val data: List<PlateMarginRowDto> = emptyList(),
)
