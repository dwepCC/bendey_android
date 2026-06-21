package com.bendey.restaurant.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.DashboardRepository
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.model.CashSessionSnapshot
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

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
    val userName: String = "",
    val isOnline: Boolean = true,
    val cashLabel: String? = null,
    val cashSession: CashSessionSnapshot? = null,
) {
    val fromApi: String get() = fromDate.toApiDate()
    val toApi: String get() = toDate.toApiDate()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    sessionManager: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
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
                        userName = session?.user?.name.orEmpty(),
                        canChangeDateRange = canChange,
                        fromDate = from,
                        toDate = to,
                        range = if (canChange) state.range else DashboardRange.TODAY,
                    )
                }
                if (!canChange) refresh()
            }
        }
        viewModelScope.launch {
            sessionManager.cashSessionFlow.collect { cash ->
                val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
                _uiState.update {
                    it.copy(
                        cashSession = cash,
                        cashLabel = cash?.let { session ->
                            val balance = session.expectedBalance ?: session.openingAmount
                            "Caja ${currency.format(balance)}"
                        } ?: "Caja ${currency.format(0.0)}",
                    )
                }
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
            if (_uiState.value.tab == DashboardTab.CATALOGO) {
                _uiState.update { it.copy(catalogLoading = true, error = null, fromDate = fromDate, toDate = toDate) }
                when (val result = dashboardRepository.loadCatalogAnalytics(from = from, to = to)) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(catalogLoading = false, catalog = result.data)
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(catalogLoading = false, error = result.message)
                    }
                    AppResult.Loading -> Unit
                }
            } else {
                _uiState.update { it.copy(loading = true, error = null, fromDate = fromDate, toDate = toDate) }
                when (val result = dashboardRepository.loadDashboard(from = from, to = to)) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(loading = false, dashboard = result.data)
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(loading = false, error = result.message)
                    }
                    AppResult.Loading -> Unit
                }
            }
        }
    }
}
