package com.bendey.restaurant.feature.auth.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.auth.RegistrationCoordinator
import com.bendey.restaurant.core.data.auth.RegistrationLoginHint
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionStore: UserSessionStore,
    private val registrationCoordinator: RegistrationCoordinator,
) : ViewModel() {
    val tenant: StateFlow<TenantBinding?> = sessionStore.tenantFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Aviso tras crear el restaurante cuando el inicio de sesión automático falló. */
    val loginHint: StateFlow<RegistrationLoginHint?> = registrationCoordinator.loginHint

    fun dismissLoginHint() = registrationCoordinator.consumeLoginHint()
}
