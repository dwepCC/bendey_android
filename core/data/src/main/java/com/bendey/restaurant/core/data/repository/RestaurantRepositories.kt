package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.AddOrderResult
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OpenSessionResult
import com.bendey.restaurant.core.domain.restaurant.OrderItemInput
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
import com.bendey.restaurant.core.network.dto.OpenSessionRequestDto
import com.bendey.restaurant.core.network.dto.OrderItemInputDto
import com.bendey.restaurant.core.network.dto.ProductDto
import com.bendey.restaurant.core.network.dto.PrecuentaDto
import com.bendey.restaurant.core.network.dto.RestaurantTableDto
import com.bendey.restaurant.core.network.dto.SessionDetailDto
import com.bendey.restaurant.core.network.dto.SessionOrderDto
import com.bendey.restaurant.core.network.dto.FloorUpsertRequestDto
import com.bendey.restaurant.core.network.dto.TableUpsertRequestDto
import com.bendey.restaurant.core.network.dto.UpdateComandaStatusRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : PosRepository {

    override suspend fun loadCategories(): AppResult<List<ProductCategory>> = apiCall {
        tenantRetrofitProvider.create<ProductsApi>().listCategories().data.map {
            ProductCategory(id = it.id, name = it.name)
        }
    }

    override suspend fun loadProducts(
        query: String,
        categoryId: Int?,
        page: Int,
        branchId: Int?,
    ): AppResult<Pair<List<PosProduct>, Int>> = apiCall {
        val response = tenantRetrofitProvider.create<ProductsApi>().listProducts(
            query = query,
            page = page,
            categoryId = categoryId,
            branchId = branchId,
        )
        val products = response.data.map { it.toDomain() }
        products to (response.total ?: products.size)
    }

    override suspend fun openCounterSession(orderType: String): AppResult<OpenSessionResult> = apiCall {
        val api = tenantRetrofitProvider.create<RestaurantApi>()
        val response = api.openSession(
            OpenSessionRequestDto(
                orderType = orderType,
                guests = 1,
            ),
        )
        val session = response.data ?: error("Sesión no creada")
        OpenSessionResult(sessionId = session.id, orderCode = session.orderCode)
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
            .sortedWith(compareBy({ it.floorId }, { it.name }))
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
}

@Singleton
class KitchenRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : KitchenRepository {

    override suspend fun loadKitchen(): AppResult<List<KitchenItem>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>().getKitchen().data.map { it.toDomain() }
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
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
    hasModifiers = hasModifiers,
    hasVariants = hasVariants,
    manageStock = manageStock,
    availableForSale = availableForSale,
)

private fun OrderItemInput.toDto() = OrderItemInputDto(
    itemKind = "product",
    productId = productId,
    productCode = productCode,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    notes = notes?.takeIf { it.isNotBlank() },
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
)

private fun SessionDetailDto.toDomain() = TableSessionDetail(
    id = id,
    tableName = tableName,
    floorName = floorName,
    waiterName = waiterName,
    guests = guests,
    orderCode = orderCode,
    totalAmount = totalAmount,
    orders = orders.map { it.toDomain() },
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
