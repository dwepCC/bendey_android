package com.bendey.restaurant.feature.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.catalog.BranchFormInput
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.CompanyConfig
import com.bendey.restaurant.core.domain.catalog.CompanyConfigFormInput
import com.bendey.restaurant.core.domain.catalog.UbiItem
import com.bendey.restaurant.core.domain.catalog.RestaurantEmployeeType
import com.bendey.restaurant.core.domain.catalog.RestaurantSettings
import com.bendey.restaurant.core.domain.catalog.RestaurantStaffManagementRow
import com.bendey.restaurant.core.domain.catalog.StaffCreateFormInput
import com.bendey.restaurant.core.domain.catalog.StaffEditFormInput
import com.bendey.restaurant.core.domain.catalog.SeriesFormInput
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.catalog.SunatConfig
import com.bendey.restaurant.core.domain.catalog.SunatConfigFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.subscription.BILLING_MODULE_KEY
import com.bendey.restaurant.core.domain.subscription.hasModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConfigTab(val label: String) {
    GENERAL("General"),
    OPERACION("Operación"),
    BRANCHES("Sucursales"),
    SERIES("Series"),
    MENU_DIGITAL("Menú Digital"),
}

data class ConfiguracionUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val actionLoading: Boolean = false,
    val tab: ConfigTab = ConfigTab.GENERAL,
    val config: CompanyConfig? = null,
    val sunat: SunatConfig? = null,
    val settings: RestaurantSettings? = null,
    val branches: List<BranchItem> = emptyList(),
    val series: List<DocumentSeries> = emptyList(),
    val selectedBranchId: Int? = null,
    val configFormOpen: Boolean = false,
    val configForm: CompanyConfigFormInput = CompanyConfigFormInput(),
    // Ubigeo en cascada (Departamento → Provincia → Distrito). El distrito seleccionado
    // es configForm.ubigeo; region/provincia se guardan aparte para la carga en cascada.
    val ubigeoRegiones: List<UbiItem> = emptyList(),
    val ubigeoProvincias: List<UbiItem> = emptyList(),
    val ubigeoDistritos: List<UbiItem> = emptyList(),
    val ubigeoRegionId: String = "",
    val ubigeoProvinciaId: String = "",
    val sunatFormOpen: Boolean = false,
    val sunatForm: SunatConfigFormInput = SunatConfigFormInput(),
    val pinDialogOpen: Boolean = false,
    val pinValue: String = "",
    val branchFormOpen: Boolean = false,
    val branchForm: BranchFormInput = BranchFormInput(),
    val deleteBranchId: Int? = null,
    val seriesFormOpen: Boolean = false,
    val seriesForm: SeriesFormInput = SeriesFormInput(),
    val deleteSeriesId: Int? = null,
    val staffRows: List<RestaurantStaffManagementRow> = emptyList(),
    val staffLoading: Boolean = false,
    val staffCreateOpen: Boolean = false,
    val staffEditOpen: Boolean = false,
    val staffCreateForm: StaffCreateFormInput = StaffCreateFormInput(),
    val staffEditForm: StaffEditFormInput = StaffEditFormInput(),
    val canManageRestaurantSettings: Boolean = false,
    val billingModuleEnabled: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init {
        applyCachedSettings()
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { user ->
                val perms = user?.restaurantPermissions.orEmpty()
                _uiState.update {
                    it.copy(
                        canManageRestaurantSettings = RestaurantPermissions.canManageRestaurantSettings(perms),
                        billingModuleEnabled = hasModule(user?.modules.orEmpty(), BILLING_MODULE_KEY),
                    )
                }
            }
        }
        refresh(forceNetwork = false)
    }

    private fun applyCachedSettings() {
        repository.peekTenantSettings()?.let { cached ->
            _uiState.update {
                it.copy(
                    loading = false,
                    config = cached.config,
                    sunat = cached.sunat,
                    settings = cached.settings,
                    branches = cached.branches,
                    selectedBranchId = it.selectedBranchId ?: cached.branches.firstOrNull()?.id,
                )
            }
        }
    }

    private fun requireManageSettings(): Boolean {
        if (_uiState.value.canManageRestaurantSettings) return true
        _uiState.update { it.copy(error = "No tiene permiso para modificar la configuración del restaurante") }
        return false
    }

    fun setTab(tab: ConfigTab) {
        _uiState.update { it.copy(tab = tab, error = null) }
        when (tab) {
            ConfigTab.SERIES -> loadSeries()
            ConfigTab.OPERACION -> loadStaffManagement()
            else -> Unit
        }
    }

    fun refresh(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            if (!forceNetwork) {
                repository.peekTenantSettings()?.let { cached ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            config = cached.config,
                            sunat = cached.sunat,
                            settings = cached.settings,
                            branches = cached.branches,
                            selectedBranchId = it.selectedBranchId ?: cached.branches.firstOrNull()?.id,
                            error = null,
                        )
                    }
                    return@launch
                }
            }
            val hasData = _uiState.value.config != null
            if (forceNetwork) repository.invalidateTenantSettingsCache()
            _uiState.update {
                it.copy(
                    loading = !hasData,
                    refreshing = hasData && forceNetwork,
                    error = null,
                )
            }
            coroutineScope {
                val config = async { repository.getCompanyConfig() }
                val sunat = async { repository.getSunatConfig() }
                val settings = async { repository.getRestaurantSettings() }
                val branches = async { repository.listBranches() }
                val configResult = config.await()
                val sunatResult = sunat.await()
                val settingsResult = settings.await()
                val branchesResult = branches.await()
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        config = (configResult as? AppResult.Success)?.data ?: it.config,
                        sunat = (sunatResult as? AppResult.Success)?.data ?: it.sunat,
                        settings = (settingsResult as? AppResult.Success)?.data ?: it.settings,
                        branches = (branchesResult as? AppResult.Success)?.data ?: it.branches,
                        selectedBranchId = it.selectedBranchId
                            ?: (branchesResult as? AppResult.Success)?.data?.firstOrNull()?.id,
                        error = listOfNotNull(
                            (configResult as? AppResult.Error)?.message,
                            (sunatResult as? AppResult.Error)?.message,
                            (settingsResult as? AppResult.Error)?.message,
                            (branchesResult as? AppResult.Error)?.message,
                        ).firstOrNull(),
                    )
                }
            }
            if (_uiState.value.tab == ConfigTab.SERIES) loadSeries()
        }
    }

    fun selectBranch(branchId: Int) {
        _uiState.update { it.copy(selectedBranchId = branchId) }
        loadSeries()
    }

    private fun loadSeries() {
        viewModelScope.launch {
            val branchId = _uiState.value.selectedBranchId
            when (val result = repository.listSeries(branchId)) {
                is AppResult.Success -> _uiState.update { it.copy(series = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openEditConfig() {
        if (!requireManageSettings()) return
        val config = _uiState.value.config ?: return
        val code = config.ubigeo.filter { it.isDigit() }
        val regionId = if (code.length >= 2) code.take(2) + "0000" else ""
        val provinciaId = if (code.length >= 4) code.take(4) + "00" else ""
        _uiState.update {
            it.copy(
                configFormOpen = true,
                configForm = CompanyConfigFormInput(
                    tradeName = config.tradeName,
                    address = config.address,
                    ubigeo = if (code.length == 6) code else "",
                    phone = config.phone,
                    email = config.email,
                ),
                ubigeoRegionId = regionId,
                ubigeoProvinciaId = provinciaId,
                ubigeoRegiones = emptyList(),
                ubigeoProvincias = emptyList(),
                ubigeoDistritos = emptyList(),
                error = null,
            )
        }
        loadUbigeoForEdit(regionId, provinciaId)
    }

    private fun loadUbigeoForEdit(regionId: String, provinciaId: String) {
        viewModelScope.launch {
            val reg = repository.getUbigeoRegiones()
            if (reg is AppResult.Success) _uiState.update { it.copy(ubigeoRegiones = reg.data) }
            if (regionId.isNotBlank()) {
                val prov = repository.getUbigeoProvincias(regionId)
                if (prov is AppResult.Success) _uiState.update { it.copy(ubigeoProvincias = prov.data) }
            }
            if (provinciaId.isNotBlank()) {
                val dist = repository.getUbigeoDistritos(provinciaId)
                if (dist is AppResult.Success) _uiState.update { it.copy(ubigeoDistritos = dist.data) }
            }
        }
    }

    fun onUbigeoRegion(regionId: String) {
        _uiState.update {
            it.copy(
                ubigeoRegionId = regionId,
                ubigeoProvinciaId = "",
                ubigeoProvincias = emptyList(),
                ubigeoDistritos = emptyList(),
                configForm = it.configForm.copy(ubigeo = ""),
            )
        }
        if (regionId.isNotBlank()) viewModelScope.launch {
            val prov = repository.getUbigeoProvincias(regionId)
            if (prov is AppResult.Success) _uiState.update { it.copy(ubigeoProvincias = prov.data) }
        }
    }

    fun onUbigeoProvincia(provinciaId: String) {
        _uiState.update {
            it.copy(
                ubigeoProvinciaId = provinciaId,
                ubigeoDistritos = emptyList(),
                configForm = it.configForm.copy(ubigeo = ""),
            )
        }
        if (provinciaId.isNotBlank()) viewModelScope.launch {
            val dist = repository.getUbigeoDistritos(provinciaId)
            if (dist is AppResult.Success) _uiState.update { it.copy(ubigeoDistritos = dist.data) }
        }
    }

    fun onUbigeoDistrito(distritoId: String) {
        _uiState.update { it.copy(configForm = it.configForm.copy(ubigeo = distritoId)) }
    }

    fun dismissEditConfig() { _uiState.update { it.copy(configFormOpen = false) } }
    fun updateConfigForm(transform: (CompanyConfigFormInput) -> CompanyConfigFormInput) {
        _uiState.update { it.copy(configForm = transform(it.configForm)) }
    }

    fun saveConfig() {
        if (!requireManageSettings()) return
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
        if (!requireManageSettings()) return
        val sunat = _uiState.value.sunat ?: return
        _uiState.update {
            it.copy(
                sunatFormOpen = true,
                sunatForm = SunatConfigFormInput(
                    taxRate = normalizeIgvRateOption(sunat.taxRate),
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
        if (!requireManageSettings()) return
        val rateOption = _uiState.value.sunatForm.taxRate
        if (rateOption !in IGV_RATE_OPTIONS) {
            _uiState.update { it.copy(error = "Selecciona una tasa de IGV válida") }
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

    fun openPinDialog() {
        if (!requireManageSettings()) return
        _uiState.update { it.copy(pinDialogOpen = true, pinValue = "", error = null) }
    }
    fun dismissPinDialog() { _uiState.update { it.copy(pinDialogOpen = false, pinValue = "") } }
    fun setPinValue(value: String) { _uiState.update { it.copy(pinValue = value) } }

    fun savePin() {
        if (!requireManageSettings()) return
        val pin = _uiState.value.pinValue.filter { it.isDigit() }
        if (pin.length !in 4..6) {
            _uiState.update { it.copy(error = "PIN de operaciones: 4 a 6 dígitos") }
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

    fun openCreateBranch() {
        if (!requireManageSettings()) return
        _uiState.update { it.copy(branchFormOpen = true, branchForm = BranchFormInput(), error = null) }
    }

    fun openEditBranch(branch: BranchItem) {
        if (!requireManageSettings()) return
        _uiState.update {
            it.copy(
                branchFormOpen = true,
                branchForm = BranchFormInput(
                    id = branch.id,
                    name = branch.name,
                    address = branch.address,
                    phone = branch.phone,
                    fiscalDomicileCode = branch.fiscalDomicileCode,
                    isMain = branch.isMain,
                    active = branch.active,
                ),
                error = null,
            )
        }
    }

    fun dismissBranchForm() { _uiState.update { it.copy(branchFormOpen = false) } }
    fun updateBranchForm(transform: (BranchFormInput) -> BranchFormInput) {
        _uiState.update { it.copy(branchForm = transform(it.branchForm)) }
    }

    fun saveBranch() {
        if (!requireManageSettings()) return
        val form = _uiState.value.branchForm
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa el nombre de la sucursal") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val branchId = form.id
            val result = if (branchId == null) repository.createBranch(form) else repository.updateBranch(branchId, form)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, branchFormOpen = false) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteBranch(id: Int) {
        if (!requireManageSettings()) return
        val branch = _uiState.value.branches.find { it.id == id } ?: return
        if (branch.isMain) {
            _uiState.update { it.copy(error = "No se puede eliminar la sucursal principal") }
            return
        }
        _uiState.update { it.copy(deleteBranchId = id) }
    }
    fun dismissDeleteBranch() { _uiState.update { it.copy(deleteBranchId = null) } }

    fun confirmDeleteBranch() {
        val id = _uiState.value.deleteBranchId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.deleteBranch(id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, deleteBranchId = null) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreateSeries() {
        if (!requireManageSettings()) return
        val billingModuleEnabled = _uiState.value.billingModuleEnabled
        _uiState.update {
            it.copy(
                seriesFormOpen = true,
                seriesForm = SeriesFormInput(
                    branchId = it.selectedBranchId,
                    sunatCode = if (billingModuleEnabled) "00" else "00",
                ),
                error = null,
            )
        }
    }

    fun openEditSeries(series: DocumentSeries) {
        if (!requireManageSettings()) return
        val billingModuleEnabled = _uiState.value.billingModuleEnabled
        val code = series.sunatCode.orEmpty()
        if (!billingModuleEnabled && code.isNotBlank() && code != "00") {
            _uiState.update {
                it.copy(error = "Tu plan no incluye facturación electrónica: solo puedes editar series de nota de venta")
            }
            return
        }
        _uiState.update {
            it.copy(
                seriesFormOpen = true,
                seriesForm = SeriesFormInput(
                    id = series.id,
                    branchId = series.branchId,
                    docType = series.docType,
                    series = series.series,
                    category = series.category,
                    sunatCode = if (billingModuleEnabled) code.ifBlank { "00" } else "00",
                    active = series.active,
                    currentNumber = series.currentNumber,
                    locked = series.locked,
                    canDelete = series.canDelete,
                ),
                error = null,
            )
        }
    }

    fun dismissSeriesForm() { _uiState.update { it.copy(seriesFormOpen = false) } }
    fun updateSeriesForm(transform: (SeriesFormInput) -> SeriesFormInput) {
        _uiState.update { it.copy(seriesForm = transform(it.seriesForm)) }
    }

    fun saveSeries() {
        if (!requireManageSettings()) return
        val form = _uiState.value.seriesForm
        val billingModuleEnabled = _uiState.value.billingModuleEnabled
        val sunatCode = if (billingModuleEnabled) form.sunatCode.trim().ifBlank { "00" } else "00"
        if (!billingModuleEnabled && sunatCode != "00") {
            _uiState.update { it.copy(error = "Tu plan no incluye facturación electrónica: solo se permiten series de nota de venta") }
            return
        }
        if (form.series.isBlank() || form.branchId == null) {
            _uiState.update { it.copy(error = "Completa sucursal y serie") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val seriesId = form.id
            val payload = form.copy(sunatCode = sunatCode)
            val result = if (seriesId == null) repository.createSeries(payload) else repository.updateSeries(seriesId, payload)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, seriesFormOpen = false) }
                    loadSeries()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteSeries(id: Int) {
        if (!requireManageSettings()) return
        val series = _uiState.value.series.find { it.id == id } ?: return
        if (!series.canDelete) {
            _uiState.update { it.copy(error = "No se puede eliminar: la serie ya tiene documentos emitidos") }
            return
        }
        _uiState.update { it.copy(deleteSeriesId = id) }
    }
    fun dismissDeleteSeries() { _uiState.update { it.copy(deleteSeriesId = null) } }

    fun confirmDeleteSeries() {
        val id = _uiState.value.deleteSeriesId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.deleteSeries(id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, deleteSeriesId = null) }
                    loadSeries()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadStaffManagement() {
        viewModelScope.launch {
            _uiState.update { it.copy(staffLoading = true, error = null) }
            when (val result = repository.listStaffManagement()) {
                is AppResult.Success -> _uiState.update { it.copy(staffLoading = false, staffRows = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(staffLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreateStaff() {
        if (!requireManageSettings()) return
        val branches = _uiState.value.branches.filter { it.active }
        val defaultBranch = branches.firstOrNull { it.isMain } ?: branches.firstOrNull()
        _uiState.update {
            it.copy(
                staffCreateOpen = true,
                staffCreateForm = StaffCreateFormInput(
                    branchIds = defaultBranch?.let { b -> listOf(b.id) }.orEmpty(),
                ),
                error = null,
            )
        }
    }

    fun dismissCreateStaff() { _uiState.update { it.copy(staffCreateOpen = false) } }

    fun updateStaffCreateForm(transform: (StaffCreateFormInput) -> StaffCreateFormInput) {
        _uiState.update { it.copy(staffCreateForm = transform(it.staffCreateForm)) }
    }

    fun toggleStaffCreateBranch(branchId: Int) {
        _uiState.update { state ->
            val ids = state.staffCreateForm.branchIds.toMutableList()
            if (ids.contains(branchId)) {
                if (ids.size <= 1) return@update state
                ids.remove(branchId)
            } else {
                ids.add(branchId)
            }
            state.copy(staffCreateForm = state.staffCreateForm.copy(branchIds = ids))
        }
    }

    fun confirmCreateStaff() {
        if (!requireManageSettings()) return
        val form = _uiState.value.staffCreateForm
        val pin = form.pin.filter { it.isDigit() }
        when {
            form.name.isBlank() -> _uiState.update { it.copy(error = "Nombre requerido") }
            form.email.isBlank() -> _uiState.update { it.copy(error = "Email requerido") }
            pin.length !in 4..6 -> _uiState.update { it.copy(error = "PIN de acceso: 4 a 6 dígitos") }
            form.branchIds.isEmpty() -> _uiState.update { it.copy(error = "Seleccione al menos una sucursal") }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(actionLoading = true, error = null) }
                when (val result = repository.createStaffUser(form.copy(pin = pin))) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(actionLoading = false, staffCreateOpen = false) }
                        loadStaffManagement()
                    }
                    is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                    AppResult.Loading -> Unit
                }
            }
        }
    }

    fun openEditStaff(row: RestaurantStaffManagementRow) {
        if (!requireManageSettings()) return
        _uiState.update {
            it.copy(
                staffEditOpen = true,
                staffEditForm = StaffEditFormInput(
                    userId = row.userId,
                    name = row.name,
                    email = row.email,
                    employeeType = row.employeeType.ifBlank { RestaurantEmployeeType.NONE.apiValue },
                    branchIds = row.branchIds,
                    hasPin = row.hasPin,
                ),
                error = null,
            )
        }
    }

    fun dismissEditStaff() { _uiState.update { it.copy(staffEditOpen = false) } }

    fun updateStaffEditForm(transform: (StaffEditFormInput) -> StaffEditFormInput) {
        _uiState.update { it.copy(staffEditForm = transform(it.staffEditForm)) }
    }

    fun toggleStaffEditBranch(branchId: Int) {
        _uiState.update { state ->
            val ids = state.staffEditForm.branchIds.toMutableList()
            if (ids.contains(branchId)) {
                if (ids.size <= 1) return@update state
                ids.remove(branchId)
            } else {
                ids.add(branchId)
            }
            state.copy(staffEditForm = state.staffEditForm.copy(branchIds = ids))
        }
    }

    fun confirmEditStaff() {
        if (!requireManageSettings()) return
        val form = _uiState.value.staffEditForm
        val pin = form.pin.filter { it.isDigit() }
        if (pin.isNotEmpty() && pin.length !in 4..6) {
            _uiState.update { it.copy(error = "PIN de acceso: 4 a 6 dígitos") }
            return
        }
        if (form.branchIds.isEmpty() && form.employeeType.isNotBlank()) {
            _uiState.update { it.copy(error = "Seleccione al menos una sucursal") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.updateStaffUser(form.copy(pin = pin))) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, staffEditOpen = false) }
                    loadStaffManagement()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}

private val IGV_RATE_OPTIONS = setOf("18", "10.5")

private fun normalizeIgvRateOption(rate: Double): String =
    if (rate in 10.0..11.0) "10.5" else "18"
