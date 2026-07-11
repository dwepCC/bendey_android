package com.bendey.restaurant.core.domain.inventory

import com.bendey.restaurant.core.domain.model.AppResult

data class StockMovementItem(
    val id: Int,
    val productId: Int,
    val productCode: String?,
    val productName: String?,
    val branchId: Int,
    val branchName: String?,
    val type: String,
    val quantity: Double,
    val balance: Double?,
    val reference: String?,
    val notes: String?,
    val userName: String?,
    val createdAt: String,
)

data class StockMovementQuery(
    val productQ: String = "",
    val branchId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val movementKind: String? = null,
    val refNotesQ: String = "",
    val page: Int = 1,
    val perPage: Int = 25,
)

data class InventoryAdjustmentInput(
    val productId: Int,
    val branchId: Int,
    val type: String,
    val quantity: Double,
    val notes: String,
)

interface InventoryRepository {
    suspend fun getStock(productId: Int, branchId: Int? = null): AppResult<Double>

    suspend fun listMovements(query: StockMovementQuery): AppResult<Pair<List<StockMovementItem>, Int>>

    suspend fun listAllMovementsForExport(query: StockMovementQuery): AppResult<List<StockMovementItem>>

    suspend fun recordAdjustment(input: InventoryAdjustmentInput): AppResult<Unit>
}
