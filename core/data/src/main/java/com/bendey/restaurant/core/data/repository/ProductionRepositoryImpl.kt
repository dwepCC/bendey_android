package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.production.LowStockInsumo
import com.bendey.restaurant.core.domain.production.PlateMarginRow
import com.bendey.restaurant.core.domain.production.Recipe
import com.bendey.restaurant.core.domain.production.RecipeDetail
import com.bendey.restaurant.core.domain.production.RecipeItem
import com.bendey.restaurant.core.domain.production.ProductionRepository
import com.bendey.restaurant.core.network.api.ProductionApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.LowStockInsumoDto
import com.bendey.restaurant.core.network.dto.PlateMarginRowDto
import com.bendey.restaurant.core.network.dto.RecipeDetailDto
import com.bendey.restaurant.core.network.dto.RecipeDto
import com.bendey.restaurant.core.network.dto.RecipeItemDto
import com.bendey.restaurant.core.network.dto.UpsertRecipeRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductionRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ProductionRepository {

    private val api: ProductionApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getRecipe(productId: Int): AppResult<RecipeDetail?> = apiCall {
        api.getRecipe(productId).data?.toDomain()
    }

    override suspend fun upsertRecipe(productId: Int, notes: String, items: List<RecipeItem>): AppResult<Recipe> =
        apiCall {
            api.upsertRecipe(
                productId,
                UpsertRecipeRequestDto(notes = notes, items = items.mapIndexed { i, item ->
                    RecipeItemDto(productId = item.productId, quantity = item.quantity, sortOrder = item.sortOrder.takeIf { it != 0 } ?: i)
                }),
            ).data.toDomain()
        }

    override suspend fun deleteRecipe(productId: Int): AppResult<Unit> = apiCall {
        api.deleteRecipe(productId)
        Unit
    }

    override suspend fun getRecipeCost(productId: Int, branchId: Int?): AppResult<Double> = apiCall {
        api.getRecipeCost(productId, branchId).data.cost
    }

    override suspend fun lowStockInsumos(branchId: Int?): AppResult<List<LowStockInsumo>> = apiCall {
        api.lowStockInsumos(branchId).data.map { it.toDomain() }
    }

    override suspend fun plateMargin(branchId: Int?, from: String?, to: String?): AppResult<List<PlateMarginRow>> =
        apiCall {
            api.plateMargin(branchId, from, to).data.map { it.toDomain() }
        }

    private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (e: Exception) {
        val mapped = NetworkErrorMapper.map(e)
        AppResult.Error(mapped.message ?: "Error de conexión", mapped)
    }
}

private fun RecipeDto.toDomain() = Recipe(
    id = id,
    productId = productId,
    notes = notes.orEmpty(),
    active = active,
)

private fun RecipeDetailDto.toDomain() = RecipeDetail(
    recipe = recipe.toDomain(),
    items = items.map {
        RecipeItem(id = it.id, productId = it.productId, quantity = it.quantity, sortOrder = it.sortOrder ?: 0)
    },
)

private fun LowStockInsumoDto.toDomain() = LowStockInsumo(
    productId = productId,
    code = code,
    name = name,
    unit = unit,
    quantity = quantity,
    minStock = minStock,
)

private fun PlateMarginRowDto.toDomain() = PlateMarginRow(
    productId = productId,
    name = name,
    qtySold = qtySold,
    revenue = revenue,
    cost = cost,
    margin = margin,
)
