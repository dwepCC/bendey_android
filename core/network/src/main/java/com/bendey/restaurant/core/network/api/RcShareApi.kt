package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Reparto del Recargo al Consumo (sección 24 del contrato). */
@Serializable
data class RcLineDto(
    @SerialName("staff_id") val staffId: Int? = null,
    val name: String? = null,
    val amount: Double? = null,
)

@Serializable
data class RcDistributionDto(
    val id: Int? = null,
    @SerialName("period_from") val periodFrom: String? = null,
    @SerialName("period_to") val periodTo: String? = null,
    val amount: Double? = null,
    val notes: String? = null,
    @SerialName("paid_from_cash") val paidFromCash: Boolean? = null,
    val booked: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val lines: List<RcLineDto>? = null,
)

@Serializable
data class RcSummaryDto(
    val from: String? = null,
    val to: String? = null,
    val collected: Double? = null,
    val distributed: Double? = null,
    val pending: Double? = null,
    @SerialName("sales_count") val salesCount: Int? = null,
    @SerialName("avg_rate") val avgRate: Double? = null,
    val history: List<RcDistributionDto>? = null,
    val data: RcSummaryDto? = null,
)

@Serializable
data class RcDistributeLineDto(@SerialName("staff_id") val staffId: Int, val amount: Double)

@Serializable
data class RcDistributeRequestDto(
    val from: String,
    val to: String,
    val lines: List<RcDistributeLineDto>,
    val notes: String? = null,
    @SerialName("pay_from_cash") val payFromCash: Boolean = false,
)

@Serializable
data class RcDistributeResponseDto(val success: Boolean? = null)

interface RcShareApi {
    @GET("/api/restaurant/rc-share/summary")
    suspend fun summary(@Query("from") from: String, @Query("to") to: String): RcSummaryDto

    @POST("/api/restaurant/rc-share/distribute")
    suspend fun distribute(@Body body: RcDistributeRequestDto): RcDistributeResponseDto
}
