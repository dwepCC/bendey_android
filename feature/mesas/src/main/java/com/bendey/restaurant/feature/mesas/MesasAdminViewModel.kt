package com.bendey.restaurant.feature.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FloorFormState(
    val id: Int? = null,
    val name: String = "",
    val sortOrder: String = "0",
)

data class TableFormState(
    val id: Int? = null,
    val floorId: Int? = null,
    val name: String = "",
    val capacity: String = "4",
)

data class MesasAdminUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val floors: List<Floor> = emptyList(),
    val tables: List<RestaurantTable> = emptyList(),
    val floorFilterId: Int? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val floorFormOpen: Boolean = false,
    val floorForm: FloorFormState = FloorFormState(),
    val tableFormOpen: Boolean = false,
    val tableForm: TableFormState = TableFormState(),
    val deleteTableId: Int? = null,
    val deleteFloorId: Int? = null,
) {
    val filteredTables: List<RestaurantTable>
        get() {
            val byFloor = floorFilterId?.let { id -> tables.filter { it.floorId == id } } ?: tables
            val term = searchQuery.trim().lowercase()
            return if (term.isBlank()) byFloor else byFloor.filter { it.name.lowercase().contains(term) }
        }
}

@HiltViewModel
class MesasAdminViewModel @Inject constructor(
    private val mesasRepository: MesasRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MesasAdminUiState())
    val uiState: StateFlow<MesasAdminUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val floorsResult = mesasRepository.loadFloors()
            val tablesResult = mesasRepository.loadTables(floorId = _uiState.value.floorFilterId)
            when {
                floorsResult is AppResult.Loading || tablesResult is AppResult.Loading -> Unit
                floorsResult is AppResult.Error -> {
                    _uiState.update { it.copy(loading = false, error = floorsResult.message) }
                }
                tablesResult is AppResult.Error -> {
                    _uiState.update { it.copy(loading = false, error = tablesResult.message) }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            floors = (floorsResult as AppResult.Success).data,
                            tables = (tablesResult as AppResult.Success).data,
                        )
                    }
                }
            }
        }
    }

    fun setFloorFilter(floorId: Int?) {
        _uiState.update { it.copy(floorFilterId = floorId) }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = mesasRepository.loadTables(floorId)) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, tables = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun openCreateFloor() {
        val nextOrder = _uiState.value.floors.size
        _uiState.update {
            it.copy(
                floorFormOpen = true,
                floorForm = FloorFormState(sortOrder = nextOrder.toString()),
                error = null,
            )
        }
    }

    fun openEditFloor(floor: Floor) {
        _uiState.update {
            it.copy(
                floorFormOpen = true,
                floorForm = FloorFormState(
                    id = floor.id,
                    name = floor.name,
                    sortOrder = floor.sortOrder.toString(),
                ),
                error = null,
            )
        }
    }

    fun dismissFloorForm() {
        _uiState.update { it.copy(floorFormOpen = false, floorForm = FloorFormState()) }
    }

    fun updateFloorForm(transform: (FloorFormState) -> FloorFormState) {
        _uiState.update { it.copy(floorForm = transform(it.floorForm)) }
    }

    fun saveFloor() {
        val form = _uiState.value.floorForm
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa el nombre del ambiente") }
            return
        }
        val sortOrder = form.sortOrder.toIntOrNull() ?: 0
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val result = if (form.id == null) {
                mesasRepository.createFloor(form.name.trim(), sortOrder)
            } else {
                mesasRepository.updateFloor(form.id, form.name.trim(), sortOrder)
            }
            when (result) {
                is AppResult.Success -> {
                    dismissFloorForm()
                    refresh()
                    _uiState.update { it.copy(saving = false) }
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteFloor(id: Int) {
        _uiState.update { it.copy(deleteFloorId = id) }
    }

    fun dismissDeleteFloor() {
        _uiState.update { it.copy(deleteFloorId = null) }
    }

    fun confirmDeleteFloor() {
        val id = _uiState.value.deleteFloorId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            when (val result = mesasRepository.deleteFloor(id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(deleteFloorId = null, saving = false) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun openCreateTable() {
        val defaultFloor = _uiState.value.floorFilterId ?: _uiState.value.floors.firstOrNull()?.id
        _uiState.update {
            it.copy(
                tableFormOpen = true,
                tableForm = TableFormState(floorId = defaultFloor),
                error = null,
            )
        }
    }

    fun openEditTable(table: RestaurantTable) {
        _uiState.update {
            it.copy(
                tableFormOpen = true,
                tableForm = TableFormState(
                    id = table.id,
                    floorId = table.floorId,
                    name = table.name,
                    capacity = table.capacity.toString(),
                ),
                error = null,
            )
        }
    }

    fun dismissTableForm() {
        _uiState.update { it.copy(tableFormOpen = false, tableForm = TableFormState()) }
    }

    fun updateTableForm(transform: (TableFormState) -> TableFormState) {
        _uiState.update { it.copy(tableForm = transform(it.tableForm)) }
    }

    fun saveTable() {
        val form = _uiState.value.tableForm
        val floorId = form.floorId
        val capacity = form.capacity.toIntOrNull() ?: 0
        if (floorId == null || form.name.isBlank() || capacity <= 0) {
            _uiState.update { it.copy(error = "Completa ambiente, nombre y capacidad") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val result = if (form.id == null) {
                mesasRepository.createTable(floorId, form.name.trim(), capacity)
            } else {
                mesasRepository.updateTable(form.id, floorId, form.name.trim(), capacity)
            }
            when (result) {
                is AppResult.Success -> {
                    dismissTableForm()
                    refresh()
                    _uiState.update { it.copy(saving = false) }
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteTable(id: Int) {
        _uiState.update { it.copy(deleteTableId = id) }
    }

    fun dismissDeleteTable() {
        _uiState.update { it.copy(deleteTableId = null) }
    }

    fun confirmDeleteTable() {
        val id = _uiState.value.deleteTableId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            when (val result = mesasRepository.deleteTable(id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(deleteTableId = null, saving = false) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun tableDeleteBlockedReason(table: RestaurantTable): String? = when {
        table.sessionId != null -> "Tiene un pedido abierto. Ciérralo antes de eliminar."
        table.status != TableStatus.LIBRE -> "La mesa está ${table.status.label.lowercase()}."
        else -> null
    }
}
