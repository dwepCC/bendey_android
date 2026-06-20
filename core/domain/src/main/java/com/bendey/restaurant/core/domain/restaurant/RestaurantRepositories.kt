package com.bendey.restaurant.core.domain.restaurant

import com.bendey.restaurant.core.domain.model.AppResult

interface PosRepository {
    suspend fun loadCategories(): AppResult<List<ProductCategory>>
    suspend fun loadProducts(
        query: String,
        categoryId: Int?,
        page: Int,
        branchId: Int?,
    ): AppResult<Pair<List<PosProduct>, Int>>

    suspend fun openCounterSession(orderType: String): AppResult<OpenSessionResult>
    suspend fun addOrder(sessionId: Int, items: List<OrderItemInput>): AppResult<AddOrderResult>
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
    suspend fun createFloor(name: String, sortOrder: Int): AppResult<Unit>
    suspend fun updateFloor(id: Int, name: String, sortOrder: Int): AppResult<Unit>
    suspend fun deleteFloor(id: Int): AppResult<Unit>
    suspend fun createTable(floorId: Int, name: String, capacity: Int): AppResult<Unit>
    suspend fun updateTable(id: Int, floorId: Int, name: String, capacity: Int): AppResult<Unit>
    suspend fun deleteTable(id: Int): AppResult<Unit>
}

interface KitchenRepository {
    suspend fun loadKitchen(): AppResult<List<KitchenItem>>
    suspend fun updateComandaStatus(comandaId: Int, status: ComandaStatus): AppResult<Unit>
}
