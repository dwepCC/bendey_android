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
    /** "insumo" | "comercial" — el reporte ya no cubre solo insumos. */
    val productType: String = "insumo",
)

data class PlateMarginRow(
    val productId: Int,
    val name: String,
    val qtySold: Double,
    val revenue: Double,
    val cost: Double,
    val margin: Double,
)

/** Lo que aporta un ingrediente al costo del plato. `sinCostear` marca el que no tiene de dónde
 *  valorizarse: sin distinguirlo, un total bajo por datos faltantes se lee igual que uno bajo real. */
data class RecipeDraftCostItem(
    val productId: Int,
    val quantity: Double,
    val unitCost: Double,
    val subtotal: Double,
    val sinCostear: Boolean,
)

data class RecipeDraftCost(
    val total: Double,
    val items: List<RecipeDraftCostItem>,
    val sinCostear: Int,
)

/** Motor de recetas — espejo de internal/production en el backend. Toda la lógica de costeo y
 * descuento de insumos vive en el backend; este repositorio solo expone su API. */
interface ProductionRepository {
    suspend fun getRecipe(productId: Int): AppResult<RecipeDetail?>
    suspend fun upsertRecipe(productId: Int, notes: String, items: List<RecipeItem>): AppResult<Recipe>
    suspend fun deleteRecipe(productId: Int): AppResult<Unit>
    suspend fun getRecipeCost(productId: Int, branchId: Int? = null): AppResult<Double>
    /** Cuesta un borrador sin guardarlo, para que el editor sume mientras se arma el plato. */
    suspend fun costDraft(items: List<RecipeItem>, branchId: Int? = null): AppResult<RecipeDraftCost>
    suspend fun lowStockInsumos(branchId: Int? = null): AppResult<List<LowStockInsumo>>
    suspend fun lowStockCount(branchId: Int? = null): AppResult<Int>
    suspend fun plateMargin(branchId: Int? = null, from: String? = null, to: String? = null): AppResult<List<PlateMarginRow>>
}
