package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.LowStockInsumosResponseDto
import com.bendey.restaurant.core.network.dto.PlateMarginResponseDto
import com.bendey.restaurant.core.network.dto.RecipeCostResponseDto
import com.bendey.restaurant.core.network.dto.RecipeDetailResponseDto
import com.bendey.restaurant.core.network.dto.RecipeResponseDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.UpsertRecipeRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Motor de recetas — espejo de internal/production en el backend. */
interface ProductionApi {
    @GET("/api/production/recipes/{productId}")
    suspend fun getRecipe(@Path("productId") productId: Int): RecipeDetailResponseDto

    @PUT("/api/production/recipes/{productId}")
    suspend fun upsertRecipe(
        @Path("productId") productId: Int,
        @Body body: UpsertRecipeRequestDto,
    ): RecipeResponseDto

    @DELETE("/api/production/recipes/{productId}")
    suspend fun deleteRecipe(@Path("productId") productId: Int): SuccessResponseDto

    @GET("/api/production/recipes/{productId}/cost")
    suspend fun getRecipeCost(
        @Path("productId") productId: Int,
        @Query("branch_id") branchId: Int? = null,
    ): RecipeCostResponseDto

    @GET("/api/production/reports/low-stock")
    suspend fun lowStockInsumos(@Query("branch_id") branchId: Int? = null): LowStockInsumosResponseDto

    @GET("/api/production/reports/plate-margin")
    suspend fun plateMargin(
        @Query("branch_id") branchId: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): PlateMarginResponseDto
}
