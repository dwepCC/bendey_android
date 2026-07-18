package com.bendey.restaurant.core.realtime.recovery

import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.realtime.domains.DomainRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/** Único módulo autorizado a HTTP para sincronizar Domain Stores. Puerto de `recovery/index.ts` (Tauri). */
@Singleton
class RealtimeRecovery @Inject constructor(
    private val hydrators: RestaurantHydrators,
    private val domainRegistry: DomainRegistry,
    private val observability: RealtimeObservability,
) {
    private val inFlight = mutableSetOf<String>()

    private var cashSessionIdProvider: (() -> Int?)? = null
    private var activeBranchIdProvider: (() -> Int?)? = null
    private var canViewCashConfigProvider: (() -> Boolean)? = null

    fun setCashSessionIdProvider(provider: () -> Int?) {
        cashSessionIdProvider = provider
    }

    fun setActiveBranchIdProvider(provider: () -> Int?) {
        activeBranchIdProvider = provider
    }

    fun setCanViewCashConfigProvider(provider: () -> Boolean) {
        canViewCashConfigProvider = provider
    }

    suspend fun restoreEntity(domain: String, entity: String, id: Int) {
        run(RecoveryRequest(RecoveryPolicy.ENTITY, RecoveryScope(domain = domain, entity = entity, entityId = id), "restore_entity"))
    }

    suspend fun restoreDomain(domain: String, scope: RecoveryScope? = null) {
        run(RecoveryRequest(RecoveryPolicy.DOMAIN, (scope ?: RecoveryScope()).copy(domain = domain), "restore_domain"))
    }

    suspend fun restoreForBranch(branchId: Int) {
        run(RecoveryRequest(RecoveryPolicy.BRANCH, RecoveryScope(branchId = branchId), "branch_change"))
    }

    suspend fun restoreAll() {
        run(RecoveryRequest(RecoveryPolicy.FULL, reason = "restore_all"))
    }

    suspend fun evaluatePostReconnect() {
        run(RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "operational"), "reconnect"))
    }

    fun resetAllStores() {
        domainRegistry.getAllStores().forEach { it.reset() }
    }

    suspend fun run(req: RecoveryRequest) {
        val key = "${req.policy}:${req.scope?.slice ?: req.scope?.entity ?: ""}:${req.scope?.entityId ?: ""}:${req.reason ?: ""}"
        if (!inFlight.add(key)) return
        observability.recordRecovery(req.policy.name)
        try {
            execute(req)
        } finally {
            inFlight.remove(key)
        }
    }

    private suspend fun execute(req: RecoveryRequest) {
        val slice = req.scope?.slice
        val domain = req.scope?.domain

        if (req.policy == RecoveryPolicy.ENTITY && domain == "restaurant" && req.scope?.entity == "session" && req.scope.entityId != null) {
            hydrators.hydrateSession(req.scope.entityId)
            return
        }

        if (domain != "restaurant" && req.policy != RecoveryPolicy.FULL && req.policy != RecoveryPolicy.BRANCH) return

        when (slice) {
            "tables" -> {
                hydrators.hydrateTables()
                return
            }
            "kitchen" -> {
                hydrators.hydrateKitchen()
                return
            }
            "orders" -> {
                hydrators.hydrateOpenOrders()
                return
            }
            "operational" -> {
                hydrators.hydrateOperationalStatus()
                return
            }
            "cash" -> {
                val branchId = activeBranchIdProvider?.invoke() ?: return
                if (req.reason == "bill_closed" || req.reason == "cash_movements") {
                    val cashId = cashSessionIdProvider?.invoke()
                    if (cashId != null) hydrators.hydrateCashMovements(branchId, cashId)
                } else {
                    hydrators.hydrateCash(branchId, canViewCashConfigProvider?.invoke() ?: false)
                }
                return
            }
            "dashboard-operacion" -> {
                val from = req.scope?.dateFrom
                val to = req.scope?.dateTo
                if (from != null && to != null) hydrators.hydrateDashboardOperacion(from, to)
                return
            }
            "dashboard-catalog" -> {
                val from = req.scope?.dateFrom
                val to = req.scope?.dateTo
                if (from != null && to != null) hydrators.hydrateDashboardCatalog(from, to)
                return
            }
            "floors" -> {
                hydrators.hydrateFloors()
                return
            }
            else -> Unit
        }

        if (req.policy == RecoveryPolicy.FULL || req.policy == RecoveryPolicy.BRANCH) {
            coroutineScope {
                val a = async { hydrators.hydrateFloors() }
                val b = async { hydrators.hydrateTables() }
                val c = async { hydrators.hydrateOperationalStatus() }
                a.await()
                b.await()
                c.await()
            }
        }
    }
}
