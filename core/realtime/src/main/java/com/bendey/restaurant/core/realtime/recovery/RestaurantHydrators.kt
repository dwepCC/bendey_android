package com.bendey.restaurant.core.realtime.recovery

import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.dashboard.DashboardRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OpenOrderSummary
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import com.bendey.restaurant.core.realtime.store.BillQueueItem
import com.bendey.restaurant.core.realtime.store.CashStoreData
import com.bendey.restaurant.core.realtime.store.DashboardStoreData
import com.bendey.restaurant.core.realtime.store.OperationalStore
import com.bendey.restaurant.core.realtime.store.PatchResult
import com.bendey.restaurant.core.realtime.store.RestaurantStores
import com.bendey.restaurant.core.realtime.store.StoreMeta
import com.bendey.restaurant.core.realtime.store.StoreSnapshot
import com.bendey.restaurant.core.realtime.store.dashboardStoreKey
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private fun <T> AppResult<T>.dataOrNull(): T? = (this as? AppResult.Success<T>)?.data

private fun nowIso(): String = Instant.now().toString()

private fun <T> snapshotFromList(
    items: List<T>,
    getId: (T) -> Any,
    sortIds: ((String, String) -> Int)? = null,
): StoreSnapshot<T> {
    val entities = LinkedHashMap<String, T>()
    val ids = mutableListOf<String>()
    for (item in items) {
        val key = getId(item).toString()
        entities[key] = item
        ids.add(key)
    }
    if (sortIds != null) ids.sortWith(Comparator(sortIds))
    return StoreSnapshot(entities = entities, ids = ids, meta = StoreMeta(hydratedAt = nowIso(), loading = false))
}

/** Puerto de `recovery/restaurantHydrators.ts` (Tauri) — mismos endpoints, mismos stores destino. */
@Singleton
class RestaurantHydrators @Inject constructor(
    private val mesasRepository: MesasRepository,
    private val posRepository: PosRepository,
    private val kitchenRepository: KitchenRepository,
    private val cashRepository: CashRepository,
    private val dashboardRepository: DashboardRepository,
    private val stores: RestaurantStores,
) {
    /** Retorna el resultado crudo (además de hidratar el store) — preserva mensaje de error en Salas. */
    suspend fun hydrateFloors(): AppResult<Unit> {
        return when (val result = mesasRepository.loadFloors()) {
            is AppResult.Success -> {
                stores.floors.hydrate(
                    snapshotFromList(result.data, { it.id }, { a, b -> (a.toIntOrNull() ?: 0) - (b.toIntOrNull() ?: 0) }),
                )
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
    }

    suspend fun hydrateTables(floorId: Int? = null): AppResult<Unit> {
        val numericSort: (String, String) -> Int = { a, b -> (a.toIntOrNull() ?: 0) - (b.toIntOrNull() ?: 0) }
        return when (val result = mesasRepository.loadTables(floorId)) {
            is AppResult.Success -> {
                val tables = result.data
                if (floorId == null) {
                    stores.tables.hydrate(snapshotFromList(tables, { it.id }, numericSort))
                } else {
                    val current = stores.tables.getSnapshot()
                    val merged = current.entities.toMutableMap()
                    val idSet = LinkedHashSet(current.ids)
                    for (t in tables) {
                        val key = t.id.toString()
                        merged[key] = t
                        idSet.add(key)
                    }
                    val ids = idSet.sortedWith(Comparator(numericSort))
                    stores.tables.hydrate(
                        StoreSnapshot(
                            entities = merged,
                            ids = ids,
                            meta = current.meta.copy(hydratedAt = nowIso(), loading = false, stale = false),
                        ),
                    )
                }
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
    }

    /** Retorna `true` si la carga fue exitosa — preserva feedback de error en refresh manual (CocinaViewModel). */
    suspend fun hydrateKitchen(): Boolean {
        val comandas = kitchenRepository.loadKitchen().dataOrNull() ?: return false
        stores.kitchen.hydrate(snapshotFromList(comandas, { it.id }))
        return true
    }

    suspend fun hydrateOpenOrders() {
        val rows = posRepository.listOpenOrders().dataOrNull() ?: return
        val filtered = rows.filter { it.orderType == "takeaway" || it.orderType == "delivery" }
        stores.orders.hydrate(snapshotFromList(filtered, { it.id }))
    }

    suspend fun hydrateSession(sessionId: Int): TableSessionDetail? {
        return when (val result = mesasRepository.getSession(sessionId)) {
            is AppResult.Success -> {
                stores.sessions.upsert(result.data)
                stores.sessions.setMeta { it.copy(hydratedAt = nowIso(), loading = false) }
                result.data
            }
            else -> {
                if (stores.sessions.getSnapshot().entities.containsKey(sessionId.toString())) {
                    stores.sessions.remove(sessionId.toString())
                }
                null
            }
        }
    }

    suspend fun hydrateOperationalStatus() {
        val status = mesasRepository.getOperationalStatus().dataOrNull() ?: return
        stores.operational.hydrate(
            StoreSnapshot(
                entities = mapOf(OperationalStore.OPERATIONAL_STATUS_KEY to status),
                ids = listOf(OperationalStore.OPERATIONAL_STATUS_KEY),
                meta = StoreMeta(hydratedAt = nowIso(), loading = false),
            ),
        )
    }

    suspend fun hydrateCash(branchId: Int, canViewCashConfig: Boolean) {
        val openSession = cashRepository.getOpenSession(branchId).dataOrNull()
        val sessionHistory = cashRepository.listSessions(branchId).dataOrNull().orEmpty()
        val bankAccounts = if (canViewCashConfig) cashRepository.listBankAccounts().dataOrNull().orEmpty() else emptyList()
        val paymentMethods = if (canViewCashConfig) cashRepository.listPaymentMethods().dataOrNull().orEmpty() else emptyList()

        val validSession = openSession?.takeIf { it.branchId == branchId }
        val movements = validSession?.id?.let { cashRepository.listMovements(it).dataOrNull().orEmpty() }.orEmpty()

        val payload = CashStoreData(
            id = branchId,
            openSession = validSession,
            sessionHistory = sessionHistory,
            movements = movements,
            bankAccounts = bankAccounts,
            paymentMethods = paymentMethods,
        )

        stores.cash.hydrate(
            StoreSnapshot(
                entities = mapOf(branchId.toString() to payload),
                ids = listOf(branchId.toString()),
                meta = StoreMeta(hydratedAt = nowIso(), loading = false, branchId = branchId),
            ),
        )
    }

    suspend fun hydrateCashMovements(branchId: Int, openCashSessionId: Int) {
        val fetchedMovements = cashRepository.listMovements(openCashSessionId).dataOrNull().orEmpty()
        val key = branchId.toString()
        val existing = stores.cash.getSnapshot().entities[key]
        if (existing != null) {
            stores.cash.patch(key) { copy(movements = fetchedMovements) }
            return
        }
        hydrateCash(branchId, canViewCashConfig = true)
    }

    suspend fun hydrateDashboardOperacion(from: String, to: String): AppResult<Unit> {
        return when (val result = dashboardRepository.loadDashboard(from, to)) {
            is AppResult.Success -> {
                val id = dashboardStoreKey(from, to)
                val prev = stores.dashboard.getSnapshot().entities[id]
                stores.dashboard.upsert(
                    DashboardStoreData(id = id, operacion = result.data, catalog = prev?.catalog, lastUpdated = nowIso()),
                )
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
    }

    suspend fun hydrateDashboardCatalog(from: String, to: String): AppResult<Unit> {
        return when (val result = dashboardRepository.loadCatalogAnalytics(from, to)) {
            is AppResult.Success -> {
                val id = dashboardStoreKey(from, to)
                val prev = stores.dashboard.getSnapshot().entities[id]
                stores.dashboard.upsert(
                    DashboardStoreData(id = id, operacion = prev?.operacion, catalog = result.data, lastUpdated = nowIso()),
                )
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
    }

    fun patchTableFromSessionEvent(tableId: Int, partial: RestaurantTable.() -> RestaurantTable): Boolean =
        stores.tables.patch(tableId.toString(), partial) == PatchResult.APPLIED

    fun patchSessionFromEvent(sessionId: Int, partial: TableSessionDetail.() -> TableSessionDetail): Boolean {
        if (!stores.sessions.getSnapshot().entities.containsKey(sessionId.toString())) return false
        return stores.sessions.patch(sessionId.toString(), partial) == PatchResult.APPLIED
    }

    fun recordBillClosed(saleId: Int, sessionId: Int, total: Double, paymentStatus: String?) {
        stores.bills.upsert(
            BillQueueItem(id = saleId, saleId = saleId, sessionId = sessionId, totalAmount = total, status = paymentStatus ?: "paid"),
        )
    }
}
