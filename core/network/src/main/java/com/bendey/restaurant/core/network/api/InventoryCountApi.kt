package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Valorización y conteo físico del inventario (sección 25 del contrato). */
@Serializable
data class ValuationRowDto(
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("presentation_id") val presentationId: Int? = null,
    val code: String? = null,
    val name: String? = null,
    val quantity: Double? = null,
    @SerialName("average_cost") val averageCost: Double? = null,
    val value: Double? = null,
    val negative: Boolean? = null,
)

@Serializable
data class ValuationDataDto(
    @SerialName("branch_id") val branchId: Int? = null,
    val rows: List<ValuationRowDto>? = null,
    @SerialName("total_value") val totalValue: Double? = null,
    @SerialName("total_products") val totalProducts: Int? = null,
    @SerialName("negative_count") val negativeCount: Int? = null,
)

@Serializable
data class ValuationResponseDto(val data: ValuationDataDto? = null)

@Serializable
data class CountLineRequestDto(
    @SerialName("product_id") val productId: Int,
    @SerialName("presentation_id") val presentationId: Int,
    val counted: Double,
)

@Serializable
data class PhysicalCountRequestDto(
    @SerialName("branch_id") val branchId: Int,
    val lines: List<CountLineRequestDto>,
    val note: String? = null,
    val apply: Boolean,
)

@Serializable
data class CountResultLineDto(
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("presentation_id") val presentationId: Int? = null,
    val name: String? = null,
    val system: Double? = null,
    val counted: Double? = null,
    val difference: Double? = null,
    @SerialName("unit_cost") val unitCost: Double? = null,
    @SerialName("value_diff") val valueDiff: Double? = null,
    val applied: Boolean? = null,
    val error: String? = null,
)

@Serializable
data class PhysicalCountDataDto(
    val applied: Boolean? = null,
    val lines: List<CountResultLineDto>? = null,
    @SerialName("adjusted_count") val adjustedCount: Int? = null,
    @SerialName("failed_count") val failedCount: Int? = null,
    @SerialName("total_value_diff") val totalValueDiff: Double? = null,
)

@Serializable
data class PhysicalCountResponseDto(val data: PhysicalCountDataDto? = null)

interface InventoryCountApi {
    @GET("/api/inventory/valuation")
    suspend fun valuation(
        @Query("branch_id") branchId: Int? = null,
        @Query("q") q: String? = null,
    ): ValuationResponseDto

    @POST("/api/inventory/physical-count")
    suspend fun physicalCount(@Body body: PhysicalCountRequestDto): PhysicalCountResponseDto
}
