package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.delivery.DeliveryReport
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRepository
import com.bendey.restaurant.core.domain.delivery.ReportPreset
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

data class ReporteUiState(
    val preset: ReportPreset = ReportPreset.LAST_7,
    /** Fechas ISO (yyyy-MM-dd) de los campos «Desde» / «Hasta» de «Elegir fechas». */
    val fromText: String = "",
    val toText: String = "",
    val loading: Boolean = false,
    /** Error de carga (catálogo) o de validación del rango. */
    val error: String? = null,
    val report: DeliveryReport? = null,
    /** Rango del reporte que se está mostrando. */
    val shownRange: ReportRange? = null,
) {
    /** Ya cargó y no hubo pedidos en el rango. */
    val isEmpty: Boolean get() = report != null && report.summary.orders == 0
}

/**
 * Reporte de delivery (D5). Sin Android: lo usa [ReporteViewModel] y los tests. [today] da el día de Lima
 * (inyectable para probar Hoy/7/30 sin depender del reloj).
 */
class ReportePresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryReportRepository,
    private val today: () -> LocalDate = ReportRangeRules::todayLima,
) {
    private val _state = MutableStateFlow(ReporteUiState())
    val state: StateFlow<ReporteUiState> = _state.asStateFlow()
    private var job: Job? = null

    /** Primera carga (7 días); no repite si ya hay datos. */
    fun start() {
        if (_state.value.report == null && !_state.value.loading) selectPreset(_state.value.preset)
    }

    fun selectPreset(preset: ReportPreset) {
        if (preset == ReportPreset.CUSTOM) {
            val t = today()
            _state.update {
                it.copy(
                    preset = preset,
                    error = null,
                    fromText = it.fromText.ifBlank { t.minusDays(6).toString() },
                    toText = it.toText.ifBlank { t.toString() },
                )
            }
            return
        }
        val range = ReportRangeRules.preset(preset, today()) ?: return
        _state.update { it.copy(preset = preset, fromText = range.fromIso, toText = range.toIso, error = null) }
        load(range)
    }

    fun setFrom(iso: String) = _state.update { it.copy(fromText = iso, error = null) }
    fun setTo(iso: String) = _state.update { it.copy(toText = iso, error = null) }

    /** «Consultar» de «Elegir fechas»: valida y carga. */
    fun applyCustom() {
        val s = _state.value
        val from = runCatching { LocalDate.parse(s.fromText) }.getOrNull()
        val to = runCatching { LocalDate.parse(s.toText) }.getOrNull()
        if (from == null || to == null) {
            _state.update { it.copy(error = "Elige las fechas «Desde» y «Hasta».") }
            return
        }
        ReportRangeRules.validate(from, to, today())?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }
        load(ReportRange(from, to))
    }

    fun refresh() {
        val s = _state.value
        if (s.preset == ReportPreset.CUSTOM) applyCustom() else selectPreset(s.preset)
    }

    private fun load(range: ReportRange) {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch {
            when (val r = repository.report(range.fromIso, range.toIso)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, report = r.data, shownRange = range, error = null) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}
