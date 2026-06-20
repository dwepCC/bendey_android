package com.bendey.restaurant.feature.cocina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CocinaUiState(
    val loading: Boolean = false,
    val updatingId: Int? = null,
    val items: List<KitchenItem> = emptyList(),
    val error: String? = null,
) {
    fun count(status: ComandaStatus): Int = items.count { it.status == status }

    fun itemsFor(status: ComandaStatus): List<KitchenItem> =
        items.filter { it.status == status }
}

@HiltViewModel
class CocinaViewModel @Inject constructor(
    private val kitchenRepository: KitchenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CocinaUiState())
    val uiState: StateFlow<CocinaUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = kitchenRepository.loadKitchen()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(loading = false, items = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun advanceItem(item: KitchenItem) {
        val next = ComandaStatus.next(item.status.backendValue) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(updatingId = item.id, error = null) }
            when (val result = kitchenRepository.updateComandaStatus(item.id, next)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> _uiState.update {
                    it.copy(updatingId = null, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
