package com.bendey.restaurant.core.domain.production

import com.bendey.restaurant.core.domain.model.AppResult

data class RecipeItem(
    val id: Int? = null,
    val productId: Int,
    val quantity: Double,
    val sortOrder: Int = 0,
)

data class Recipe(
    val id: Int,
    val productId: Int,
    val notes: String,
    val active: Boolean,
)

data class RecipeDetail(
    val recipe: Recipe,
    val items: List<RecipeItem>,
)

data class LowStockInsumo(
    val productId: Int,
    val code: String,
    val name: String,
    val unit: String,
    val quantity: Double,
    val minStock: Double,
)

data class PlateMarginRow(
    val productId: Int,
    val name: String,
    val qtySold: Double,
    val revenue: Double,
    val cost: Double,
    val margin: Double,
)

/** Motor de recetas — espejo de internal/production en el backend. Toda la lógica de costeo y
 * descuento de insumos vive en el backend; este repositorio solo expone su API. */
interface ProductionRepository {
    suspend fun getRecipe(productId: Int): AppResult<RecipeDetail?>
    suspend fun upsertRecipe(productId: Int, notes: String, items: List<RecipeItem>): AppResult<Recipe>
    suspend fun deleteRecipe(productId: Int): AppResult<Unit>
    suspend fun getRecipeCost(productId: Int, branchId: Int? = null): AppResult<Double>
    suspend fun lowStockInsumos(branchId: Int? = null): AppResult<List<LowStockInsumo>>
    suspend fun plateMargin(branchId: Int? = null, from: String? = null, to: String? = null): AppResult<List<PlateMarginRow>>
}
