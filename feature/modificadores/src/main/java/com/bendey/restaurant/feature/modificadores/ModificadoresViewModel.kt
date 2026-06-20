package com.bendey.restaurant.feature.modificadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.ModifierGroupFormInput
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierOption
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.toFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModificadoresUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val groups: List<ModifierGroup> = emptyList(),
    val formOpen: Boolean = false,
    val editingId: Int? = null,
    val form: ModifierGroupFormInput = ModifierGroupFormInput(),
    val deleteId: Int? = null,
    val error: String? = null,
)

@HiltViewModel
class ModificadoresViewModel @Inject constructor(
    private val repository: ModifiersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModificadoresUiState())
    val uiState: StateFlow<ModificadoresUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.listModifierGroups()) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, groups = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreate() {
        _uiState.update {
            it.copy(formOpen = true, editingId = null, form = ModifierGroupFormInput(options = listOf(ModifierOption(name = ""))), error = null)
        }
    }

    fun openEdit(id: Int) {
        val group = _uiState.value.groups.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(formOpen = true, editingId = id, form = group.toFormInput(), error = null) }
    }

    fun dismissForm() {
        _uiState.update { it.copy(formOpen = false, editingId = null) }
    }

    fun updateForm(transform: (ModifierGroupFormInput) -> ModifierGroupFormInput) {
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
                repository.createModifierGroup(state.form)
            } else {
                repository.updateModifierGroup(state.editingId, state.form)
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

    fun requestDelete(id: Int) {
        _uiState.update { it.copy(deleteId = id) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleteId = null, actionLoading = true) }
            when (val result = repository.deleteModifierGroup(id)) {
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
