package com.bendey.restaurant.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.UiPresence
import com.bendey.restaurant.core.realtime.dispatcher.ConnectionState
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.realtime.recovery.RestaurantHydrators
import com.bendey.restaurant.core.realtime.store.DashboardStore
import com.bendey.restaurant.core.realtime.store.dashboardStoreKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import com.bendey.restaurant.core.realtime.DashboardRange as RealtimeDashboardRange

private const val DASHBOARD_DEGRADATION_INTERVAL_MS = 300_000L

enum class DashboardRange(val label: String) {
    TODAY("Hoy"),
    YESTERDAY("Ayer"),
    WEEK("7 días"),
    MONTH("30 días"),
    CUSTOM("Rango"),
}

enum class DashboardTab(val label: String) {
    OPERACION("Operación"),
    CATALOGO("Catálogo"),
}

data class DashboardUiState(
    val dashboard: RestaurantDashboard = RestaurantDashboard(),
    val catalog: CatalogAnalytics? = null,
    val tab: DashboardTab = DashboardTab.OPERACION,
    val range: DashboardRange = DashboardRange.TODAY,
    val fromDate: LocalDate = todayInPeru(),
    val toDate: LocalDate = todayInPeru(),
    val canChangeDateRange: Boolean = false,
    val loading: Boolean = false,
    val catalogLoading: Boolean = false,
    val error: String? = null,
    val branchName: String? = null,
    val exportBusy: String? = null,
    val allowsReportExport: Boolean = false,
) {
    val fromApi: String get() = fromDate.toApiDate()
    val toApi: String get() = toDate.toApiDate()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: UserSessionStore,
    private val restaurantHydrators: RestaurantHydrators,
    private val dashboardStore: DashboardStore,
    private val observability: RealtimeObservability,
    private val fileShareService: BendeyFileShareService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        UiPresence.setDashboard(true, RealtimeDashboardRange(_uiState.value.fromApi, _uiState.value.toApi))
        viewModelScope.launch {
            dashboardStore.state.collect { snapshot ->
                val key = dashboardStoreKey(_uiState.value.fromApi, _uiState.value.toApi)
                val entry = snapshot.entities[key] ?: return@collect
                _uiState.update {
                    it.copy(
                        dashboard = entry.operacion ?: it.dashboard,
                        catalog = entry.catalog ?: it.catalog,
                    )
                }
            }
        }
        // Degradación: recovery periódico solo si el WS no está READY (REALTIME_TAURI_IMPLEMENTATION.md §6.6).
        viewModelScope.launch {
            while (isActive) {
                delay(DASHBOARD_DEGRADATION_INTERVAL_MS)
                if (observability.snapshot.value.connectionState != ConnectionState.READY) {
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            sessionManager.userSessionFlow.collect { session ->
                val today = todayInPeru()
                val canChange = isDashboardDateAdmin(session?.user?.employeeType)
                _uiState.update { state ->
                    val (from, to) = if (canChange) {
                        state.fromDate to state.toDate
                    } else {
                        today to today
                    }
                    state.copy(
                        branchName = session?.activeBranch?.name,
                        canChangeDateRange = canChange,
                        fromDate = from,
                        toDate = to,
                        range = if (canChange) state.range else DashboardRange.TODAY,
                        allowsReportExport = session?.allowsReportExport == true,
                    )
                }
                if (!canChange) refresh()
            }
        }
        refresh()
    }

    fun selectRange(range: DashboardRange) {
        if (!_uiState.value.canChangeDateRange) return
        val today = todayInPeru()
        val (from, to) = range.resolveDatesPeru(today)
        _uiState.update { it.copy(range = range, fromDate = from, toDate = to) }
        refresh()
    }

    fun applyCustomRange(fromValue: String, toValue: String) {
        if (!_uiState.value.canChangeDateRange) return
        val from = parseApiDate(fromValue) ?: return
        val to = parseApiDate(toValue) ?: return
        val today = todayInPeru()
        val clampedTo = to.coerceAtMost(today).coerceAtLeast(from)
        val clampedFrom = from.coerceAtMost(clampedTo)
        _uiState.update {
            it.copy(
                range = DashboardRange.CUSTOM,
                fromDate = clampedFrom,
                toDate = clampedTo,
            )
        }
        refresh()
    }

    fun setCustomFrom(value: String) {
        if (!_uiState.value.canChangeDateRange) return
        val parsed = parseApiDate(value) ?: return
        val to = _uiState.value.toDate.coerceAtLeast(parsed).coerceAtMost(todayInPeru())
        val from = parsed.coerceAtMost(to)
        _uiState.update { it.copy(fromDate = from, toDate = to) }
        refresh()
    }

    fun setCustomTo(value: String) {
        if (!_uiState.value.canChangeDateRange) return
        val parsed = parseApiDate(value) ?: return
        val today = todayInPeru()
        val to = parsed.coerceAtMost(today).coerceAtLeast(_uiState.value.fromDate)
        _uiState.update { it.copy(toDate = to) }
        refresh()
    }

    fun selectTab(tab: DashboardTab) {
        _uiState.update { it.copy(tab = tab, error = null) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val state = _uiState.value
            val today = todayInPeru()
            val (fromDate, toDate) = if (state.canChangeDateRange) {
                state.fromDate to state.toDate.coerceAtMost(today)
            } else {
                today to today
            }
            val from = fromDate.toApiDate()
            val to = toDate.toApiDate()
            UiPresence.setDashboard(true, RealtimeDashboardRange(from, to))
            if (_uiState.value.tab == DashboardTab.CATALOGO) {
                _uiState.update { it.copy(catalogLoading = true, error = null, fromDate = fromDate, toDate = toDate) }
                when (val result = restaurantHydrators.hydrateDashboardCatalog(from = from, to = to)) {
                    is AppResult.Error -> _uiState.update {
                        it.copy(catalogLoading = false, error = result.message)
                    }
                    else -> _uiState.update { it.copy(catalogLoading = false) }
                }
            } else {
                _uiState.update { it.copy(loading = true, error = null, fromDate = fromDate, toDate = toDate) }
                when (val result = restaurantHydrators.hydrateDashboardOperacion(from = from, to = to)) {
                    is AppResult.Error -> _uiState.update {
                        it.copy(loading = false, error = result.message)
                    }
                    else -> _uiState.update { it.copy(loading = false) }
                }
            }
        }
    }

    fun exportPdf(context: Context) = export(context, format = "pdf")

    fun exportExcel(context: Context) = export(context, format = "excel")

    private fun export(context: Context, format: String) {
        val state = _uiState.value
        if (state.exportBusy != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(exportBusy = format, error = null) }
            val shareResult = withContext(Dispatchers.Main) {
                if (state.tab == DashboardTab.CATALOGO) {
                    val catalog = state.catalog ?: return@withContext null
                    if (format == "pdf") {
                        exportCatalogAnalyticsPdf(context, fileShareService, catalog, state.fromApi, state.toApi, state.branchName)
                    } else {
                        exportCatalogAnalyticsCsv(context, fileShareService, catalog, state.fromApi, state.toApi, state.branchName)
                    }
                } else {
                    if (format == "pdf") {
                        exportOperationalDashboardPdf(context, fileShareService, state.dashboard, state.fromApi, state.toApi, state.branchName)
                    } else {
                        exportOperationalDashboardCsv(context, fileShareService, state.dashboard, state.fromApi, state.toApi, state.branchName)
                    }
                }
            }
            _uiState.update {
                it.copy(
                    exportBusy = null,
                    error = when (shareResult) {
                        null -> "Sin datos para exportar"
                        ExportShareResult.Success -> null
                        is ExportShareResult.Failure -> shareResult.userMessage
                    },
                )
            }
        }
    }

    override fun onCleared() {
        UiPresence.setDashboard(false)
        super.onCleared()
    }
}
