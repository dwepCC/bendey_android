package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

@Serializable
data class MarketplaceSettingsDto(
    @SerialName("marketplace_enabled") val marketplaceEnabled: Boolean = false,
    @SerialName("menu_digital_enabled") val menuDigitalEnabled: Boolean = false,
)

@Serializable
data class MarketplaceSettingsEnvelopeDto(val data: MarketplaceSettingsDto)

@Serializable
data class MarketplaceSettingsUpdateDto(@SerialName("marketplace_enabled") val marketplaceEnabled: Boolean)

@Serializable
data class MarketplaceOpsDto(
    val paused: Boolean = false,
    @SerialName("paused_until") val pausedUntil: String? = null,
    @SerialName("pause_reason") val pauseReason: String? = null,
    @SerialName("unattended_alert_minutes") val unattendedAlertMinutes: Int = 5,
    @SerialName("auto_cancel_minutes") val autoCancelMinutes: Int = 0,
)

@Serializable
data class MarketplaceOpsEnvelopeDto(val data: MarketplaceOpsDto)

@Serializable
data class MarketplacePauseRequestDto(val paused: Boolean, val minutes: Int = 0, val reason: String = "")

@Serializable
data class MarketplaceThresholdsRequestDto(
    @SerialName("unattended_alert_minutes") val unattendedAlertMinutes: Int,
    @SerialName("auto_cancel_minutes") val autoCancelMinutes: Int,
)

/** Publicacion en el marketplace (R-11) y recepcion de pedidos: pausa y umbrales (R-9). */
interface MarketplaceApi {
    @GET("/api/restaurant/marketplace/settings")
    suspend fun getSettings(): MarketplaceSettingsEnvelopeDto

    @PUT("/api/restaurant/marketplace/settings")
    suspend fun updateSettings(@Body body: MarketplaceSettingsUpdateDto)

    @GET("/api/restaurant/marketplace-ops/")
    suspend fun getOps(): MarketplaceOpsEnvelopeDto

    @PUT("/api/restaurant/marketplace-ops/pause")
    suspend fun setPause(@Body body: MarketplacePauseRequestDto): MarketplaceOpsEnvelopeDto

    @PUT("/api/restaurant/marketplace-ops/settings")
    suspend fun setThresholds(@Body body: MarketplaceThresholdsRequestDto): MarketplaceOpsEnvelopeDto
}
