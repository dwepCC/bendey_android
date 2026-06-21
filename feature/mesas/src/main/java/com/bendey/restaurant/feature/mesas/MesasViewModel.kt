package com.bendey.restaurant.feature.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.StaffOption
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.domain.restaurant.toTableStats
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.restaurant.sortRestaurantTables
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenTableForm(
    val guestsText: String = "2",
    val notes: String = "",
    val staffId: Int? = null,
)

data class FloorTableSection(
    val floorId: Int?,
    val floorName: String,
    val tables: List<RestaurantTable>,
)

data class MesasUiState(
    val loading: Boolean = false,
    val opening: Boolean = false,
    val floors: List<Floor> = emptyList(),
    val tables: List<RestaurantTable> = emptyList(),
    val staff: List<StaffOption> = emptyList(),
    val selectedFloorId: Int? = null,
    val searchQuery: String = "",
    val openTableTarget: RestaurantTable? = null,
    val openForm: OpenTableForm = OpenTableForm(),
    val error: String? = null,
    val snackMessage: String? = null,
    val openSessionTarget: Int? = null,
    val canAssignStaff: Boolean = false,
    val currentUserName: String = "",
    val currentUserStaffId: Int? = null,
) {
    private val sortedTables: List<RestaurantTable>
        get() = sortRestaurantTables(tables, floors)

    val filteredTables: List<RestaurantTable>
        get() {
            val term = searchQuery.trim().lowercase()
            val base = if (selectedFloorId == null) sortedTables
            else sortedTables.filter { it.floorId == selectedFloorId }
            return if (term.isBlank()) base
            else base.filter { it.name.lowercase().contains(term) }
        }

    val stats get() = filteredTables.toTableStats()

    val floorSections: List<FloorTableSection>
        get() {
            val list = filteredTables
            if (list.isEmpty()) return emptyList()
            if (selectedFloorId != null) {
                val name = floors.find { it.id == selectedFloorId }?.name ?: "Sala"
                return listOf(FloorTableSection(selectedFloorId, name, list))
            }
            val sections = floors.mapNotNull { floor ->
                val sectionTables = list.filter { it.floorId == floor.id }
                if (sectionTables.isEmpty()) null
                else FloorTableSection(floor.id, floor.name, sectionTables)
            }
            if (sections.isNotEmpty()) return sections
            return listOf(FloorTableSection(null, "Mesas", list))
        }
}

@HiltViewModel
class MesasViewModel @Inject constructor(
    private val mesasRepository: MesasRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MesasUiState())
    val uiState: StateFlow<MesasUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                val perms = session?.restaurantPermissions.orEmpty()
                val canAssign = RestaurantPermissions.canAssignTableStaff(perms)
                _uiState.update {
                    it.copy(
                        canAssignStaff = canAssign,
                        currentUserName = session?.user?.name.orEmpty(),
                        currentUserStaffId = session?.user?.staffId,
                    )
                }
                if (canAssign) loadStaffOptions(canAssign = true)
            }
        }
    }

    private fun loadStaffOptions(canAssign: Boolean) {
        viewModelScope.launch {
            if (!canAssign) {
                _uiState.update { it.copy(staff = emptyList()) }
                return@launch
            }
            when (val result = settingsRepository.listStaffManagement()) {
                is AppResult.Success -> {
                    val waiters = result.data.mapNotNull { row ->
                        val staffId = row.staffId ?: return@mapNotNull null
                        if (!row.staffActive || row.employeeType !in WAITER_TYPES) return@mapNotNull null
                        StaffOption(
                            id = staffId,
                            displayName = row.displayName.ifBlank { row.name }.ifBlank { row.staffCode },
                            employeeType = row.employeeType,
                        )
                    }.distinctBy { it.id }
                    _uiState.update { it.copy(staff = waiters) }
                }
                else -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val floorsResult = mesasRepository.loadFloors()) {
                is AppResult.Error -> {
                    _uiState.update { it.copy(loading = false, error = floorsResult.message) }
                    return@launch
                }
                is AppResult.Success -> _uiState.update { it.copy(floors = floorsResult.data) }
                AppResult.Loading -> Unit
            }
            when (val tablesResult = mesasRepository.loadTables(floorId = null)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(loading = false, tables = tablesResult.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = tablesResult.message)
                }
                AppResult.Loading -> Unit
            }
            if (_uiState.value.canAssignStaff) {
                loadStaffOptions(canAssign = true)
            }
        }
    }

    fun selectFloor(floorId: Int?) {
        _uiState.update { it.copy(selectedFloorId = floorId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTableClick(table: RestaurantTable) {
        when (table.status) {
            TableStatus.LIBRE -> {
                val state = _uiState.value
                val defaultStaffId = when {
                    state.canAssignStaff -> null
                    else -> state.currentUserStaffId
                }
                _uiState.update {
                    it.copy(
                        openTableTarget = table,
                        openForm = OpenTableForm(staffId = defaultStaffId),
                    )
                }
                if (state.canAssignStaff && state.staff.isEmpty()) {
                    loadStaffOptions(canAssign = true)
                }
            }
            TableStatus.OCUPADA, TableStatus.EN_CONSUMO -> {
                table.sessionId?.let { sessionId ->
                    _uiState.update { it.copy(openSessionTarget = sessionId) }
                }
            }
            TableStatus.RESERVADA -> Unit
        }
    }

    fun dismissOpenDialog() {
        _uiState.update { it.copy(openTableTarget = null) }
    }

    fun updateOpenForm(transform: (OpenTableForm) -> OpenTableForm) {
        _uiState.update { it.copy(openForm = transform(it.openForm)) }
    }

    fun confirmOpenTable() {
        val state = _uiState.value
        val table = state.openTableTarget ?: return
        val guests = state.openForm.guestsText.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1)
        if (guests == null) {
            _uiState.update { it.copy(error = "Ingrese un número de comensales válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(opening = true, error = null) }
            when (
                val result = mesasRepository.openTableSession(
                    tableId = table.id,
                    guests = guests,
                    notes = state.openForm.notes,
                    staffId = state.openForm.staffId,
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            opening = false,
                            openTableTarget = null,
                            openSessionTarget = result.data.sessionId,
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(opening = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeOpenSessionTarget() {
        _uiState.update { it.copy(openSessionTarget = null) }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    companion object {
        private val WAITER_TYPES = setOf("waiter", "mozo", "cashier", "admin", "supervisor")
    }
}
