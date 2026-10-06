package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.delivery.canViewDelivery
import com.bendey.restaurant.core.domain.delivery.isDeliveryDriver
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.realtime.pending.PendingApprovalStore
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
import com.bendey.restaurant.core.ui.components.BendeyConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Paridad con `resolveHeaderDisplayName` en RestaurantHeader.tsx */
private fun resolveHeaderDisplayName(tradeName: String, businessName: String, tenantName: String): String {
    tradeName.trim().takeIf { it.isNotEmpty() }?.let { return it }
    businessName.trim().takeIf { it.isNotEmpty() }?.let { return it }
    return tenantName.trim().ifBlank { "Restaurante" }
}

@HiltViewModel
class AppHeaderViewModel @Inject constructor(
    sessionStore: UserSessionStore,
    private val settingsRepository: SettingsRepository,
    observability: RealtimeObservability,
    networkStatusMonitor: NetworkStatusMonitor,
    reachabilityMonitor: BackendReachabilityMonitor,
    pendingApprovalStore: PendingApprovalStore,
    deliveryBoardStore: DeliveryBoardStore,
) : ViewModel() {

    /** Pedidos del QR que acaban de llegar (nombre de mesa o null), para avisar en pantalla. */
    val pendingArrivals = pendingApprovalStore.arrivals

    /** D1: pedidos de delivery que acaban de aparecer en "Por asignar" (nunca en la carga inicial). Para avisar en pantalla. */
    val deliveryArrivals = deliveryBoardStore.arrivals

    /**
     * D1: contador del ítem Entregas de la barra = pedidos de delivery por asignar. Solo con `d.v` y no para el
     * repartidor (su vista es de solo lectura y a él no le toca asignar). 0 = sin globo.
     */
    val deliveryBadgeCount: StateFlow<Int> = combine(sessionStore.userSessionFlow, deliveryBoardStore.state) { session, board ->
        if (session != null && !isDeliveryDriver(session.user.employeeType) && canViewDelivery(session.restaurantPermissions)) board.unassignedCount else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        reachabilityMonitor.start()
    }

    /** Estado de conexión real: red del dispositivo + sonda al backend + WebSocket. Nunca verde sin comprobar. */
    private val connectionStatus = combine(
        observability.snapshot.map { it.connectionState },
        networkStatusMonitor.hasNetwork,
        reachabilityMonitor.level,
        ::resolveConnectionStatus,
    )

    private val companyTradeName = MutableStateFlow("")
    private val companyBusinessName = MutableStateFlow("")

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                if (session == null) {
                    companyTradeName.value = ""
                    companyBusinessName.value = ""
                    return@collect
                }
                when (val result = settingsRepository.getCompanyConfig()) {
                    is AppResult.Success -> {
                        companyTradeName.value = result.data.tradeName
                        companyBusinessName.value = result.data.businessName
                    }
                    else -> {
                        companyTradeName.value = ""
                        companyBusinessName.value = ""
                    }
                }
            }
        }
    }

    val headerState: StateFlow<BendeyAppHeaderState> = combine(
        sessionStore.tenantFlow,
        sessionStore.userSessionFlow,
        companyTradeName,
        companyBusinessName,
        connectionStatus,
        pendingApprovalStore.state,
    ) { values ->
        val tenant = values[0] as com.bendey.restaurant.core.domain.model.TenantBinding?
        val session = values[1] as com.bendey.restaurant.core.domain.model.UserSession?
        val tradeName = values[2] as String
        val businessName = values[3] as String
        val connection = values[4] as BendeyConnectionStatus
        val pending = values[5] as com.bendey.restaurant.core.realtime.pending.PendingApprovalState
        val user = session?.user
        val name = user?.name.orEmpty()
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
        BendeyAppHeaderState(
            restaurantName = resolveHeaderDisplayName(
                tradeName = tradeName,
                businessName = businessName,
                tenantName = tenant?.name.orEmpty(),
            ),
            branchName = session?.activeBranch?.name.orEmpty(),
            userName = name,
            userInitials = initials,
            connection = connection,
            // La campana abre la cola de pedidos del cliente por revisar (R10.1/R10.2); el conteo solo lo ve
            // quien puede revisarlos.
            notificationCount = PendingApprovalLogic.visibleCount(session?.restaurantPermissions, pending.count),
            isAdmin = user?.isPinSession == false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BendeyAppHeaderState())
}
