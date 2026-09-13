package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.DocumentSeriesDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.BranchUpsertRequestDto
import com.bendey.restaurant.core.network.dto.SaleDetailConfigResponseDto
import com.bendey.restaurant.core.network.dto.SaleDetailConfigUpdateRequestDto
import com.bendey.restaurant.core.network.dto.ServiceChargeConfigResponseDto
import com.bendey.restaurant.core.network.dto.ServiceChargeUpdateRequestDto
import com.bendey.restaurant.core.network.dto.SeriesCreateRequestDto
import com.bendey.restaurant.core.network.dto.SeriesUpdateRequestDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CompanyApi {
    @GET("/api/company/series")
    suspend fun listSeries(
        @Query("branch_id") branchId: Int? = null,
        @Query("category") category: String? = "venta",
    ): ListResponseDto<DocumentSeriesDto>

    @POST("/api/company/series")
    suspend fun createSeries(@Body body: SeriesCreateRequestDto): SuccessResponseDto

    @PUT("/api/company/series/{id}")
    suspend fun updateSeries(
        @Path("id") id: Int,
        @Body body: SeriesUpdateRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/company/series/{id}")
    suspend fun deleteSeries(@Path("id") id: Int): SuccessResponseDto

    @POST("/api/company/branches")
    suspend fun createBranch(@Body body: BranchUpsertRequestDto): SuccessResponseDto

    @PUT("/api/company/branches/{id}")
    suspend fun updateBranch(
        @Path("id") id: Int,
        @Body body: BranchUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/company/branches/{id}")
    suspend fun deleteBranch(@Path("id") id: Int): SuccessResponseDto

    @GET("/api/company/branches/{id}/sale-detail-mode")
    suspend fun getSaleDetailConfig(@Path("id") branchId: Int): SaleDetailConfigResponseDto

    @PUT("/api/company/branches/{id}/sale-detail-mode")
    suspend fun updateSaleDetailConfig(
        @Path("id") branchId: Int,
        @Body body: SaleDetailConfigUpdateRequestDto,
    ): SaleDetailConfigResponseDto

    @GET("/api/company/branches/{id}/service-charge")
    suspend fun getServiceCharge(@Path("id") branchId: Int): ServiceChargeConfigResponseDto

    @PUT("/api/company/branches/{id}/service-charge")
    suspend fun updateServiceCharge(
        @Path("id") branchId: Int,
        @Body body: ServiceChargeUpdateRequestDto,
    ): ServiceChargeConfigResponseDto
}
