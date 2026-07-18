package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.cache.OperationalDataPreloader
import com.bendey.restaurant.core.realtime.StaffOrderAlertsCoordinator
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.ui.permission.PermissionContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val sessionStore: UserSessionStore,
    private val authRepository: AuthRepository,
    operationalDataPreloader: OperationalDataPreloader,
    private val staffOrderAlertsCoordinator: StaffOrderAlertsCoordinator,
) : ViewModel() {

    /** null = hidratando DataStore (evita flash de pantalla RUC). */
    val isTenantBound: StateFlow<Boolean?> = sessionStore.isTenantBoundFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isAuthenticated: StateFlow<Boolean?> = sessionStore.isAuthenticatedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionContext: StateFlow<PermissionContext?> = sessionStore.userSessionFlow
        .map { session ->
            session?.let {
                PermissionContext(
                    permissions = it.restaurantPermissions,
                    employeeType = it.user.employeeType,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Cambia en cada login — fuerza ruta inicial correcta al entrar al shell principal. */
    val sessionKey: StateFlow<String?> = sessionStore.userSessionFlow
        .map { it?.token }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            operationalDataPreloader.preloadActiveBranch()
        }
        staffOrderAlertsCoordinator.start(viewModelScope)
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .distinctUntilChanged { old, new -> old?.token == new?.token }
                .collect { session ->
                    if (session != null) {
                        runCatching { authRepository.refreshRestaurantPermissions() }
                    }
                }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
