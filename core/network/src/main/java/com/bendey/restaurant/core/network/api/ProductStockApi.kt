package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/** Stock de un producto por presentación (GET /api/products/:id/presentation-stock). */
@Serializable
data class PresentationStockLevelDto(
    @SerialName("presentation_id") val presentationId: Int? = null,
    val name: String? = null,
    val quantity: Double? = null,
    @SerialName("min_stock") val minStock: Double? = null,
    val active: Boolean? = null,
)

@Serializable
data class PresentationStockDataDto(
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("stock_by_presentation") val stockByPresentation: Boolean? = null,
    @SerialName("min_stock") val minStock: Double? = null,
    val total: Double? = null,
    val presentations: List<PresentationStockLevelDto>? = null,
)

@Serializable
data class PresentationStockResponseDto(val data: PresentationStockDataDto? = null)

interface ProductStockApi {
    /** active_only=1: solo presentaciones activas, y ninguna si el producto no reparte por presentación. */
    @GET("/api/products/{id}/presentation-stock?active_only=1")
    suspend fun stock(@Path("id") productId: Int): PresentationStockResponseDto
}
