package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Propinas (sección 22 del contrato). Todo opcional: el cliente es tolerante. */
@Serializable
data class PendingTipDto(
    @SerialName("staff_id") val staffId: Int? = null,
    val name: String? = null,
    val count: Int? = null,
    val total: Double? = null,
    val unbooked: Int? = null,
)

@Serializable
data class PendingTipsResponseDto(val data: List<PendingTipDto>? = null)

@Serializable
data class TipPayoutRequestDto(@SerialName("staff_id") val staffId: Int)

@Serializable
data class TipPayoutDto(val amount: Double? = null, @SerialName("tips_count") val tipsCount: Int? = null)

@Serializable
data class TipPayoutDataDto(val payout: TipPayoutDto? = null, val name: String? = null)

@Serializable
data class TipPayoutResponseDto(val data: TipPayoutDataDto? = null)

@Serializable
data class TipStaffRowDto(
    @SerialName("staff_id") val staffId: Int? = null,
    val name: String? = null,
    val count: Int? = null,
    val total: Double? = null,
    val paid: Double? = null,
    val pending: Double? = null,
)

@Serializable
data class TipMethodRowDto(val method: String? = null, val count: Int? = null, val total: Double? = null)

@Serializable
data class TipsReportDto(
    val total: Double? = null,
    val count: Int? = null,
    val paid: Double? = null,
    val pending: Double? = null,
    val staff: List<TipStaffRowDto>? = null,
    val methods: List<TipMethodRowDto>? = null,
    val data: TipsReportDto? = null,
)

@Serializable
data class TipRecentDto(
    val amount: Double? = null,
    @SerialName("method_code") val methodCode: String? = null,
    @SerialName("payout_id") val payoutId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MyTipsDto(
    val pending: Double? = null,
    val paid: Double? = null,
    val recent: List<TipRecentDto>? = null,
    val data: MyTipsDto? = null,
)

interface TipsApi {
    @GET("/api/restaurant/tips/pending")
    suspend fun pending(): PendingTipsResponseDto

    @POST("/api/restaurant/tips/payout")
    suspend fun payout(@Body body: TipPayoutRequestDto): TipPayoutResponseDto

    @GET("/api/restaurant/tips/report")
    suspend fun report(@Query("from") from: String, @Query("to") to: String): TipsReportDto

    @GET("/api/restaurant/tips/mine")
    suspend fun mine(): MyTipsDto
}
