package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.cache.OperationalDataCache
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.AddOrderResult
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.BranchOperationalStatus
import com.bendey.restaurant.core.data.kitchen.expandKitchenItemsForKds
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OpenSessionResult
import com.bendey.restaurant.core.domain.restaurant.OrderItemInput
import com.bendey.restaurant.core.domain.catalog.normalizePreparationAreaName
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import com.bendey.restaurant.core.domain.restaurant.PrecuentaLine
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.domain.restaurant.StaffOption
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.network.api.ProductsApi
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.AddOrderRequestDto
import com.bendey.restaurant.core.network.dto.ComandaDto
import com.bendey.restaurant.core.network.dto.KitchenComandaDto
import com.bendey.restaurant.core.domain.restaurant.DeliveryDriverBrief
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.OpenOrderSummary
import com.bendey.restaurant.core.domain.restaurant.PosSessionInput
import com.bendey.restaurant.core.network.api.DeliveryApi
import com.bendey.restaurant.core.network.dto.CancelComandaRequestDto
import com.bendey.restaurant.core.network.dto.CancelSessionRequestDto
import com.bendey.restaurant.core.network.dto.OpenOrderSummaryDto
import com.bendey.restaurant.core.network.dto.OpenSessionRequestDto
import com.bendey.restaurant.core.network.dto.OperationalStatusDto
import com.bendey.restaurant.core.network.dto.OrderItemInputDto
import com.bendey.restaurant.core.network.dto.ProductDto
import com.bendey.restaurant.core.network.dto.PrecuentaDto
import com.bendey.restaurant.core.network.dto.RestaurantTableDto
import com.bendey.restaurant.core.network.dto.SessionDetailDto
import com.bendey.restaurant.core.network.dto.SessionOrderDto
import com.bendey.restaurant.core.network.dto.FloorUpsertRequestDto
import com.bendey.restaurant.core.network.dto.TableUpsertRequestDto
import com.bendey.restaurant.core.network.dto.UpdateComandaNotesRequestDto
import com.bendey.restaurant.core.network.dto.UpdateComandaStatusRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val operationalDataCache: OperationalDataCache,
) : PosRepository {

    override suspend fun loadCategories(): AppResult<List<ProductCategory>> {
        operationalDataCache.getCategories()?.let { return AppResult.Success(it) }
        return fetchCategories()
    }

    override suspend fun refreshCategories(): AppResult<List<ProductCategory>> = fetchCategories()

    private suspend fun fetchCategories(): AppResult<List<ProductCategory>> = apiCall {
        tenantRetrofitProvider.create<ProductsApi>().listCategories().data.map {
            ProductCategory(id = it.id, name = it.name)
        }.also { operationalDataCache.setCategories(it) }
    }

    override suspend fun loadProducts(
        query: String,
        categoryId: Int?,
        page: Int,
        branchId: Int?,
        catalogOnly: Boolean?,
        preparationAreaId: Int?,
    ): AppResult<Pair<List<PosProduct>, Int>> = apiCall {
        val response = tenantRetrofitProvider.create<ProductsApi>().listProducts(
            query = query,
            page = page,
            categoryId = categoryId,
            branchId = branchId,
            catalogOnly = catalogOnly?.let { if (it) "true" else "false" },
            preparationAreaId = preparationAreaId,
        )
        val products = response.data.map { it.toDomain() }
        products to (response.total ?: products.size)
    }

    override suspend fun openPosSession(input: PosSessionInput): AppResult<OpenSessionResult> = apiCall {
        val api = tenantRetrofitProvider.create<RestaurantApi>()
        val response = api.openSession(input.toDto())
        val session = response.data ?: error("Sesión no creada")
        OpenSessionResult(sessionId = session.id, orderCode = session.orderCode)
    }

    override suspend fun updatePosSession(sessionId: Int, input: PosSessionInput): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().updateSession(sessionId, input.toUpdateDto())
    }

    override suspend fun listOpenOrders(): AppResult<List<OpenOrderSummary>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .listOpenOrders()
            .data
            .filter { it.orderType == "takeaway" || it.orderType == "delivery" }
            .map { it.toDomain() }
    }

    override suspend fun cancelSession(sessionId: Int, reason: String, pin: String): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .cancelSession(sessionId, CancelSessionRequestDto(reason = reason.trim(), pin = pin.trim()))
    }

    override suspend fun cancelComanda(comandaId: Int, reason: String, pin: String): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .cancelComanda(comandaId, CancelComandaRequestDto(reason = reason.trim(), pin = pin.trim()))
    }

    override suspend fun updateComandaNotes(comandaId: Int, notes: String): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .updateComandaNotes(comandaId, UpdateComandaNotesRequestDto(notes = notes.trim()))
    }

    override suspend fun markTableOrderPrinted(tableOrderId: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().markTableOrderPrinted(tableOrderId)
    }

    override suspend fun getPrecuenta(sessionId: Int): AppResult<PrecuentaData> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .getPrecuenta(sessionId)
            .data
            ?.toDomain()
            ?: error("Precuenta no disponible")
    }

    override suspend fun listDeliveryDrivers(): AppResult<List<DeliveryDriverBrief>> = apiCall {
        tenantRetrofitProvider.create<DeliveryApi>()
            .listDeliveryDrivers(activeOnly = "true")
            .data
            .filter { it.active }
            .map { DeliveryDriverBrief(id = it.id, name = it.name) }
    }

    override suspend fun addOrder(
        sessionId: Int,
        items: List<OrderItemInput>,
    ): AppResult<AddOrderResult> = apiCall {
        val api = tenantRetrofitProvider.create<RestaurantApi>()
        val response = api.addOrder(
            sessionId = sessionId,
            body = AddOrderRequestDto(items = items.map { it.toDto() }),
        )
        val order = response.data ?: error("Pedido no registrado")
        AddOrderResult(
            orderId = order.id,
            orderNumber = order.orderNumber,
            comandas = order.comandas.map { it.toDomain() },
        )
    }
}

@Singleton
class MesasRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : MesasRepository {

    override suspend fun loadFloors(): AppResult<List<Floor>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().listFloors().data.map {
            Floor(id = it.id, name = it.name, sortOrder = it.sortOrder)
        }.sortedBy { it.sortOrder }
    }

    override suspend fun loadTables(floorId: Int?): AppResult<List<RestaurantTable>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .listTables(floorId = floorId)
            .data
            .map { it.toDomain() }
    }

    override suspend fun loadStaff(): AppResult<List<StaffOption>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().listStaff().data.map {
            StaffOption(
                id = it.id,
                displayName = it.displayName ?: it.staffCode ?: "#${it.id}",
                employeeType = it.employeeType,
            )
        }
    }

    override suspend fun openTableSession(
        tableId: Int,
        guests: Int,
        notes: String?,
        staffId: Int?,
    ): AppResult<OpenSessionResult> = apiCall {
        val response = tenantRetrofitProvider.create<RestaurantApi>().openSession(
            OpenSessionRequestDto(
                tableId = tableId,
                guests = guests,
                notes = notes?.takeIf { it.isNotBlank() },
                staffId = staffId,
            ),
        )
        val session = response.data ?: error("No se pudo abrir la mesa")
        OpenSessionResult(sessionId = session.id, orderCode = session.orderCode)
    }

    override suspend fun getSession(sessionId: Int): AppResult<TableSessionDetail> = apiCall {
        val dto = tenantRetrofitProvider.create<RestaurantApi>()
            .getSession(sessionId)
            .data ?: error("Sesión no encontrada")
        dto.toDomain()
    }

    override suspend fun getPrecuenta(sessionId: Int): AppResult<PrecuentaData> = apiCall {
        val dto = tenantRetrofitProvider.create<RestaurantApi>()
            .getPrecuenta(sessionId)
            .data ?: error("Precuenta no disponible")
        dto.toDomain()
    }

    override suspend fun closeSession(sessionId: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().closeSession(sessionId)
    }

    override suspend fun createFloor(name: String, sortOrder: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().createFloor(
            FloorUpsertRequestDto(name = name, sortOrder = sortOrder),
        )
    }

    override suspend fun updateFloor(id: Int, name: String, sortOrder: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().updateFloor(
            id,
            FloorUpsertRequestDto(name = name, sortOrder = sortOrder),
        )
    }

    override suspend fun deleteFloor(id: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().deleteFloor(id)
    }

    override suspend fun createTable(floorId: Int, name: String, capacity: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().createTable(
            TableUpsertRequestDto(floorId = floorId, name = name, capacity = capacity),
        )
    }

    override suspend fun updateTable(
        id: Int,
        floorId: Int,
        name: String,
        capacity: Int,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().updateTable(
            id,
            TableUpsertRequestDto(floorId = floorId, name = name, capacity = capacity),
        )
    }

    override suspend fun deleteTable(id: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().deleteTable(id)
    }

    override suspend fun getOperationalStatus(): AppResult<BranchOperationalStatus> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .getOperationalStatus()
            .data
            ?.toDomain()
            ?: BranchOperationalStatus()
    }
}

private fun OperationalStatusDto.toDomain() = BranchOperationalStatus(
    openTablesCount = openTablesCount,
    openSessionsCount = openSessionsCount,
    pendingBillingCount = pendingBillingCount,
    activeComandasCount = activeComandasCount,
    hasActiveOperations = hasActiveOperations,
)

@Singleton
class KitchenRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : KitchenRepository {

    override suspend fun loadKitchen(): AppResult<List<KitchenItem>> = apiCall {
        val raw = tenantRetrofitProvider.create<RestaurantApi>().getKitchen().data.map { it.toDomain() }
        expandKitchenItemsForKds(raw)
    }

    override suspend fun updateComandaStatus(
        comandaId: Int,
        status: ComandaStatus,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().updateComandaStatus(
            comandaId,
            UpdateComandaStatusRequestDto(status = status.backendValue),
        )
    }

    override suspend fun cancelComanda(comandaId: Int, reason: String, pin: String): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .cancelComanda(comandaId, CancelComandaRequestDto(reason = reason.trim(), pin = pin.trim()))
    }
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun ProductDto.toDomain() = PosProduct(
    id = id,
    code = code,
    name = name,
    salePrice = salePrice,
    categoryId = categoryId,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    preparationArea = preparationArea?.let { normalizePreparationAreaName(it.name) },
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
    hasModifiers = hasModifiers,
    hasVariants = hasVariants,
    manageStock = manageStock,
    availableForSale = availableForSale,
)

private fun OrderItemInput.toDto() = OrderItemInputDto(
    itemKind = itemKind,
    productId = productId,
    productCode = productCode.takeIf { it.isNotBlank() },
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    notes = notes?.takeIf { it.isNotBlank() },
    modifiersJson = modifiersJson?.takeIf { it.isNotBlank() },
    comboId = comboId,
    comboConfigJson = comboConfigJson?.takeIf { it.isNotBlank() },
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
)

private fun ComandaDto.toDomain() = ComandaLine(
    id = id,
    productName = productName,
    quantity = quantity,
    notes = notes,
    modifiersJson = modifiersJson,
    status = ComandaStatus.fromBackend(status),
    preparationArea = preparationArea,
    comboSnapshotJson = comboSnapshotJson,
)

private fun RestaurantTableDto.toDomain() = RestaurantTable(
    id = id,
    floorId = floorId,
    floorName = floorName,
    name = name,
    capacity = capacity,
    status = TableStatus.fromBackend(status),
    sessionId = sessionId,
    totalAmount = totalAmount,
    waiterName = waiterName,
    guests = guests,
)

private fun KitchenComandaDto.toDomain() = KitchenItem(
    id = id,
    productName = productName,
    quantity = quantity,
    notes = notes,
    modifiersJson = modifiersJson,
    status = ComandaStatus.fromBackend(status),
    orderNumber = orderNumber,
    orderCode = orderCode,
    tableName = tableName,
    floorName = floorName,
    customerName = customerName,
    waiterName = waiterName,
    orderType = orderType,
    preparationArea = preparationArea,
    comboSnapshotJson = comboSnapshotJson,
    createdAt = createdAt,
    sessionOpenedAt = sessionOpenedAt,
    displayName = productName,
    displayQuantity = quantity,
)

private fun SessionDetailDto.toDomain() = TableSessionDetail(
    id = id,
    tableName = tableName,
    floorName = floorName,
    waiterName = waiterName,
    guests = guests,
    orderCode = orderCode,
    totalAmount = totalAmount,
    orderType = orderType,
    contactId = contactId,
    customerName = customerName,
    customerPhone = customerPhone,
    deliveryAddress = deliveryAddress,
    deliveryReference = deliveryReference,
    deliveryDriverId = deliveryDriverId,
    driverName = driverName,
    notes = notes,
    estimatedMinutes = estimatedMinutes,
    orders = orders.map { it.toDomain() },
)

private fun OpenOrderSummaryDto.toDomain() = OpenOrderSummary(
    id = id,
    orderCode = orderCode,
    orderType = orderType,
    orderStatus = orderStatus,
    customerName = customerName,
    customerPhone = customerPhone,
    deliveryAddress = deliveryAddress,
    totalAmount = totalAmount,
    itemCount = itemCount,
)

private fun PosSessionInput.toDto() = OpenSessionRequestDto(
    tableId = null,
    guests = 1,
    orderType = orderType,
    contactId = contactId,
    customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
    customerPhone = customerPhone?.trim()?.takeIf { it.isNotEmpty() },
    deliveryDriverId = deliveryDriverId,
    deliveryAddress = deliveryAddress?.trim()?.takeIf { it.isNotEmpty() },
    deliveryReference = deliveryReference?.trim()?.takeIf { it.isNotEmpty() },
    estimatedMinutes = estimatedMinutes,
    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
    saveAsDraft = saveAsDraft.takeIf { it },
)

private fun PosSessionInput.toUpdateDto() = OpenSessionRequestDto(
    tableId = null,
    guests = 1,
    orderType = orderType,
    contactId = contactId,
    customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
    customerPhone = customerPhone?.trim()?.takeIf { it.isNotEmpty() },
    deliveryDriverId = deliveryDriverId,
    deliveryAddress = deliveryAddress?.trim()?.takeIf { it.isNotEmpty() },
    deliveryReference = deliveryReference?.trim()?.takeIf { it.isNotEmpty() },
    estimatedMinutes = estimatedMinutes,
    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
    saveAsDraft = null,
)

private fun SessionOrderDto.toDomain() = SessionOrderSummary(
    id = id,
    orderNumber = orderNumber,
    comandas = comandas.map { comanda ->
        SessionComandaSummary(
            id = comanda.id,
            productName = comanda.productName,
            quantity = comanda.quantity,
            unitPrice = comanda.unitPrice,
            status = ComandaStatus.fromBackend(comanda.status),
            notes = comanda.notes,
            modifiersJson = comanda.modifiersJson,
            preparationArea = comanda.preparationArea,
            comboSnapshotJson = comanda.comboSnapshotJson,
        )
    },
)

private fun PrecuentaDto.toDomain() = PrecuentaData(
    tableName = tableName,
    orderCode = orderCode,
    total = total,
    lines = lines.map { line ->
        PrecuentaLine(
            productName = line.productName,
            quantity = line.quantity,
            unitPrice = line.unitPrice,
            lineTotal = line.lineTotal ?: (line.quantity * line.unitPrice),
        )
    },
)
