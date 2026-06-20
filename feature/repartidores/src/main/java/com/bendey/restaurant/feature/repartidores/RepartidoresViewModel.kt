package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.model.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepartidoresTabKind { DRIVERS, COMPANIES }

data class RepartidoresUiState(
    val tab: RepartidoresTabKind = RepartidoresTabKind.DRIVERS,
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val drivers: List<DeliveryDriver> = emptyList(),
    val companies: List<DeliveryCompany> = emptyList(),
    val driverFormOpen: Boolean = false,
    val companyFormOpen: Boolean = false,
    val editingDriverId: Int? = null,
    val editingCompanyId: Int? = null,
    val driverForm: DeliveryDriverFormInput = DeliveryDriverFormInput(),
    val companyForm: DeliveryCompanyFormInput = DeliveryCompanyFormInput(),
    val deleteDriverId: Int? = null,
    val deleteCompanyId: Int? = null,
    val error: String? = null,
)

@HiltViewModel
class RepartidoresViewModel @Inject constructor(
    private val repository: DeliveryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepartidoresUiState())
    val uiState: StateFlow<RepartidoresUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun selectTab(name: String) {
        val tab = RepartidoresTabKind.valueOf(name)
        _uiState.update { it.copy(tab = tab, error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val drivers = repository.listDrivers()
            val companies = repository.listCompanies()
            _uiState.update {
                it.copy(
                    loading = false,
                    drivers = (drivers as? AppResult.Success)?.data.orEmpty(),
                    companies = (companies as? AppResult.Success)?.data.orEmpty(),
                    error = (drivers as? AppResult.Error)?.message ?: (companies as? AppResult.Error)?.message,
                )
            }
        }
    }

    fun openCreate() {
        when (_uiState.value.tab) {
            RepartidoresTabKind.DRIVERS -> _uiState.update { it.copy(driverFormOpen = true, editingDriverId = null, driverForm = DeliveryDriverFormInput(), error = null) }
            RepartidoresTabKind.COMPANIES -> _uiState.update { it.copy(companyFormOpen = true, editingCompanyId = null, companyForm = DeliveryCompanyFormInput(), error = null) }
        }
    }

    fun openEditDriver(id: Int) {
        val driver = _uiState.value.drivers.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                driverFormOpen = true,
                editingDriverId = id,
                driverForm = DeliveryDriverFormInput(
                    name = driver.name,
                    phone = driver.phone,
                    vehicleType = driver.vehicleType,
                    plate = driver.plate,
                    notes = driver.notes,
                    deliveryCompanyId = driver.deliveryCompanyId,
                    active = driver.active,
                ),
                error = null,
            )
        }
    }

    fun openEditCompany(id: Int) {
        val company = _uiState.value.companies.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(companyFormOpen = true, editingCompanyId = id, companyForm = DeliveryCompanyFormInput(name = company.name), error = null) }
    }

    fun dismissDriverForm() { _uiState.update { it.copy(driverFormOpen = false, editingDriverId = null) } }
    fun dismissCompanyForm() { _uiState.update { it.copy(companyFormOpen = false, editingCompanyId = null) } }
    fun updateDriverForm(transform: (DeliveryDriverFormInput) -> DeliveryDriverFormInput) { _uiState.update { it.copy(driverForm = transform(it.driverForm)) } }
    fun updateCompanyForm(transform: (DeliveryCompanyFormInput) -> DeliveryCompanyFormInput) { _uiState.update { it.copy(companyForm = transform(it.companyForm)) } }

    fun saveDriver() {
        val state = _uiState.value
        if (state.driverForm.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (state.editingDriverId == null) repository.createDriver(state.driverForm) else repository.updateDriver(state.editingDriverId, state.driverForm)
            when (result) {
                is AppResult.Success -> { _uiState.update { it.copy(actionLoading = false, driverFormOpen = false, editingDriverId = null) }; refresh() }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun saveCompany() {
        val state = _uiState.value
        if (state.companyForm.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (state.editingCompanyId == null) {
                repository.createCompany(state.companyForm)
            } else {
                val company = state.companies.firstOrNull { it.id == state.editingCompanyId }
                repository.updateCompany(state.editingCompanyId, state.companyForm.name, company?.active ?: true)
            }
            when (result) {
                is AppResult.Success -> { _uiState.update { it.copy(actionLoading = false, companyFormOpen = false, editingCompanyId = null) }; refresh() }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteDriver(id: Int) { _uiState.update { it.copy(deleteDriverId = id) } }
    fun requestDeleteCompany(id: Int) { _uiState.update { it.copy(deleteCompanyId = id) } }
    fun dismissDeleteDriver() { _uiState.update { it.copy(deleteDriverId = null) } }
    fun dismissDeleteCompany() { _uiState.update { it.copy(deleteCompanyId = null) } }
    fun confirmDeleteDriver() {
        val id = _uiState.value.deleteDriverId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleteDriverId = null) }
            when (val result = repository.deleteDriver(id)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
    fun confirmDeleteCompany() {
        val id = _uiState.value.deleteCompanyId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleteCompanyId = null) }
            when (val result = repository.deleteCompany(id)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}
