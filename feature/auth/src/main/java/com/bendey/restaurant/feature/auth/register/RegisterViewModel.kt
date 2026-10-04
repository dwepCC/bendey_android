package com.bendey.restaurant.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.auth.RegistrationCoordinator
import com.bendey.restaurant.core.domain.auth.RestaurantRegistrationInput
import com.bendey.restaurant.core.domain.auth.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RUC_LENGTH = 11
private const val MIN_PASSWORD_LENGTH = 6

/**
 * Registro en UNA pantalla (R4, DEC-12): RUC → razón social con ✓ → correo + contraseña (con
 * "mostrar", sin "confirmar"). El nombre comercial viaja precargado con la razón social y el
 * celular va vacío: ambos se completan en el wizard.
 */
data class RegisterUiState(
    val ruc: String = "",
    val razonSocial: String = "",
    val rucValidated: Boolean = false,
    val validating: Boolean = false,
    val address: String = "",
    val ubigeo: String = "",
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    /** Creando el restaurante (y entrando): se muestra el texto honesto de espera. */
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canValidate: Boolean
        get() = ruc.length == RUC_LENGTH && !validating && !loading && !rucValidated

    val canSubmit: Boolean
        get() = rucValidated &&
            razonSocial.isNotBlank() &&
            isPlausibleEmail(email) &&
            password.length >= MIN_PASSWORD_LENGTH &&
            !loading &&
            !validating
}

internal fun isPlausibleEmail(email: String): Boolean {
    val e = email.trim()
    val at = e.indexOf('@')
    return at > 0 && at < e.length - 1 && !e.contains(' ')
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val coordinator: RegistrationCoordinator,
) : ViewModel() {

    private val local = MutableStateFlow(RegisterUiState())

    val uiState: StateFlow<RegisterUiState> = combine(local, coordinator.state) { form, registration ->
        form.copy(
            loading = registration.inFlight,
            error = registration.error ?: form.error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RegisterUiState())

    fun onRucChange(value: String) {
        if (local.value.rucValidated) return
        local.update {
            it.copy(ruc = value.filter { char -> char.isDigit() }.take(RUC_LENGTH), error = null)
        }
    }

    fun validateRuc() {
        val ruc = local.value.ruc
        if (!uiState.value.canValidate) return
        viewModelScope.launch {
            local.update { it.copy(validating = true, error = null) }
            tenantRepository.validateRucWithSunat(ruc)
                .onSuccess { validation ->
                    local.update {
                        it.copy(
                            validating = false,
                            rucValidated = true,
                            razonSocial = validation.razonSocial,
                            address = validation.direccion,
                            ubigeo = validation.ubigeo,
                        )
                    }
                }
                .onFailure { error ->
                    local.update {
                        it.copy(validating = false, error = error.message ?: "No se pudo validar el RUC")
                    }
                }
        }
    }

    fun clearRuc() {
        local.update {
            it.copy(ruc = "", razonSocial = "", rucValidated = false, address = "", ubigeo = "", error = null)
        }
    }

    fun onEmailChange(value: String) {
        coordinator.clearError()
        local.update { it.copy(email = value.trim(), error = null) }
    }

    fun onPasswordChange(value: String) {
        coordinator.clearError()
        local.update { it.copy(password = value, error = null) }
    }

    fun togglePasswordVisibility() {
        local.update { it.copy(showPassword = !it.showPassword) }
    }

    /**
     * Crea el restaurante y entra directo. La corrutina vive en [RegistrationCoordinator] (no aquí):
     * al vincular el restaurante la app cambia de pantalla raíz y este ViewModel se destruye.
     */
    fun submit() {
        val state = uiState.value
        if (!state.canSubmit) return
        coordinator.start(
            RestaurantRegistrationInput(
                name = state.razonSocial.trim(),
                razonSocial = state.razonSocial.trim(),
                ruc = state.ruc,
                email = state.email.trim(),
                phone = "",
                password = state.password,
                address = state.address,
                ubigeo = state.ubigeo,
            ),
        )
    }
}
