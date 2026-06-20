package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.cache.OperationalDataPreloader
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val sessionStore: UserSessionStore,
    private val authRepository: AuthRepository,
    operationalDataPreloader: OperationalDataPreloader,
) : ViewModel() {

    /** null = hidratando DataStore (evita flash de pantalla RUC). */
    val isTenantBound: StateFlow<Boolean?> = sessionStore.isTenantBoundFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isAuthenticated: StateFlow<Boolean?> = sessionStore.isAuthenticatedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        operationalDataPreloader.preloadActiveBranch()
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
