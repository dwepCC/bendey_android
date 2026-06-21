package com.bendey.restaurant.feature.auth.pin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinUiState(
    val station: PinStation = PinStation.WAITER,
    val pin: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val stationKey: String = savedStateHandle.get<String>("station").orEmpty()

    private val _uiState = MutableStateFlow(
        PinUiState(station = PinStation.fromRouteKey(stationKey) ?: PinStation.WAITER),
    )
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    fun appendDigit(digit: String) {
        if (_uiState.value.pin.length >= 6) return
        _uiState.update { it.copy(pin = it.pin + digit, error = null) }
    }

    fun backspace() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    fun submit(onSuccess: (route: String) -> Unit) {
        val state = _uiState.value
        if (state.pin.length < 4) {
            _uiState.update { it.copy(error = "Ingrese al menos 4 dígitos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            authRepository.loginWithPin(state.pin, state.station)
                .onSuccess { session ->
                    _uiState.update { it.copy(loading = false) }
                    if (!RestaurantPermissions.hasOperationalAccess(session.restaurantPermissions)) {
                        authRepository.logout()
                        _uiState.update { it.copy(error = "Tu usuario no tiene permisos operativos") }
                    } else {
                        val route = RestaurantPermissions.postLoginRoute(
                            session.restaurantPermissions,
                            state.station,
                            session.user.employeeType,
                        )
                        onSuccess(route)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "PIN incorrecto", pin = "")
                    }
                }
        }
    }
}
