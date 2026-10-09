package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.RefundPending
import com.bendey.restaurant.core.domain.delivery.SettlementGroup
import com.bendey.restaurant.core.domain.delivery.SettlementOrder
import com.bendey.restaurant.core.domain.delivery.SettlementRecord
import com.bendey.restaurant.core.domain.delivery.SettlementRepository
import com.bendey.restaurant.core.domain.delivery.SettlementsPending
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.ReceiveSettlementRequestDto
import com.bendey.restaurant.core.network.api.RefundResolvedRequestDto
import com.bendey.restaurant.core.network.api.SettlementApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : SettlementRepository {

    private val api: SettlementApi
        get() = tenantRetrofitProvider.create()

    override suspend fun pending(): AppResult<SettlementsPending> = apiCall {
        val d = api.pending().data
        SettlementsPending(
            groups = d.groups.map { g ->
                SettlementGroup(
                    g.collectorKind, g.collectorId, g.collectorName, g.total,
                    g.orders.map { SettlementOrder(it.paymentId, it.orderCode, it.customerName, it.amount) },
                )
            },
            refunds = d.refunds.map { RefundPending(it.paymentId, it.orderCode, it.amount, it.collectorName, it.cancelReason) },
        )
    }

    override suspend fun history(): AppResult<List<SettlementRecord>> = apiCall {
        api.history().data.map {
            SettlementRecord(it.id, it.collectorName, it.receivedByName, it.ordersCount, it.receivedTotal, it.difference, it.createdAt)
        }
    }

    override suspend fun receive(group: SettlementGroup, paymentIds: List<Int>, received: Double, reason: String?): AppResult<String?> = apiCall {
        api.receive(
            ReceiveSettlementRequestDto(group.collectorKind, group.collectorId, paymentIds, received, reason?.takeIf { it.isNotBlank() }),
        ).data?.differenceWarning
    }

    override suspend fun resolveRefund(paymentId: Int, note: String): AppResult<Unit> = apiCall {
        api.resolveRefund(paymentId, RefundResolvedRequestDto(note))
        Unit
    }
}
