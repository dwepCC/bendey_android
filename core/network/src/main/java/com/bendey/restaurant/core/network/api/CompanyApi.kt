package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.DocumentSeriesDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CompanyApi {
    @GET("/api/company/series")
    suspend fun listSeries(
        @Query("branch_id") branchId: Int? = null,
        @Query("category") category: String? = "venta",
    ): ListResponseDto<DocumentSeriesDto>
}
