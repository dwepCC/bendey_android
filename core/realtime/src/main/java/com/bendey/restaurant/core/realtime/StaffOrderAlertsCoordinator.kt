package com.bendey.restaurant.core.realtime

import com.bendey.restaurant.core.domain.delivery.DeliveryThresholds
import com.bendey.restaurant.core.domain.delivery.canOperateDeliveryBoard
import com.bendey.restaurant.core.domain.delivery.canViewDelivery
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.realtime.pending.PendingApprovalStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.connection.AppForeground
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
    private val appForeground: AppForeground,
    private val pendingApprovalStore: PendingApprovalStore,
    private val deliveryBoardStore: DeliveryBoardStore,
    private val soundPlayer: NewOrderSoundPlayer,
) {
    private val latestPermissions = MutableStateFlow<List<String>>(emptyList())
    private val latestBranchId = MutableStateFlow<Int?>(null)
    private val latestEmployeeType = MutableStateFlow<String?>(null)

    fun start(scope: CoroutineScope) {
        platform.init()
        // Se llama desde el hilo principal (init del AppSessionViewModel), que es lo que exige
        // ProcessLifecycleOwner.
        appForeground.observar()
        wireProviders()

        scope.launch {
            sessionStore.userSessionFlow.collect { session ->
                latestPermissions.value = session?.restaurantPermissions.orEmpty()
                latestBranchId.value = session?.activeBranch?.id
                latestEmployeeType.value = session?.user?.employeeType
                // R10.1: solo quien puede revisar la cola la consulta (y ve el badge).
                pendingApprovalStore.setEnabled(
                    session?.activeBranch?.id != null && PendingApprovalLogic.canView(session.restaurantPermissions),
                )
                applyDeliveryPolicy(session?.activeBranch?.id, session?.restaurantPermissions.orEmpty(), session?.user?.employeeType)
            }
        }

        scope.launch {
            // D1: tablero de Delivery al día desde cualquier pantalla (badge de Entregas + sonido). Carga al
            // entrar / volver a primer plano / cambiar de sucursal / reconectar el tiempo real; el respaldo de
            // 60 s corre SOLO mientras el tiempo real está caído (con el WebSocket arriba no hay polling).
            combine(sessionStore.userSessionFlow, appForeground.enPantalla, realtimeClient.connected) { session, enPantalla, connected ->
                val branchId = session?.activeBranch?.id
                val perms = session?.restaurantPermissions.orEmpty()
                applyDeliveryPolicy(branchId, perms, session?.user?.employeeType)
                if (enPantalla && branchId != null && canViewDelivery(perms)) DeliverySyncKey(branchId, connected) else null
            }
                .distinctUntilChanged()
                .collectLatest { key ->
                    if (key == null) return@collectLatest
                    deliveryBoardStore.refresh()
                    if (!key.realtimeUp) {
                        while (true) {
                            delay(DeliveryThresholds.BACKUP_POLL_MS)
                            deliveryBoardStore.refresh()
                        }
                    }
                }
        }

        scope.launch {
            // Aviso de "pedido nuevo por asignar": nunca en la carga inicial (el store solo emite por diferencia).
            deliveryBoardStore.arrivals.collect {
                soundPlayer.play()
                soundPlayer.vibrateShort()
            }
        }

        scope.launch {
            // Cola de pedidos por revisar: al volver a primer plano, al cambiar de sucursal/permiso y con un
            // respaldo lento por si se pierde un evento. El evento en tiempo real es la via normal.
            combine(sessionStore.userSessionFlow, appForeground.enPantalla) { session, enPantalla ->
                val on = enPantalla && session?.activeBranch?.id != null &&
                    PendingApprovalLogic.canView(session.restaurantPermissions)
                if (on) session?.activeBranch?.id else null
            }
                .distinctUntilChanged()
                .collectLatest { branchId ->
                    if (branchId == null) return@collectLatest
                    while (true) {
                        pendingApprovalStore.refresh()
                        delay(PendingApprovalLogic.BACKUP_REFRESH_MS)
                    }
                }
        }

        scope.launch {
            combine(sessionStore.userSessionFlow, appForeground.enPantalla) { session, enPantalla ->
                RealtimeConnectionPolicy.shouldConnect(
                    ConnectionSession(
                        isAuthenticated = session != null,
                        restaurantPermissions = session?.restaurantPermissions.orEmpty(),
                        isForeground = enPantalla,
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
                .collect { connected ->
                    if (connected) {
                        realtimeRecovery.evaluatePostReconnect()
                        pendingApprovalStore.refresh()
                    }
                }
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
                    pendingApprovalStore.reset()
                    deliveryBoardStore.reset()
                    deliveryBoardStore.refreshAsync()
                    if (branchId != null) realtimeRecovery.restoreForBranch(branchId)
                }
        }
    }

    private data class DeliverySyncKey(val branchId: Int, val realtimeUp: Boolean)

    /**
     * D1: quién consulta el tablero y quién oye la alerta. Ver (badge, vista) = `d.v` con sucursal; la alerta
     * (sonido/vibración/aviso) solo a quien puede asignar (`d.u`) y recibe alertas de pedido nuevo, y nunca al
     * repartidor (su vista es de solo lectura). Idempotente.
     */
    private fun applyDeliveryPolicy(branchId: Int?, perms: List<String>, employeeType: String?) {
        val canView = branchId != null && canViewDelivery(perms)
        deliveryBoardStore.setEnabled(canView)
        deliveryBoardStore.alertsEnabled = canView && canOperateDeliveryBoard(perms, employeeType) &&
            RestaurantPermissions.canReceiveNewOrderSound(perms)
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
