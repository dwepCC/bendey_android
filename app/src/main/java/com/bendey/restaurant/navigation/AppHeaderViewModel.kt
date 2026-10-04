package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
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
) : ViewModel() {

    /** Estado de conexión real: WebSocket (RealtimeObservability) + red del dispositivo. */
    private val connectionStatus = combine(
        observability.snapshot.map { it.connectionState },
        networkStatusMonitor.hasNetwork,
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
    ) { tenant, session, tradeName, businessName, connection ->
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
            // Aún no existe un centro de notificaciones: sin conteo y la campana no se dibuja.
            notificationCount = 0,
            isAdmin = user?.isPinSession == false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BendeyAppHeaderState())
}
