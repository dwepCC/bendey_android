package com.bendey.restaurant.core.domain.restaurant

import com.bendey.restaurant.core.domain.model.AppResult

interface PosRepository {
    suspend fun loadCategories(): AppResult<List<ProductCategory>>
    suspend fun refreshCategories(): AppResult<List<ProductCategory>>
    suspend fun loadProducts(
        query: String,
        categoryId: Int?,
        page: Int,
        branchId: Int?,
        catalogOnly: Boolean? = null,
        preparationAreaId: Int? = null,
    ): AppResult<Pair<List<PosProduct>, Int>>

    suspend fun openPosSession(input: PosSessionInput): AppResult<OpenSessionResult>
    suspend fun updatePosSession(sessionId: Int, input: PosSessionInput): AppResult<Unit>
    suspend fun addOrder(sessionId: Int, items: List<OrderItemInput>): AppResult<AddOrderResult>
    suspend fun listOpenOrders(): AppResult<List<OpenOrderSummary>>
    suspend fun cancelSession(sessionId: Int, reason: String, pin: String): AppResult<Unit>
    suspend fun cancelComanda(comandaId: Int, reason: String, pin: String): AppResult<Unit>
    suspend fun updateComandaNotes(comandaId: Int, notes: String): AppResult<Unit>
    suspend fun markTableOrderPrinted(tableOrderId: Int): AppResult<Unit>
    suspend fun getPrecuenta(sessionId: Int): AppResult<PrecuentaData>
    suspend fun listDeliveryDrivers(): AppResult<List<DeliveryDriverBrief>>
}

interface MesasRepository {
    suspend fun loadFloors(): AppResult<List<Floor>>
    suspend fun loadTables(floorId: Int?): AppResult<List<RestaurantTable>>
    suspend fun loadStaff(): AppResult<List<StaffOption>>
    suspend fun openTableSession(
        tableId: Int,
        guests: Int,
        notes: String?,
        staffId: Int?,
    ): AppResult<OpenSessionResult>
    suspend fun getSession(sessionId: Int): AppResult<TableSessionDetail>
    suspend fun getPrecuenta(sessionId: Int): AppResult<PrecuentaData>
    suspend fun closeSession(sessionId: Int): AppResult<Unit>
    suspend fun moveSessionTable(sessionId: Int, targetTableId: Int): AppResult<Unit>
    suspend fun createFloor(name: String, sortOrder: Int): AppResult<Unit>
    suspend fun updateFloor(id: Int, name: String, sortOrder: Int): AppResult<Unit>
    suspend fun deleteFloor(id: Int): AppResult<Unit>
    suspend fun createTable(floorId: Int, name: String, capacity: Int): AppResult<Unit>
    suspend fun updateTable(id: Int, floorId: Int, name: String, capacity: Int): AppResult<Unit>
    suspend fun deleteTable(id: Int): AppResult<Unit>
    suspend fun getOperationalStatus(): AppResult<BranchOperationalStatus>
}

data class BranchOperationalStatus(
    val openTablesCount: Int = 0,
    val openSessionsCount: Int = 0,
    val pendingBillingCount: Int = 0,
    val activeComandasCount: Int = 0,
    val hasActiveOperations: Boolean = false,
)

interface KitchenRepository {
    suspend fun loadKitchen(): AppResult<List<KitchenItem>>
    suspend fun updateComandaStatus(comandaId: Int, status: ComandaStatus): AppResult<Unit>
    suspend fun cancelComanda(comandaId: Int, reason: String, pin: String): AppResult<Unit>
}
