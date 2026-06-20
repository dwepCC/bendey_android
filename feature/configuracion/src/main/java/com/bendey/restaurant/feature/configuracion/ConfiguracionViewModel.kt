package com.bendey.restaurant.feature.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.CompanyConfig
import com.bendey.restaurant.core.domain.catalog.CompanyConfigFormInput
import com.bendey.restaurant.core.domain.catalog.RestaurantSettings
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.catalog.SunatConfig
import com.bendey.restaurant.core.domain.catalog.SunatConfigFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfiguracionUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val config: CompanyConfig? = null,
    val sunat: SunatConfig? = null,
    val settings: RestaurantSettings? = null,
    val branches: List<BranchItem> = emptyList(),
    val configFormOpen: Boolean = false,
    val configForm: CompanyConfigFormInput = CompanyConfigFormInput(),
    val sunatFormOpen: Boolean = false,
    val sunatForm: SunatConfigFormInput = SunatConfigFormInput(),
    val pinDialogOpen: Boolean = false,
    val pinValue: String = "",
    val error: String? = null,
)

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val config = repository.getCompanyConfig()
            val sunat = repository.getSunatConfig()
            val settings = repository.getRestaurantSettings()
            val branches = repository.listBranches()
            _uiState.update {
                it.copy(
                    loading = false,
                    config = (config as? AppResult.Success)?.data,
                    sunat = (sunat as? AppResult.Success)?.data,
                    settings = (settings as? AppResult.Success)?.data,
                    branches = (branches as? AppResult.Success)?.data.orEmpty(),
                    error = listOfNotNull(
                        (config as? AppResult.Error)?.message,
                        (sunat as? AppResult.Error)?.message,
                        (settings as? AppResult.Error)?.message,
                        (branches as? AppResult.Error)?.message,
                    ).firstOrNull(),
                )
            }
        }
    }

    fun openEditConfig() {
        val config = _uiState.value.config ?: return
        _uiState.update {
            it.copy(
                configFormOpen = true,
                configForm = CompanyConfigFormInput(
                    businessName = config.businessName,
                    tradeName = config.tradeName,
                    address = config.address,
                    phone = config.phone,
                    email = config.email,
                ),
                error = null,
            )
        }
    }

    fun dismissEditConfig() { _uiState.update { it.copy(configFormOpen = false) } }
    fun updateConfigForm(transform: (CompanyConfigFormInput) -> CompanyConfigFormInput) {
        _uiState.update { it.copy(configForm = transform(it.configForm)) }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.updateCompanyConfig(_uiState.value.configForm)) {
                is AppResult.Success -> _uiState.update { it.copy(actionLoading = false, configFormOpen = false, config = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openEditSunat() {
        val sunat = _uiState.value.sunat ?: return
        _uiState.update {
            it.copy(
                sunatFormOpen = true,
                sunatForm = SunatConfigFormInput(
                    taxRate = sunat.taxRate.toString(),
                    igvRegime = sunat.igvRegime,
                    taxBenefitZone = sunat.taxBenefitZone,
                ),
                error = null,
            )
        }
    }

    fun dismissEditSunat() { _uiState.update { it.copy(sunatFormOpen = false) } }
    fun updateSunatForm(transform: (SunatConfigFormInput) -> SunatConfigFormInput) {
        _uiState.update { it.copy(sunatForm = transform(it.sunatForm)) }
    }

    fun saveSunat() {
        val rate = _uiState.value.sunatForm.taxRate.replace(",", ".").toDoubleOrNull()
        if (rate == null || rate < 0) {
            _uiState.update { it.copy(error = "Tasa de IGV inválida") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.updateSunatConfig(_uiState.value.sunatForm)) {
                is AppResult.Success -> _uiState.update { it.copy(actionLoading = false, sunatFormOpen = false, sunat = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openPinDialog() { _uiState.update { it.copy(pinDialogOpen = true, pinValue = "", error = null) } }
    fun dismissPinDialog() { _uiState.update { it.copy(pinDialogOpen = false, pinValue = "") } }
    fun setPinValue(value: String) { _uiState.update { it.copy(pinValue = value) } }

    fun savePin() {
        val pin = _uiState.value.pinValue.trim()
        if (pin.length < 4) {
            _uiState.update { it.copy(error = "El PIN debe tener al menos 4 caracteres") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.updateDeletionPin(pin)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, pinDialogOpen = false, pinValue = "") }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}
