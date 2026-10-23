package com.bendey.restaurant.feature.productos.reportes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.SoldReportFilter
import com.bendey.restaurant.core.domain.sales.SoldReportPreset
import com.bendey.restaurant.core.domain.sales.SoldReportRepository
import com.bendey.restaurant.core.domain.sales.SoldReportRules
import com.bendey.restaurant.core.domain.sales.SoldReportSummary
import com.bendey.restaurant.core.domain.sales.SoldSale
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VendidosUiState(
    val preset: SoldReportPreset = SoldReportPreset.TODAY,
    val customFrom: String = ReportRangeRules.todayLima().toString(),
    val customTo: String = ReportRangeRules.todayLima().toString(),
    val users: List<Pair<Int, String>> = emptyList(),
    val methods: List<Pair<String, String>> = emptyList(),
    val userId: Int? = null,
    val paymentMethod: String? = null,
    val docKind: String? = null,
    val onlyValid: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val rangeError: String? = null,
    val total: Int = 0,
    val summary: SoldReportSummary = SoldReportSummary(),
    val sales: List<SoldSale> = emptyList(),
    val page: Int = 1,
    val expanded: Set<Int> = emptySet(),
    val allowsExport: Boolean = false,
    val exportBusy: String? = null,
    val snack: String? = null,
) {
    val hasMore: Boolean get() = sales.size < total
}

/** Reportes → Vendidos: cada venta con sus productos, quién cobró, mozo, atención, comprobante y pagos. */
@HiltViewModel
class VendidosViewModel @Inject constructor(
    private val repository: SoldReportRepository,
    private val cashRepository: CashRepository,
    private val sessionStore: UserSessionStore,
    private val fileShareService: BendeyFileShareService,
) : ViewModel() {
    private val _state = MutableStateFlow(VendidosUiState())
    val state: StateFlow<VendidosUiState> = _state.asStateFlow()
    private var started = false
    private var loadJob: Job? = null

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val session = sessionStore.userSessionFlow.first()
            _state.update { it.copy(allowsExport = session?.allowsReportExport == true) }
            (cashRepository.listCashFilterUsers() as? AppResult.Success)?.let { r ->
                _state.update { s -> s.copy(users = r.data.map { it.userId to it.name }.distinctBy { it.first }) }
            }
            (cashRepository.listPaymentMethods() as? AppResult.Success)?.let { r ->
                _state.update { s -> s.copy(methods = r.data.filter { it.active }.map { it.code to it.name }.distinctBy { it.first }) }
            }
        }
        reload()
    }

    fun setPreset(p: SoldReportPreset) = change { it.copy(preset = p) }
    fun setCustomFrom(v: String) = change { it.copy(customFrom = v) }
    fun setCustomTo(v: String) = change { it.copy(customTo = v) }
    fun setUser(id: Int?) = change { it.copy(userId = id) }
    fun setPaymentMethod(code: String?) = change { it.copy(paymentMethod = code) }
    fun setDocKind(kind: String?) = change { it.copy(docKind = kind) }
    fun setOnlyValid(v: Boolean) = change { it.copy(onlyValid = v) }

    fun toggle(saleId: Int) = _state.update {
        it.copy(expanded = if (saleId in it.expanded) it.expanded - saleId else it.expanded + saleId)
    }

    private fun change(f: (VendidosUiState) -> VendidosUiState) {
        _state.update(f)
        reload()
    }

    /** Rango efectivo, o null con el mensaje de error si las fechas elegidas no sirven. */
    private fun filter(s: VendidosUiState): SoldReportFilter? {
        val today = ReportRangeRules.todayLima()
        val range = SoldReportRules.preset(s.preset, today)
        val (from, to) = if (range != null) {
            range.from to range.to
        } else {
            val f = runCatching { LocalDate.parse(s.customFrom) }.getOrNull()
            val t = runCatching { LocalDate.parse(s.customTo) }.getOrNull()
            if (f == null || t == null) {
                _state.update { it.copy(rangeError = "Elige las dos fechas.") }
                return null
            }
            ReportRangeRules.validate(f, t, today)?.let { msg ->
                _state.update { it.copy(rangeError = msg) }
                return null
            }
            f to t
        }
        _state.update { it.copy(rangeError = null) }
        return SoldReportFilter(
            from = from.toString(),
            to = to.toString(),
            userId = s.userId,
            paymentMethod = s.paymentMethod,
            docKind = s.docKind,
            onlyValid = s.onlyValid,
        )
    }

    fun reload() {
        val f = filter(_state.value) ?: return
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null, page = 1, expanded = emptySet()) }
        loadJob = viewModelScope.launch {
            when (val r = repository.page(f, 1, SoldReportRules.PER_PAGE)) {
                is AppResult.Success -> _state.update {
                    it.copy(loading = false, total = r.data.total, summary = r.data.summary, sales = r.data.sales)
                }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message, sales = emptyList(), total = 0) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        val f = filter(s) ?: return
        val next = s.page + 1
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            when (val r = repository.page(f, next, SoldReportRules.PER_PAGE)) {
                is AppResult.Success -> _state.update {
                    it.copy(loadingMore = false, page = next, total = r.data.total, sales = it.sales + r.data.sales)
                }
                is AppResult.Error -> _state.update { it.copy(loadingMore = false, snack = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun export(context: Context, format: String) {
        val s = _state.value
        if (s.exportBusy != null) return
        val f = filter(s) ?: return
        _state.update { it.copy(exportBusy = format) }
        viewModelScope.launch {
            when (val r = repository.export(f)) {
                is AppResult.Success -> {
                    if (r.data.rows.isEmpty()) {
                        _state.update { it.copy(exportBusy = null, snack = "No hay datos para exportar") }
                        return@launch
                    }
                    val result = if (format == "pdf") {
                        exportSoldPdf(context, fileShareService, r.data.rows, f.from, f.to)
                    } else {
                        exportSoldCsv(context, fileShareService, r.data.rows, f.from, f.to)
                    }
                    val msg = (result as? ExportShareResult.Failure)?.userMessage
                        ?: if (r.data.truncated) "Se exportaron las primeras 5000 ventas. Acorta las fechas para ver todo." else null
                    _state.update { it.copy(exportBusy = null, snack = msg) }
                }
                is AppResult.Error -> _state.update { it.copy(exportBusy = null, snack = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnack() = _state.update { it.copy(snack = null) }
}
