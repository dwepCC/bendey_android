package com.bendey.restaurant.feature.auth.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.auth.RegistrationCoordinator
import com.bendey.restaurant.core.data.auth.RegistrationLoginHint
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.waiter.StationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qué hacer al abrir Inicio según la estación recordada del dispositivo (R8). */
sealed interface StationJump {
    /** Aún leyendo la preferencia: no se pinta la elección para no parpadear. */
    data object Loading : StationJump

    /** No hay estación recordada (o hay un aviso que atender): mostrar la elección normal. */
    data object Choose : StationJump

    /** Saltar directo al PIN de esta estación. */
    data class To(val station: PinStation) : StationJump
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionStore: UserSessionStore,
    private val registrationCoordinator: RegistrationCoordinator,
    private val stationPreferences: StationPreferences,
) : ViewModel() {
    private val _jump = MutableStateFlow<StationJump>(StationJump.Loading)
    val jump: StateFlow<StationJump> = _jump.asStateFlow()

    init {
        viewModelScope.launch {
            val station = stationPreferences.remembered()
            // Con un aviso pendiente (registro recién hecho) no se salta: el usuario debe verlo.
            _jump.value = if (station != null && registrationCoordinator.loginHint.value == null) {
                StationJump.To(station)
            } else {
                StationJump.Choose
            }
        }
    }

    val tenant: StateFlow<TenantBinding?> = sessionStore.tenantFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Aviso tras crear el restaurante cuando el inicio de sesión automático falló. */
    val loginHint: StateFlow<RegistrationLoginHint?> = registrationCoordinator.loginHint

    fun dismissLoginHint() = registrationCoordinator.consumeLoginHint()
}
