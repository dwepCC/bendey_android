package com.bendey.restaurant.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class DashboardRange(val label: String, val days: Int) {
    TODAY("Hoy", 1),
    WEEK("7 días", 7),
    MONTH("30 días", 30),
}

data class DashboardUiState(
    val dashboard: RestaurantDashboard = RestaurantDashboard(),
    val range: DashboardRange = DashboardRange.TODAY,
    val loading: Boolean = false,
    val error: String? = null,
    val branchName: String? = null,
    val userName: String = "",
    val isOnline: Boolean = true,
    val cashLabel: String? = null,
    val cashSession: CashSessionSnapshot? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    sessionManager: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        viewModelScope.launch {
            sessionManager.userSessionFlow.collect { session ->
                _uiState.update {
                    it.copy(
                        branchName = session?.activeBranch?.name,
                        userName = session?.user?.name.orEmpty(),
                    )
                }
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
        _uiState.update { it.copy(range = range) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val today = LocalDate.now()
            val range = _uiState.value.range
            val from = today.minusDays(range.days - 1L).format(dateFmt)
            val to = today.format(dateFmt)
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
