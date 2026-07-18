package com.bendey.restaurant.core.realtime.store

import com.bendey.restaurant.core.domain.cash.CashBankAccount
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import com.bendey.restaurant.core.domain.restaurant.BranchOperationalStatus
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.OpenOrderSummary
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import javax.inject.Inject
import javax.inject.Singleton

private fun numericIdComparator(): (String, String, Map<String, *>) -> Int =
    { a, b, _ -> (a.toIntOrNull() ?: 0).compareTo(b.toIntOrNull() ?: 0) }

/** Pisos / salas — bootstrap HTTP. */
@Singleton
class FloorsStore @Inject constructor() :
    RealtimeStore<Floor> by createRealtimeStore(getId = { it.id }, sortIds = numericIdComparator())

/** Mesa operativa (Salas). */
@Singleton
class TablesStore @Inject constructor() :
    RealtimeStore<RestaurantTable> by createRealtimeStore(getId = { it.id }, sortIds = numericIdComparator())

/** Sesión activa (Mesa / POS). */
@Singleton
class SessionsStore @Inject constructor() :
    RealtimeStore<TableSessionDetail> by createRealtimeStore(getId = { it.id })

/** Pedidos abiertos POS (llevar / delivery). */
@Singleton
class OrdersStore @Inject constructor() :
    RealtimeStore<OpenOrderSummary> by createRealtimeStore(getId = { it.id })

/** KDS cocina. */
@Singleton
class KitchenStore @Inject constructor() :
    RealtimeStore<KitchenItem> by createRealtimeStore(getId = { it.id })

/** Ventas / cobros registrados vía realtime (`restaurant.bill.closed`). */
data class BillQueueItem(
    val id: Int,
    val saleId: Int? = null,
    val sessionId: Int? = null,
    val totalAmount: Double? = null,
    val status: String? = null,
)

@Singleton
class BillsStore @Inject constructor() :
    RealtimeStore<BillQueueItem> by createRealtimeStore(getId = { it.id })

/** KPI operativo de sucursal (dashboard / caja) — entidad única con clave fija `"status"`. */
@Singleton
class OperationalStore @Inject constructor() :
    RealtimeStore<BranchOperationalStatus> by createRealtimeStore(getId = { OPERATIONAL_STATUS_KEY }) {
    companion object {
        const val OPERATIONAL_STATUS_KEY = "status"
    }
}

/** Estado de caja por sucursal — única fuente para CajaScreen. */
data class CashStoreData(
    val id: Int,
    val openSession: CashSession? = null,
    val sessionHistory: List<CashSessionBrief> = emptyList(),
    val movements: List<CashMovement> = emptyList(),
    val bankAccounts: List<CashBankAccount> = emptyList(),
    val paymentMethods: List<CashPaymentMethod> = emptyList(),
)

@Singleton
class CashStore @Inject constructor() :
    RealtimeStore<CashStoreData> by createRealtimeStore(getId = { it.id })

/** Dashboard operacional / catálogo por rango de fechas. */
data class DashboardStoreData(
    val id: String,
    val operacion: RestaurantDashboard? = null,
    val catalog: CatalogAnalytics? = null,
    val lastUpdated: String? = null,
)

@Singleton
class DashboardStore @Inject constructor() :
    RealtimeStore<DashboardStoreData> by createRealtimeStore(getId = { it.id })

fun dashboardStoreKey(from: String, to: String): String = "$from:$to"

/** Agrupa los 9 stores restaurante — usado por `RestaurantDomain.getStores()`. */
@Singleton
class RestaurantStores @Inject constructor(
    val floors: FloorsStore,
    val tables: TablesStore,
    val sessions: SessionsStore,
    val orders: OrdersStore,
    val kitchen: KitchenStore,
    val bills: BillsStore,
    val operational: OperationalStore,
    val cash: CashStore,
    val dashboard: DashboardStore,
) {
    fun all(): List<RealtimeStore<*>> = listOf(floors, tables, sessions, orders, kitchen, bills, operational, cash, dashboard)
}
