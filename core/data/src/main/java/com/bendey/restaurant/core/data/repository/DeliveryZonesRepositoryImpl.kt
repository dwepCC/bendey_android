package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryZone
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneInput
import com.bendey.restaurant.core.domain.delivery.DeliveryZonesData
import com.bendey.restaurant.core.domain.delivery.DeliveryZonesRepository
import com.bendey.restaurant.core.domain.delivery.EarningsPayout
import com.bendey.restaurant.core.domain.delivery.PendingEarning
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.DeliveryEarningsApi
import com.bendey.restaurant.core.network.api.DeliveryZoneDto
import com.bendey.restaurant.core.network.api.DeliveryZoneRequestDto
import com.bendey.restaurant.core.network.api.DeliveryZonesApi
import com.bendey.restaurant.core.network.api.EarningsPayoutRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryZonesRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DeliveryZonesRepository {

    private val api: DeliveryZonesApi
        get() = tenantRetrofitProvider.create()

    override suspend fun zones(): AppResult<DeliveryZonesData> = apiCall {
        val r = api.zones()
        DeliveryZonesData(r.data.map { it.toDomain() }.sortedBy { it.name.lowercase() }, r.restrictToZones ?: false)
    }

    override suspend fun saveZone(input: DeliveryZoneInput): AppResult<DeliveryZone> = apiCall {
        val dto = DeliveryZoneRequestDto(
            ubigeoCode = input.ubigeoCode,
            name = input.name.trim(),
            fee = input.fee,
            minOrder = input.minOrder,
            etaMinutes = input.etaMinutes,
            active = input.active,
        )
        api.saveZone(dto).data?.toDomain() ?: error("Zona no guardada")
    }

    override suspend fun deleteZone(id: Int): AppResult<Unit> = apiCall {
        api.deleteZone(id)
        Unit
    }
}

internal fun DeliveryZoneDto.toDomain() = DeliveryZone(
    id = id,
    ubigeoCode = ubigeoCode,
    name = name,
    fee = (fee ?: 0.0).coerceAtLeast(0.0),
    minOrder = (minOrder ?: 0.0).coerceAtLeast(0.0),
    etaMinutes = (etaMinutes ?: 0).coerceAtLeast(0),
    active = active ?: true,
)

@Singleton
class DeliveryEarningsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DeliveryEarningsRepository {

    private val api: DeliveryEarningsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun pending(): AppResult<List<PendingEarning>> = apiCall {
        api.pending().data.map {
            PendingEarning(it.driverId, it.name.orEmpty().ifBlank { "Sin nombre" }, it.deliveries ?: 0, it.total ?: 0.0)
        }
    }

    override suspend fun payout(driverId: Int): AppResult<EarningsPayout> = apiCall {
        val d = api.payout(EarningsPayoutRequestDto(driverId)).data
        val p = d?.payout
        EarningsPayout(
            name = d?.name.orEmpty(),
            amount = p?.amount ?: 0.0,
            deliveries = p?.deliveriesCount ?: 0,
            booked = p?.booked ?: true,
        )
    }
}
