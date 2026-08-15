package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.SalesByProductResponseDto
import com.bendey.restaurant.core.network.dto.SalesListResponseDto
import com.bendey.restaurant.core.network.dto.SaleDetailResponseDto
import com.bendey.restaurant.core.network.dto.CancelSaleRequestDto
import com.bendey.restaurant.core.network.dto.CancelSaleResponseDto
import com.bendey.restaurant.core.network.dto.IssueElectronicRequestDto
import com.bendey.restaurant.core.network.dto.IssueElectronicResponseDto
import com.bendey.restaurant.core.network.dto.RefundSaleRequestDto
import com.bendey.restaurant.core.network.dto.RefundSaleResponseDto
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
        @Query("export_all") exportAll: Int? = null,
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

    /**
     * Registra que el dinero de una venta volvio al cliente. Anular no es devolver: esta es la unica
     * operacion que saca plata de la caja, y por eso pide confirmacion explicita.
     */
    @POST("/api/sales/{saleId}/refund")
    suspend fun refundSale(
        @Path("saleId") saleId: Int,
        @Body body: RefundSaleRequestDto,
    ): RefundSaleResponseDto

    @POST("/api/sales/{saleId}/issue-electronic")
    suspend fun issueElectronicFromNota(
        @Path("saleId") saleId: Int,
        @Body body: IssueElectronicRequestDto,
    ): IssueElectronicResponseDto

    @GET("/api/sales/by-product")
    suspend fun listSalesByProduct(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("branch_id") branchId: Int? = null,
        @Query("category_id") categoryId: Int? = null,
    ): SalesByProductResponseDto
}
