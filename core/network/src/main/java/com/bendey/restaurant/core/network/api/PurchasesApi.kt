package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.CreatePurchaseRequestDto
import com.bendey.restaurant.core.network.dto.CreatePurchaseResponseDto
import com.bendey.restaurant.core.network.dto.PurchaseDetailResponseDto
import com.bendey.restaurant.core.network.dto.PurchaseListResponseDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Solo alcanzable con sesión de login completo (usuario/contraseña) — el backend gate
 * ("purchases.view/create/delete") no se emite en tokens de PIN. Ver ComprasViewModel.
 */
interface PurchasesApi {
    @GET("/api/purchases")
    suspend fun listPurchases(
        @Query("q") query: String = "",
        @Query("contact_id") contactId: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
    ): PurchaseListResponseDto

    @GET("/api/purchases/{id}")
    suspend fun getPurchase(@Path("id") id: Int): PurchaseDetailResponseDto

    @POST("/api/purchases")
    suspend fun createPurchase(@Body body: CreatePurchaseRequestDto): CreatePurchaseResponseDto

    @POST("/api/purchases/{id}/void")
    suspend fun voidPurchase(@Path("id") id: Int): SuccessResponseDto
}
