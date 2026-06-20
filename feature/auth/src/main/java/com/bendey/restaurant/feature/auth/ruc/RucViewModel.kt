package com.bendey.restaurant.feature.auth.ruc

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

private const val RUC_MIN_LENGTH = 8
private const val RUC_PERU_LENGTH = 11

data class RucUiState(
    val ruc: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val bound: Boolean = false,
) {
    val canSubmit: Boolean get() = ruc.length >= RUC_MIN_LENGTH && !loading && !bound
}

@HiltViewModel
class RucViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RucUiState())
    val uiState: StateFlow<RucUiState> = _uiState.asStateFlow()

    fun onRucChange(value: String) {
        _uiState.update {
            it.copy(
                ruc = value.filter { c -> c.isDigit() }.take(RUC_PERU_LENGTH),
                error = null,
            )
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val ruc = _uiState.value.ruc
        if (!_uiState.value.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            tenantRepository.resolveTenantByRuc(ruc)
                .onSuccess { binding ->
                    runCatching { tenantRepository.bindTenant(binding) }
                        .onSuccess {
                            _uiState.update { it.copy(loading = false, bound = true) }
                            onSuccess()
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(loading = false, error = e.message ?: "No se pudo vincular el negocio")
                            }
                        }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "RUC no encontrado")
                    }
                }
        }
    }
}
