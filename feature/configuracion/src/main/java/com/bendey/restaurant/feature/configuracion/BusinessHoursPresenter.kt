package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.ops.BusinessHours
import com.bendey.restaurant.core.domain.ops.BusinessHoursRules
import com.bendey.restaurant.core.domain.ops.BusinessHoursStatus
import com.bendey.restaurant.core.domain.ops.OpsControlsRules
import com.bendey.restaurant.core.domain.ops.OpsRepository
import com.bendey.restaurant.core.domain.ops.SlotDraft
import com.bendey.restaurant.core.domain.ops.WeekDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del editor semanal del horario de atención. */
data class BusinessHoursState(
    val saved: BusinessHours? = null,
    val week: WeekDraft = BusinessHoursRules.fromIntervals(emptyList()),
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    /** `s.m`: sin esto el horario es de solo lectura. */
    val canEdit: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val savedNotice: Boolean = false,
) {
    val status: BusinessHoursStatus get() = saved?.status ?: BusinessHoursStatus()

    /** Error de validación local (o null si se puede guardar). */
    val validation: String? get() = BusinessHoursRules.validateWeek(week)

    val dirty: Boolean get() = saved != null && BusinessHoursRules.isDirty(week, saved.intervals)

    val canSave: Boolean get() = canEdit && !saving && dirty && validation == null
}

/**
 * Reductor del horario de atención del marketplace. Sin Android. Solo `s.m` guarda; valida local igual que el
 * backend (apertura distinta del cierre, hasta 4 tramos por día) y manda un PUT que REEMPLAZA todo el horario.
 */
class BusinessHoursPresenter(
    private val scope: CoroutineScope,
    private val repository: OpsRepository,
) {
    private val _state = MutableStateFlow(BusinessHoursState())
    val state: StateFlow<BusinessHoursState> = _state.asStateFlow()

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = OpsControlsRules.canEdit(permissions)) }
    }

    fun load() {
        _state.update { it.copy(loading = true, loadFailed = false, error = null) }
        scope.launch {
            when (val r = repository.getBusinessHours()) {
                is AppResult.Success -> _state.update { it.applySaved(r.data, loading = false) }
                is AppResult.Error -> _state.update { it.copy(loading = false, loadFailed = true, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun edit(change: (WeekDraft) -> WeekDraft) = _state.update {
        if (!it.canEdit || it.saving) it else it.copy(week = change(it.week), error = null, savedNotice = false)
    }

    fun setDayOpen(day: Int, open: Boolean) = edit { BusinessHoursRules.setDayOpen(it, day, open) }
    fun addSlot(day: Int) = edit { BusinessHoursRules.addSlot(it, day) }
    fun removeSlot(day: Int, index: Int) = edit { BusinessHoursRules.removeSlot(it, day, index) }
    fun updateSlot(day: Int, index: Int, slot: SlotDraft) = edit { BusinessHoursRules.updateSlot(it, day, index, slot) }
    fun copyToAll(fromDay: Int) = edit { BusinessHoursRules.copyToAll(it, fromDay) }

    fun save() {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        s.validation?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }
        _state.update { it.copy(saving = true, error = null, savedNotice = false) }
        scope.launch {
            when (val r = repository.saveBusinessHours(BusinessHoursRules.toIntervals(s.week))) {
                is AppResult.Success -> _state.update { it.applySaved(r.data, loading = false).copy(saving = false, savedNotice = true) }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun BusinessHoursState.applySaved(data: BusinessHours, loading: Boolean) = copy(
        saved = data,
        week = BusinessHoursRules.fromIntervals(data.intervals),
        loading = loading,
        loadFailed = false,
        error = null,
    )
}
