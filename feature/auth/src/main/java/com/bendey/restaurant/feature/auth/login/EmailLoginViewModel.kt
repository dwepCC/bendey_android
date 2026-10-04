package com.bendey.restaurant.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.auth.RegistrationCoordinator
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmailLoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val registrationCoordinator: RegistrationCoordinator,
) : ViewModel() {

    // Si el restaurante se acaba de crear pero el inicio automático falló, el correo ya viene puesto.
    private val _uiState = MutableStateFlow(
        EmailLoginUiState(email = registrationCoordinator.loginHint.value?.email.orEmpty()),
    )
    val uiState: StateFlow<EmailLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun submit(onSuccess: (route: String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Escribe tu correo y tu contraseña.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            authRepository.loginWithEmail(state.email, state.password)
                .onSuccess { session ->
                    _uiState.update { it.copy(loading = false) }
                    if (!RestaurantPermissions.hasOperationalAccess(session.restaurantPermissions)) {
                        authRepository.logout()
                        _uiState.update { it.copy(error = "Tu usuario no tiene permisos operativos") }
                    } else {
                        val route = RestaurantPermissions.defaultRoute(
                            session.restaurantPermissions,
                            session.user.employeeType,
                        )
                        registrationCoordinator.consumeLoginHint()
                        onSuccess(route)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "No se pudo iniciar sesión. Revisa tus datos y vuelve a intentar.") }
                }
        }
    }
}
