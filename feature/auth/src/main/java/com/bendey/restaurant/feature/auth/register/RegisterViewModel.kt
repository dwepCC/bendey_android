package com.bendey.restaurant.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.auth.RestaurantRegistrationInput
import com.bendey.restaurant.core.domain.auth.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RUC_LENGTH = 11
private const val MIN_PASSWORD_LENGTH = 6

data class RegisterUiState(
    val ruc: String = "",
    val razonSocial: String = "",
    val restaurantName: String = "",
    val rucValidated: Boolean = false,
    val validating: Boolean = false,
    val address: String = "",
    val ubigeo: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registeredRestaurantName: String? = null,
) {
    val canValidate: Boolean
        get() = ruc.length == RUC_LENGTH && !validating && !loading && !rucValidated

    val canSubmit: Boolean
        get() = rucValidated &&
            restaurantName.isNotBlank() &&
            email.contains("@") &&
            password.length >= MIN_PASSWORD_LENGTH &&
            password == confirmPassword &&
            !loading &&
            !validating &&
            registeredRestaurantName == null
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onRucChange(value: String) {
        if (_uiState.value.rucValidated) return
        _uiState.update {
            it.copy(
                ruc = value.filter { char -> char.isDigit() }.take(RUC_LENGTH),
                error = null,
            )
        }
    }

    fun validateRuc() {
        val ruc = _uiState.value.ruc
        if (!_uiState.value.canValidate) return
        viewModelScope.launch {
            _uiState.update { it.copy(validating = true, error = null) }
            tenantRepository.validateRucWithSunat(ruc)
                .onSuccess { validation ->
                    _uiState.update {
                        it.copy(
                            validating = false,
                            rucValidated = true,
                            razonSocial = validation.razonSocial,
                            restaurantName = validation.razonSocial,
                            address = validation.direccion,
                            ubigeo = validation.ubigeo,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            validating = false,
                            error = error.message ?: "No se pudo validar el RUC",
                        )
                    }
                }
        }
    }

    fun clearRuc() {
        _uiState.update { current ->
            current.copy(
                ruc = "",
                razonSocial = "",
                restaurantName = "",
                rucValidated = false,
                address = "",
                ubigeo = "",
                error = null,
            )
        }
    }

    fun onRestaurantNameChange(value: String) {
        _uiState.update { it.copy(restaurantName = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value.trim(), error = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                phone = value.filter { char -> char.isDigit() || char == '+' || char == ' ' }.take(15),
                error = null,
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun submit(onSuccess: (restaurantName: String) -> Unit) {
        val state = _uiState.value
        if (!state.canSubmit) return
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            tenantRepository.registerRestaurant(
                RestaurantRegistrationInput(
                    name = state.restaurantName.trim(),
                    razonSocial = state.razonSocial.trim(),
                    ruc = state.ruc,
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    password = state.password,
                    address = state.address,
                    ubigeo = state.ubigeo,
                ),
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(loading = false, registeredRestaurantName = result.name)
                }
                onSuccess(result.name)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "No se pudo crear tu restaurante",
                    )
                }
            }
        }
    }
}
