package com.bendey.restaurant.core.domain.rcshare

import com.bendey.restaurant.core.domain.delivery.ReportRange
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.domain.model.AppResult
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RcPeriodPreset(val label: String) {
    THIS_MONTH("Este mes"),
    PREV_MONTH("Mes anterior"),
    CUSTOM("Elegir fechas"),
}

/** Trabajador al que se le puede asignar parte del reparto. */
data class RcStaff(val id: Int, val name: String)

data class RcShareUiState(
    val preset: RcPeriodPreset = RcPeriodPreset.THIS_MONTH,
    val fromText: String = "",
    val toText: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val summary: RcSummary? = null,
    val shownRange: ReportRange? = null,
    val canDistribute: Boolean = false,
    // ---- hoja «Registrar reparto» ----
    val sheetOpen: Boolean = false,
    val staff: List<RcStaff> = emptyList(),
    val staffLoading: Boolean = false,
    val draft: List<RcShareDraftLine> = emptyList(),
    val notes: String = "",
    val payFromCash: Boolean = false,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val snack: String? = null,
) {
    val pending: Double get() = summary?.pending ?: 0.0

    /** Total tecleado en la hoja. */
    val draftTotal: Double get() = RcShareRules.total(draft)

    /** Lo que queda sin asignar (negativo = se pasó). */
    val leftover: Double get() = RcShareRules.fromCents(RcShareRules.leftoverCents(pending, draft))

    val overLimit: Boolean get() = RcShareRules.totalCents(draft) > RcShareRules.toCents(pending)

    val canSubmit: Boolean get() = !submitting && RcShareRules.validate(pending, draft) == null
}

/**
 * Pantalla «Reparto del recargo al consumo». Sin Android: la usan el ViewModel y los tests.
 * [today] da el día de Lima (inyectable).
 */
class RcSharePresenter(
    private val scope: CoroutineScope,
    private val repository: RcShareRepository,
    private val loadStaff: suspend () -> AppResult<List<RcStaff>>,
    private val today: () -> LocalDate = ReportRangeRules::todayLima,
) {
    private val _state = MutableStateFlow(RcShareUiState())
    val state: StateFlow<RcShareUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(canDistribute: Boolean) {
        _state.update { it.copy(canDistribute = canDistribute) }
        if (_state.value.summary == null && !_state.value.loading) selectPreset(_state.value.preset)
    }

    fun selectPreset(preset: RcPeriodPreset) {
        if (preset == RcPeriodPreset.CUSTOM) {
            val range = RcShareRules.defaultPeriod(today())
            _state.update {
                it.copy(
                    preset = preset,
                    error = null,
                    fromText = it.fromText.ifBlank { range.fromIso },
                    toText = it.toText.ifBlank { range.toIso },
                )
            }
            return
        }
        val range = if (preset == RcPeriodPreset.PREV_MONTH) RcShareRules.previousMonth(today()) else RcShareRules.defaultPeriod(today())
        _state.update { it.copy(preset = preset, fromText = range.fromIso, toText = range.toIso, error = null) }
        load(range)
    }

    fun setFrom(iso: String) = _state.update { it.copy(fromText = iso, error = null) }
    fun setTo(iso: String) = _state.update { it.copy(toText = iso, error = null) }

    fun applyCustom() {
        val s = _state.value
        val from = runCatching { LocalDate.parse(s.fromText) }.getOrNull()
        val to = runCatching { LocalDate.parse(s.toText) }.getOrNull()
        if (from == null || to == null) {
            _state.update { it.copy(error = "Elige las fechas «Desde» y «Hasta».") }
            return
        }
        RcShareRules.validatePeriod(from, to, today())?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }
        load(ReportRange(from, to))
    }

    fun refresh() {
        val s = _state.value
        if (s.preset == RcPeriodPreset.CUSTOM) applyCustom() else selectPreset(s.preset)
    }

    private fun load(range: ReportRange) {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch {
            when (val r = repository.summary(range.fromIso, range.toIso)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, summary = r.data, shownRange = range, error = null) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- hoja de reparto ----

    fun openDistribute() {
        val s = _state.value
        if (!s.canDistribute || s.summary == null || s.pending <= 0.009) return
        _state.update { it.copy(sheetOpen = true, submitError = null, notes = "", payFromCash = false, draft = it.draft.map { l -> l.copy(amount = "") }) }
        if (s.staff.isNotEmpty() || s.staffLoading) return
        _state.update { it.copy(staffLoading = true) }
        scope.launch {
            when (val r = loadStaff()) {
                is AppResult.Success -> _state.update {
                    it.copy(staffLoading = false, staff = r.data, draft = r.data.map { p -> RcShareDraftLine(p.id) })
                }
                is AppResult.Error -> _state.update { it.copy(staffLoading = false, submitError = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun closeDistribute() {
        if (!_state.value.submitting) _state.update { it.copy(sheetOpen = false, submitError = null) }
    }

    fun setAmount(staffId: Int, text: String) = _state.update { st ->
        st.copy(draft = st.draft.map { if (it.staffId == staffId) it.copy(amount = text) else it }, submitError = null)
    }

    /** «Dividir en partes iguales lo que falta». */
    fun splitRemainder() = _state.update { it.copy(draft = RcShareRules.splitRemainder(it.pending, it.draft), submitError = null) }

    fun setNotes(text: String) = _state.update { it.copy(notes = text.take(255)) }
    fun setPayFromCash(value: Boolean) = _state.update { it.copy(payFromCash = value, submitError = null) }
    fun consumeSnack() = _state.update { it.copy(snack = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        RcShareRules.validate(s.pending, s.draft)?.let { msg ->
            _state.update { it.copy(submitError = msg) }
            return
        }
        val range = s.shownRange ?: return
        val names = s.staff.associate { it.id to it.name }
        val input = RcShareRules.toInput(range.fromIso, range.toIso, s.draft, names, s.notes, s.payFromCash)
        _state.update { it.copy(submitting = true, submitError = null) }
        scope.launch {
            when (val r = repository.distribute(input)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            sheetOpen = false,
                            snack = "Reparto registrado: ${RcShareText.money(input.lines.sumOf { l -> l.amount })}.",
                            draft = it.draft.map { l -> l.copy(amount = "") },
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> {
                    _state.update { it.copy(submitting = false, submitError = r.message) }
                    // Otro usuario ya repartió o el período cambió: se vuelve a leer lo pendiente.
                    if (r.code == "RC_SHARE_EXCEEDS_PENDING" || r.code == "RC_SHARE_NOTHING") refresh()
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
