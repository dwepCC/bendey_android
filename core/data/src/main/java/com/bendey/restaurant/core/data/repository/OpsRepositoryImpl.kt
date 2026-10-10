package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.cache.OperationalDataCache
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.ops.BusinessHours
import com.bendey.restaurant.core.domain.ops.BusinessHoursStatus
import com.bendey.restaurant.core.domain.ops.BusinessInterval
import com.bendey.restaurant.core.domain.ops.OpsControls
import com.bendey.restaurant.core.domain.ops.OpsRepository
import com.bendey.restaurant.core.domain.ops.ReopenResult
import com.bendey.restaurant.core.network.api.BusinessHourIntervalDto
import com.bendey.restaurant.core.network.api.BusinessHoursDto
import com.bendey.restaurant.core.network.api.BusinessHoursUpdateDto
import com.bendey.restaurant.core.network.api.OpsApi
import com.bendey.restaurant.core.network.api.OpsControlsDto
import com.bendey.restaurant.core.network.api.OpsControlsUpdateDto
import com.bendey.restaurant.core.network.api.ReopenSessionRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.ErrorFlow
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Control de descuentos, horario de atención y reabrir pedido tras anular una venta. */
@Singleton
class OpsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val operationalDataCache: OperationalDataCache,
) : OpsRepository {

    private val api: OpsApi
        get() = tenantRetrofitProvider.create()

    override fun peekOpsControls(): OpsControls? = operationalDataCache.opsControls.value

    override suspend fun getOpsControls(forceRefresh: Boolean): AppResult<OpsControls> {
        if (!forceRefresh) operationalDataCache.opsControls.value?.let { return AppResult.Success(it) }
        return apiCall {
            api.getOpsControls().toDomain().also { operationalDataCache.opsControls.value = it }
        }
    }

    override suspend fun setDiscountControl(enabled: Boolean): AppResult<OpsControls> = apiCall {
        api.updateOpsControls(OpsControlsUpdateDto(enabled)).toDomain().also { operationalDataCache.opsControls.value = it }
    }

    override suspend fun getBusinessHours(): AppResult<BusinessHours> = apiCall {
        api.getBusinessHours().toDomain()
    }

    override suspend fun saveBusinessHours(intervals: List<BusinessInterval>): AppResult<BusinessHours> = apiCall {
        api.putBusinessHours(
            BusinessHoursUpdateDto(intervals.map { BusinessHourIntervalDto(it.weekday, it.openAt, it.closeAt) }),
        ).toDomain()
    }

    override suspend fun reopenSession(saleId: Int, pin: String, reason: String): AppResult<ReopenResult> =
        apiCall(ErrorFlow.VOID_REFUND) {
            val data = api.reopenSession(saleId, ReopenSessionRequestDto(pin = pin.trim(), reason = reason.trim())).data
                ?: error("El servidor no confirmó la reapertura")
            ReopenResult(data.sessionId, data.releasedComandas, data.reopened)
        }
}

internal fun OpsControlsDto.toDomain() = OpsControls(discountControl = discountControl, canDiscount = canDiscount)

internal fun BusinessHoursDto.toDomain() = BusinessHours(
    intervals = intervals.map { BusinessInterval(it.weekday, it.openAt, it.closeAt) },
    status = BusinessHoursStatus(status.configured, status.open, status.message),
)
