package com.bendey.restaurant.feature.areaspreparacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.PreparationAreaFormInput
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.catalog.PreparationAreasRepository
import com.bendey.restaurant.core.domain.catalog.toFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AreasPreparacionUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val areas: List<PreparationAreaItem> = emptyList(),
    val searchQuery: String = "",
    val showInactive: Boolean = false,
    val formOpen: Boolean = false,
    val editingId: Int? = null,
    val form: PreparationAreaFormInput = PreparationAreaFormInput(),
    val error: String? = null,
) {
    val filteredAreas: List<PreparationAreaItem>
        get() {
            val query = searchQuery.trim().lowercase()
            return areas.filter { area ->
                val matchesQuery = query.isBlank() ||
                    area.name.lowercase().contains(query) ||
                    area.description.lowercase().contains(query)
                val matchesActive = showInactive || area.active
                matchesQuery && matchesActive
            }
        }
}

@HiltViewModel
class AreasPreparacionViewModel @Inject constructor(
    private val repository: PreparationAreasRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AreasPreparacionUiState())
    val uiState: StateFlow<AreasPreparacionUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.listPreparationAreas(activeOnly = false)) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, areas = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun setShowInactive(show: Boolean) {
        _uiState.update { it.copy(showInactive = show) }
    }

    fun openCreate() {
        _uiState.update {
            it.copy(formOpen = true, editingId = null, form = PreparationAreaFormInput(), error = null)
        }
    }

    fun openEdit(id: Int) {
        val area = _uiState.value.areas.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(formOpen = true, editingId = id, form = area.toFormInput(), error = null) }
    }

    fun dismissForm() {
        _uiState.update { it.copy(formOpen = false, editingId = null) }
    }

    fun updateForm(transform: (PreparationAreaFormInput) -> PreparationAreaFormInput) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun save() {
        val state = _uiState.value
        if (state.form.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (state.editingId == null) {
                repository.createPreparationArea(state.form).let { created ->
                    when (created) {
                        is AppResult.Success -> AppResult.Success(Unit)
                        is AppResult.Error -> created
                        AppResult.Loading -> AppResult.Loading
                    }
                }
            } else {
                repository.updatePreparationArea(state.editingId, state.form)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, formOpen = false, editingId = null) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun toggleStatus(id: Int, active: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = repository.setPreparationAreaStatus(id, active)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}
