package com.bendey.restaurant.feature.auth.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.auth.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RUC_PERU_LENGTH = 11

data class WelcomeUiState(
    val ruc: String = "",
    val linking: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = ruc.length == RUC_PERU_LENGTH && !linking
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onRucChange(value: String) {
        _uiState.update {
            it.copy(
                ruc = value.filter { char -> char.isDigit() }.take(RUC_PERU_LENGTH),
                error = null,
            )
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val ruc = _uiState.value.ruc
        if (!_uiState.value.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(linking = true, error = null) }
            tenantRepository.resolveTenantByRuc(ruc)
                .onSuccess { binding ->
                    runCatching { tenantRepository.bindTenant(binding) }
                        .onSuccess {
                            _uiState.update { it.copy(linking = false) }
                            onSuccess()
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    linking = false,
                                    error = error.message ?: "No se pudo vincular el negocio",
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            linking = false,
                            error = error.message ?: "No encontramos un restaurante con ese RUC",
                        )
                    }
                }
        }
    }
}
