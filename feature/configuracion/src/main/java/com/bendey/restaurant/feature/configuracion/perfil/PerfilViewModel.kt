package com.bendey.restaurant.feature.configuracion.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.profile.ProfileFormInput
import com.bendey.restaurant.core.domain.profile.ProfileRepository
import com.bendey.restaurant.core.domain.profile.toFormInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val changingPassword: Boolean = false,
    val form: ProfileFormInput = ProfileFormInput(),
    val roleName: String = "",
    val branchName: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
    val passwordError: String? = null,
    val snackMessage: String? = null,
)

/** Perfil del propio usuario logueado — solo accesible con login completo (email/contraseña). */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = profileRepository.getMyProfile()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        form = result.data.toFormInput(),
                        roleName = result.data.roleName,
                        branchName = result.data.branchName,
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun updateForm(transform: (ProfileFormInput) -> ProfileFormInput) {
        _uiState.update { it.copy(form = transform(it.form), error = null) }
    }

    fun saveProfile() {
        val form = _uiState.value.form
        if (form.name.trim().isEmpty() || form.email.trim().isEmpty()) {
            _uiState.update { it.copy(error = "Nombre y email son obligatorios") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            when (val result = profileRepository.updateMyProfile(form)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(saving = false, form = result.data.toFormInput(), snackMessage = "Perfil actualizado")
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setCurrentPassword(value: String) {
        _uiState.update { it.copy(currentPassword = value, passwordError = null) }
    }

    fun setNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value, passwordError = null) }
    }

    fun setConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, passwordError = null) }
    }

    fun changePassword() {
        val state = _uiState.value
        if (state.currentPassword.isBlank() || state.newPassword.isBlank()) {
            _uiState.update { it.copy(passwordError = "Completa la contraseña actual y la nueva") }
            return
        }
        // Mismo mínimo que valida el backend — evitar el viaje redondo por un error obvio.
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(passwordError = "La nueva contraseña debe tener mínimo 8 caracteres") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(passwordError = "Las contraseñas nuevas no coinciden") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(changingPassword = true, passwordError = null) }
            when (
                val result = profileRepository.changeMyPassword(
                    currentPassword = state.currentPassword,
                    newPassword = state.newPassword,
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        changingPassword = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        snackMessage = "Contraseña actualizada",
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(changingPassword = false, passwordError = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}
