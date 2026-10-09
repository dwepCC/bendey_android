package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.marketplace.MarketplaceOps
import com.bendey.restaurant.core.domain.marketplace.MarketplaceRepository
import com.bendey.restaurant.core.domain.marketplace.MarketplaceSettings
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.MarketplaceApi
import com.bendey.restaurant.core.network.api.MarketplaceOpsDto
import com.bendey.restaurant.core.network.api.MarketplacePauseRequestDto
import com.bendey.restaurant.core.network.api.MarketplaceSettingsUpdateDto
import com.bendey.restaurant.core.network.api.MarketplaceThresholdsRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : MarketplaceRepository {

    private val api: MarketplaceApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getSettings(): AppResult<MarketplaceSettings> = apiCall {
        val d = api.getSettings().data
        MarketplaceSettings(enabled = d.marketplaceEnabled, menuDigitalEnabled = d.menuDigitalEnabled)
    }

    override suspend fun setEnabled(enabled: Boolean): AppResult<Unit> = apiCall {
        api.updateSettings(MarketplaceSettingsUpdateDto(enabled))
    }

    override suspend fun getOps(): AppResult<MarketplaceOps> = apiCall { api.getOps().data.toDomain() }

    override suspend fun setPause(paused: Boolean, minutes: Int, reason: String): AppResult<MarketplaceOps> = apiCall {
        api.setPause(MarketplacePauseRequestDto(paused, minutes, reason)).data.toDomain()
    }

    override suspend fun setThresholds(alertMinutes: Int, autoCancelMinutes: Int): AppResult<MarketplaceOps> = apiCall {
        api.setThresholds(MarketplaceThresholdsRequestDto(alertMinutes, autoCancelMinutes)).data.toDomain()
    }

    private fun MarketplaceOpsDto.toDomain() = MarketplaceOps(
        paused = paused,
        pausedUntil = pausedUntil,
        pauseReason = pauseReason.orEmpty(),
        unattendedAlertMinutes = unattendedAlertMinutes,
        autoCancelMinutes = autoCancelMinutes,
    )
}
