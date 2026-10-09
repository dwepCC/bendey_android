package com.bendey.restaurant.core.domain.marketplace

import com.bendey.restaurant.core.domain.model.AppResult

data class MarketplaceSettings(val enabled: Boolean, val menuDigitalEnabled: Boolean)

/** Recepcion de pedidos del marketplace. `pausedUntil` es ISO-8601; null con `paused` = hasta reanudar. */
data class MarketplaceOps(
    val paused: Boolean,
    val pausedUntil: String?,
    val pauseReason: String,
    val unattendedAlertMinutes: Int,
    val autoCancelMinutes: Int,
)

interface MarketplaceRepository {
    suspend fun getSettings(): AppResult<MarketplaceSettings>
    suspend fun setEnabled(enabled: Boolean): AppResult<Unit>
    suspend fun getOps(): AppResult<MarketplaceOps>
    suspend fun setPause(paused: Boolean, minutes: Int, reason: String): AppResult<MarketplaceOps>
    suspend fun setThresholds(alertMinutes: Int, autoCancelMinutes: Int): AppResult<MarketplaceOps>
}
