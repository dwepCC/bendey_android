package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.ContactDataResponseDto
import com.bendey.restaurant.core.network.dto.ContactDto
import com.bendey.restaurant.core.network.dto.CreateContactRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.UpdateContactRequestDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Historial de compras de un cliente (sección 23 del contrato). Todo opcional. */
@Serializable
data class PurchaseHistorySummaryDto(
    val orders: Int? = null,
    @SerialName("total_spent") val totalSpent: Double? = null,
    @SerialName("avg_ticket") val avgTicket: Double? = null,
    @SerialName("first_visit") val firstVisit: String? = null,
    @SerialName("last_visit") val lastVisit: String? = null,
)

@Serializable
data class PurchaseTopProductDto(
    @SerialName("product_id") val productId: Int? = null,
    val description: String? = null,
    val quantity: Double? = null,
    val total: Double? = null,
)

@Serializable
data class PurchaseSaleDto(
    val id: Int? = null,
    val number: String? = null,
    @SerialName("doc_type") val docType: String? = null,
    @SerialName("issue_date") val issueDate: String? = null,
    val total: Double? = null,
    val status: String? = null,
    @SerialName("restaurant_session_id") val sessionId: Int? = null,
)

@Serializable
data class PurchaseHistoryDataDto(
    val summary: PurchaseHistorySummaryDto? = null,
    @SerialName("top_products") val topProducts: List<PurchaseTopProductDto>? = null,
    val sales: List<PurchaseSaleDto>? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
)

@Serializable
data class PurchaseHistoryResponseDto(val data: PurchaseHistoryDataDto? = null)

interface ContactsApi {
    @GET("/api/contacts/{id}/purchase-history")
    suspend fun purchaseHistory(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): PurchaseHistoryResponseDto

    @GET("/api/contacts")
    suspend fun listContacts(
        @Query("q") query: String = "",
        @Query("type") type: String = "customer",
        @Query("status") status: String? = null,
    ): ListResponseDto<ContactDto>

    @GET("/api/contacts/{id}")
    suspend fun getContact(@Path("id") id: Int): ContactDataResponseDto

    @POST("/api/contacts")
    suspend fun createContact(@Body body: CreateContactRequestDto): ContactDataResponseDto

    @PUT("/api/contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: Int,
        @Body body: UpdateContactRequestDto,
    ): ContactDataResponseDto

    @DELETE("/api/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): SuccessResponseDto

    @PATCH("/api/contacts/{id}/toggle")
    suspend fun toggleContact(@Path("id") id: Int): SuccessResponseDto
}
