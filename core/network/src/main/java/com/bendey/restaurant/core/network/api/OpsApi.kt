package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** `GET/PUT /api/restaurant/ops-controls` (respuesta sin envoltorio). */
@Serializable
data class OpsControlsDto(
    @SerialName("discount_control") val discountControl: Boolean = false,
    @SerialName("can_discount") val canDiscount: Boolean = true,
    @SerialName("show_pos_stock") val showPosStock: Boolean = false,
)

@Serializable
data class OpsControlsUpdateDto(
    // Parcial: el campo nulo no se toca en el servidor.
    @SerialName("discount_control") val discountControl: Boolean? = null,
    @SerialName("show_pos_stock") val showPosStock: Boolean? = null,
)

@Serializable
data class BusinessHourIntervalDto(
    val weekday: Int,
    @SerialName("open_at") val openAt: String,
    @SerialName("close_at") val closeAt: String,
)

@Serializable
data class BusinessHoursStatusDto(
    val configured: Boolean = false,
    val open: Boolean = true,
    val message: String = "",
)

/** `GET/PUT /api/restaurant/business-hours`. */
@Serializable
data class BusinessHoursDto(
    val intervals: List<BusinessHourIntervalDto> = emptyList(),
    val status: BusinessHoursStatusDto = BusinessHoursStatusDto(),
)

@Serializable
data class BusinessHoursUpdateDto(val intervals: List<BusinessHourIntervalDto>)

@Serializable
data class ReopenSessionRequestDto(val pin: String, val reason: String)

@Serializable
data class ReopenSessionDataDto(
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("released_comandas") val releasedComandas: Int = 0,
    val reopened: Boolean = false,
)

@Serializable
data class ReopenSessionResponseDto(val data: ReopenSessionDataDto? = null)

interface OpsApi {
    @GET("/api/restaurant/ops-controls")
    suspend fun getOpsControls(): OpsControlsDto

    @PUT("/api/restaurant/ops-controls")
    suspend fun updateOpsControls(@Body body: OpsControlsUpdateDto): OpsControlsDto

    @GET("/api/restaurant/business-hours")
    suspend fun getBusinessHours(): BusinessHoursDto

    @PUT("/api/restaurant/business-hours")
    suspend fun putBusinessHours(@Body body: BusinessHoursUpdateDto): BusinessHoursDto

    @POST("/api/restaurant/sales/{id}/reopen-session")
    suspend fun reopenSession(@Path("id") saleId: Int, @Body body: ReopenSessionRequestDto): ReopenSessionResponseDto
}
