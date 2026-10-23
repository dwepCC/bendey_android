package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.ops.OpsControls
import com.bendey.restaurant.core.domain.ops.OpsControlsRules
import com.bendey.restaurant.core.domain.ops.OpsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de «Stock en el POS» en Ajustes > Operación. */
data class PosStockControlState(
    val controls: OpsControls? = null,
    val loading: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura. */
    val canEdit: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val enabled: Boolean get() = controls?.showPosStock == true
}

/** Reductor del interruptor «Mostrar stock en el POS» (apagado por defecto). Sin Android. */
class PosStockControlPresenter(
    private val scope: CoroutineScope,
    private val repository: OpsRepository,
) {
    private val _state = MutableStateFlow(PosStockControlState(controls = repository.peekOpsControls()))
    val state: StateFlow<PosStockControlState> = _state.asStateFlow()

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = OpsControlsRules.canEdit(permissions)) }
    }

    fun load(forceRefresh: Boolean = false) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            when (val r = repository.getOpsControls(forceRefresh)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, controls = r.data) }
                is AppResult.Error -> _state.update { it.copy(loading = false) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun toggle(value: Boolean) {
        val s = _state.value
        if (!s.canEdit || s.saving || value == s.enabled) return
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            when (val r = repository.setShowPosStock(value)) {
                is AppResult.Success -> _state.update { it.copy(saving = false, controls = r.data) }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
