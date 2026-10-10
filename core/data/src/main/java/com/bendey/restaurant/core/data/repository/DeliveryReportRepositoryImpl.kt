package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryReport
import com.bendey.restaurant.core.domain.delivery.DeliveryReportCancellation
import com.bendey.restaurant.core.domain.delivery.DeliveryReportChannel
import com.bendey.restaurant.core.domain.delivery.DeliveryReportDriver
import com.bendey.restaurant.core.domain.delivery.DeliveryReportPayment
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRefunds
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryReportSummary
import com.bendey.restaurant.core.domain.delivery.DeliveryReportTime
import com.bendey.restaurant.core.domain.delivery.DeliveryReportTimes
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.DeliveryReportApi
import com.bendey.restaurant.core.network.api.DeliveryReportDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryReportRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DeliveryReportRepository {
    private val api: DeliveryReportApi
        get() = tenantRetrofitProvider.create()

    override suspend fun report(from: String, to: String): AppResult<DeliveryReport> = apiCall {
        api.report(from, to).toDomain()
    }
}

/** TOLERANTE: ausentes = 0 / vacío; el envoltorio `data` se desenvuelve. Nunca lanza. */
fun DeliveryReportDto.toDomain(): DeliveryReport {
    val r = data ?: this
    val s = r.summary
    val t = r.times
    return DeliveryReport(
        summary = DeliveryReportSummary(
            orders = s?.orders ?: 0,
            delivered = s?.delivered ?: 0,
            inProgress = s?.inProgress ?: 0,
            cancelled = s?.cancelled ?: 0,
            gross = s?.gross ?: 0.0,
            avgTicket = s?.avgTicket ?: 0.0,
            lostAmount = s?.lostAmount ?: 0.0,
            cancelRate = s?.cancelRate ?: 0.0,
        ),
        channels = r.channels.orEmpty().map {
            DeliveryReportChannel(it.source.orEmpty(), it.type.orEmpty(), it.orders ?: 0, it.cancelled ?: 0, it.amount ?: 0.0)
        },
        times = DeliveryReportTimes(
            accept = DeliveryReportTime(t?.acceptMinutes ?: 0.0, t?.acceptSamples ?: 0),
            prep = DeliveryReportTime(t?.prepMinutes ?: 0.0, t?.prepSamples ?: 0),
            delivery = DeliveryReportTime(t?.deliveryMinutes ?: 0.0, t?.deliverySamples ?: 0),
            total = DeliveryReportTime(t?.totalMinutes ?: 0.0, t?.totalSamples ?: 0),
        ),
        cancellations = r.cancellations.orEmpty().map {
            DeliveryReportCancellation(it.reason.orEmpty(), it.label.orEmpty().ifBlank { it.reason.orEmpty() }, it.orders ?: 0, it.amount ?: 0.0, it.sample.orEmpty())
        },
        payments = r.payments.orEmpty().map {
            DeliveryReportPayment(it.mode.orEmpty(), it.method.orEmpty(), it.label.orEmpty().ifBlank { it.method.orEmpty() }, it.orders ?: 0, it.amount ?: 0.0)
        },
        drivers = r.drivers.orEmpty().map {
            DeliveryReportDriver(
                driverId = it.driverId ?: 0,
                name = it.name.orEmpty().ifBlank { "Sin nombre" },
                assigned = it.assigned ?: 0,
                delivered = it.delivered ?: 0,
                failed = it.failed ?: 0,
                rejected = it.rejected ?: 0,
                avgDeliveryMinutes = it.avgDeliveryMinutes ?: 0.0,
                earnings = it.earnings ?: 0.0,
                cashCollected = it.cashCollected ?: 0.0,
                cashPending = it.cashPending ?: 0.0,
                earningsPaid = it.earningsPaid ?: 0.0,
                earningsPending = it.earningsPending ?: 0.0,
            )
        },
        refunds = DeliveryReportRefunds(r.refunds?.pending ?: 0, r.refunds?.pendingAmount ?: 0.0),
        truncated = r.truncated ?: false,
    )
}
