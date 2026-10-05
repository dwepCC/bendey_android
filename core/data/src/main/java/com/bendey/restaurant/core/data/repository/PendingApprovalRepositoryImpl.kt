package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.kitchen.modifierLinesOf
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalItem
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalOrder
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalRepository
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.PendingApprovalOrderDto
import com.bendey.restaurant.core.network.dto.RejectTableOrderRequestDto
import com.bendey.restaurant.core.network.error.ErrorFlow
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** R10.1: cola de pedidos del cliente (QR). Si la carga falla DEVUELVE error (nunca "lista vacia"). */
@Singleton
class PendingApprovalRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : PendingApprovalRepository {

    override suspend fun listPending(): AppResult<List<PendingApprovalOrder>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().listPendingApproval().data.map { it.toDomain() }
    }

    override suspend fun approve(orderId: Int): AppResult<Unit> = apiCall(ErrorFlow.SEND_COMANDA) {
        tenantRetrofitProvider.create<RestaurantApi>().approveTableOrder(orderId)
        Unit
    }

    override suspend fun reject(orderId: Int, reason: String): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .rejectTableOrder(orderId, RejectTableOrderRequestDto(reason = reason.trim()))
        Unit
    }
}

private fun PendingApprovalOrderDto.toDomain() = PendingApprovalOrder(
    orderId = orderId,
    orderNumber = orderNumber,
    sessionId = sessionId,
    tableName = tableName.orEmpty(),
    orderType = orderType.orEmpty(),
    notes = notes?.takeIf { it.isNotBlank() },
    createdAt = createdAt.orEmpty(),
    customerName = customerName?.takeIf { it.isNotBlank() },
    customerPhone = customerPhone?.takeIf { it.isNotBlank() },
    total = total,
    items = items.map {
        PendingApprovalItem(
            id = it.id,
            productName = it.productName,
            quantity = it.quantity,
            notes = it.notes?.takeIf { n -> n.isNotBlank() },
            modifierLines = modifierLinesOf(it.modifiersJson),
            preparationArea = it.preparationArea?.takeIf { a -> a.isNotBlank() },
        )
    },
)
