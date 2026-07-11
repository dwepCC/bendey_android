package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.InventoryAdjustmentRequestDto
import com.bendey.restaurant.core.network.dto.OkResponseDto
import com.bendey.restaurant.core.network.dto.StockByBranchListResponseDto
import com.bendey.restaurant.core.network.dto.StockMovementListResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApi {
    @GET("/api/inventory/stock/{productId}")
    suspend fun getStock(
        @Path("productId") productId: Int,
        @Query("branch_id") branchId: Int? = null,
    ): StockByBranchListResponseDto

    @GET("/api/inventory/movements")
    suspend fun listMovements(
        @Query("product_id") productId: Int? = null,
        @Query("product_q") productQ: String? = null,
        @Query("branch_id") branchId: Int? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("movement_kind") movementKind: String? = null,
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 25,
    ): StockMovementListResponseDto

    @POST("/api/inventory/adjustment")
    suspend fun recordAdjustment(
        @Body body: InventoryAdjustmentRequestDto,
    ): OkResponseDto
}
