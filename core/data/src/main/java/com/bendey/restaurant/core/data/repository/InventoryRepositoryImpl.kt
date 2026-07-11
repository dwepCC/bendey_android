package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.inventory.InventoryAdjustmentInput
import com.bendey.restaurant.core.domain.inventory.InventoryRepository
import com.bendey.restaurant.core.domain.inventory.StockMovementItem
import com.bendey.restaurant.core.domain.inventory.StockMovementQuery
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.InventoryApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.StockMovementDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : InventoryRepository {

    private val api: InventoryApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getStock(productId: Int, branchId: Int?): AppResult<Double> = apiCall {
        val rows = api.getStock(productId, branchId).data
        if (branchId != null) {
            rows.firstOrNull { it.branchId == branchId }?.quantity
                ?: rows.firstOrNull()?.quantity
                ?: 0.0
        } else {
            rows.sumOf { it.quantity }
        }
    }

    override suspend fun listMovements(query: StockMovementQuery): AppResult<Pair<List<StockMovementItem>, Int>> =
        apiCall {
            val response = api.listMovements(
                productQ = query.productQ.trim().takeIf { it.isNotEmpty() },
                branchId = query.branchId,
                dateFrom = query.dateFrom,
                dateTo = query.dateTo,
                movementKind = query.movementKind?.takeIf { it.isNotEmpty() },
                q = query.refNotesQ.trim().takeIf { it.isNotEmpty() },
                page = query.page,
                perPage = query.perPage,
            )
            response.data.map { it.toDomain() } to (response.total ?: response.data.size)
        }

    override suspend fun listAllMovementsForExport(query: StockMovementQuery): AppResult<List<StockMovementItem>> =
        apiCall {
            api.listMovements(
                productQ = query.productQ.trim().takeIf { it.isNotEmpty() },
                branchId = query.branchId,
                dateFrom = query.dateFrom,
                dateTo = query.dateTo,
                movementKind = query.movementKind?.takeIf { it.isNotEmpty() },
                q = query.refNotesQ.trim().takeIf { it.isNotEmpty() },
                page = 1,
                perPage = 10_000,
            ).data.map { it.toDomain() }
        }

    override suspend fun recordAdjustment(input: InventoryAdjustmentInput): AppResult<Unit> = apiCall {
        api.recordAdjustment(
            com.bendey.restaurant.core.network.dto.InventoryAdjustmentRequestDto(
                productId = input.productId,
                branchId = input.branchId,
                type = input.type,
                quantity = input.quantity,
                notes = input.notes.trim(),
            ),
        )
    }
}

private fun StockMovementDto.toDomain() = StockMovementItem(
    id = id,
    productId = productId,
    productCode = productCode,
    productName = productName,
    branchId = branchId,
    branchName = branchName,
    type = type,
    quantity = quantity,
    balance = balance,
    reference = reference,
    notes = notes,
    userName = userName,
    createdAt = createdAt,
)

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}
