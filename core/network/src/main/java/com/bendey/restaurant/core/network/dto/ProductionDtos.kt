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

/** Costeo de una receta que todavía no se guardó — espejo de service.BorradorCosteado.
 *
 *  El cálculo vive en el backend a propósito: el costo de un insumo es el promedio de sus compras y,
 *  solo si no tiene ninguna, el precio declarado en su ficha. Repetir esa regla aquí y en Tauri daría
 *  dos oportunidades de mostrar un costo distinto al que el sistema usa para el margen.
 */
@Serializable
data class RecipeDraftCostItemDto(
    @SerialName("product_id") val productId: Int,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    val subtotal: Double = 0.0,
    @SerialName("sin_costear") val sinCostear: Boolean = false,
)

@Serializable
data class RecipeDraftCostDto(
    val total: Double = 0.0,
    val items: List<RecipeDraftCostItemDto> = emptyList(),
    @SerialName("sin_costear") val sinCostear: Int = 0,
)

@Serializable
data class RecipeDraftCostResponseDto(
    val data: RecipeDraftCostDto,
)

@Serializable
data class CostDraftRequestDto(
    val items: List<RecipeItemDto> = emptyList(),
)

@Serializable
data class LowStockInsumoDto(
    @SerialName("product_id") val productId: Int,
    val code: String = "",
    val name: String,
    val unit: String = "",
    val quantity: Double = 0.0,
    @SerialName("min_stock") val minStock: Double = 0.0,
    /** "insumo" | "comercial" — el reporte ya no cubre solo insumos. */
    @SerialName("product_type") val productType: String = "insumo",
)

@Serializable
data class LowStockInsumosResponseDto(
    val data: List<LowStockInsumoDto> = emptyList(),
)

@Serializable
data class LowStockCountDto(
    val count: Int = 0,
)

@Serializable
data class LowStockCountResponseDto(
    val data: LowStockCountDto = LowStockCountDto(),
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
