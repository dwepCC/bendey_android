package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.SalesListResponseDto
import com.bendey.restaurant.core.network.dto.SaleDetailResponseDto
import com.bendey.restaurant.core.network.dto.CancelSaleRequestDto
import com.bendey.restaurant.core.network.dto.CancelSaleResponseDto
import com.bendey.restaurant.core.network.dto.IssueElectronicRequestDto
import com.bendey.restaurant.core.network.dto.IssueElectronicResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SalesApi {
    @GET("/api/sales")
    suspend fun listSales(
        @Query("q") query: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 25,
        @Query("sunat_code") sunatCode: String? = null,
        @Query("doc_type") docType: String? = null,
        @Query("billing_status") billingStatus: String? = null,
        @Query("payment_method") paymentMethod: String? = null,
    ): SalesListResponseDto

    @GET("/api/sales/{saleId}")
    suspend fun getSale(
        @Path("saleId") saleId: Int,
    ): SaleDetailResponseDto

    @POST("/api/sales/{saleId}/cancel")
    suspend fun cancelNota(
        @Path("saleId") saleId: Int,
        @Body body: CancelSaleRequestDto,
    ): CancelSaleResponseDto

    @POST("/api/sales/{saleId}/issue-electronic")
    suspend fun issueElectronicFromNota(
        @Path("saleId") saleId: Int,
        @Body body: IssueElectronicRequestDto,
    ): IssueElectronicResponseDto
}
