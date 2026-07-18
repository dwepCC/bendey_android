package com.bendey.restaurant.core.realtime

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.connection.ConnectionSession
import com.bendey.restaurant.core.realtime.connection.RealtimeConnectionPolicy
import com.bendey.restaurant.core.realtime.dispatcher.ConnectionState
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeDispatcher
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.realtime.dispatcher.ValidateContext
import com.bendey.restaurant.core.realtime.effects.SideEffectContext
import com.bendey.restaurant.core.realtime.effects.SideEffectRunner
import com.bendey.restaurant.core.realtime.recovery.RealtimeRecovery
import com.bendey.restaurant.core.realtime.store.RestaurantStores
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conecta el WS staff, cablea el pipeline Realtime V2 y aplica side effects.
 * Puerto de `RestaurantLayout -> useStaffOrderAlerts -> useRealtimeConnection` (Tauri).
 */
@Singleton
class StaffOrderAlertsCoordinator @Inject constructor(
    private val realtimeClient: BendeyRealtimeClient,
    private val sessionStore: UserSessionStore,
    private val platform: RealtimePlatform,
    private val dispatcher: RealtimeDispatcher,
    private val observability: RealtimeObservability,
    private val sideEffectRunner: SideEffectRunner,
    private val realtimeRecovery: RealtimeRecovery,
    private val restaurantStores: RestaurantStores,
) {
    private val latestPermissions = MutableStateFlow<List<String>>(emptyList())
    private val latestBranchId = MutableStateFlow<Int?>(null)
    private val latestEmployeeType = MutableStateFlow<String?>(null)

    fun start(scope: CoroutineScope) {
        platform.init()
        wireProviders()

        scope.launch {
            sessionStore.userSessionFlow.collect { session ->
                latestPermissions.value = session?.restaurantPermissions.orEmpty()
                latestBranchId.value = session?.activeBranch?.id
                latestEmployeeType.value = session?.user?.employeeType
            }
        }

        scope.launch {
            sessionStore.userSessionFlow
                .map { session ->
                    RealtimeConnectionPolicy.shouldConnect(
                        ConnectionSession(
                            isAuthenticated = session != null,
                            restaurantPermissions = session?.restaurantPermissions.orEmpty(),
                        ),
                    )
                }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        observability.setConnectionState(ConnectionState.CONNECTING)
                        realtimeClient.connect()
                    } else {
                        observability.setConnectionState(ConnectionState.DISCONNECTED)
                        realtimeClient.disconnect()
                    }
                }
        }

        scope.launch {
            realtimeClient.connected.collect { connected ->
                observability.setConnectionState(if (connected) ConnectionState.READY else ConnectionState.RECONNECTING)
            }
        }

        scope.launch {
            // drop(1): el primer valor es la carga inicial, ya cubierta por el refresh() de cada ViewModel.
            realtimeClient.connected
                .drop(1)
                .distinctUntilChanged()
                .collect { connected -> if (connected) realtimeRecovery.evaluatePostReconnect() }
        }

        scope.launch {
            realtimeClient.events.collect { evt -> dispatcher.dispatch(evt) }
        }

        scope.launch {
            // MutableStateFlow ya conflacia por igualdad — distinctUntilChanged sería no-op (deprecado en StateFlow).
            latestBranchId
                .drop(1)
                .collect { branchId ->
                    realtimeRecovery.resetAllStores()
                    if (branchId != null) realtimeRecovery.restoreForBranch(branchId)
                }
        }
    }

    private fun wireProviders() {
        dispatcher.setValidateContextProvider {
            ValidateContext(
                activeBranchId = latestBranchId.value,
                activeTenantId = realtimeClient.authOk.value?.tenantId,
            )
        }
        sideEffectRunner.setContextProvider { SideEffectContext(restaurantPermissions = latestPermissions.value) }
        realtimeRecovery.setActiveBranchIdProvider { latestBranchId.value }
        realtimeRecovery.setCanViewCashConfigProvider {
            RestaurantPermissions.canViewCashSettings(latestPermissions.value, latestEmployeeType.value)
        }
        realtimeRecovery.setCashSessionIdProvider {
            val branchId = latestBranchId.value ?: return@setCashSessionIdProvider null
            restaurantStores.cash.getSnapshot().entities[branchId.toString()]?.openSession?.id
        }
    }
}
