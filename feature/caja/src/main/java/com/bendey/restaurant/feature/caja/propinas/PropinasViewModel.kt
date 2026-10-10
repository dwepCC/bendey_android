package com.bendey.restaurant.feature.caja.propinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.delivery.RangedReportPresenter
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.tips.MyTips
import com.bendey.restaurant.core.domain.tips.PendingTipStaff
import com.bendey.restaurant.core.domain.tips.PropinasTab
import com.bendey.restaurant.core.domain.tips.PropinasTabs
import com.bendey.restaurant.core.domain.tips.TipsReport
import com.bendey.restaurant.core.domain.tips.TipsRepository
import com.bendey.restaurant.core.domain.tips.TipsText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PropinasUiState(
    val tabs: List<PropinasTab> = listOf(PropinasTab.MIS_PROPINAS),
    val tab: PropinasTab = PropinasTab.MIS_PROPINAS,
    val pending: List<PendingTipStaff>? = null,
    val pendingLoading: Boolean = false,
    val pendingError: String? = null,
    /** Mozo cuyas propinas se van a pagar (diálogo de confirmación). */
    val confirm: PendingTipStaff? = null,
    val paying: Boolean = false,
    val payError: String? = null,
    val mine: MyTips? = null,
    val mineLoading: Boolean = false,
    val mineError: String? = null,
    val snack: String? = null,
)

@HiltViewModel
class PropinasViewModel @Inject constructor(
    private val repository: TipsRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(PropinasUiState())
    val state: StateFlow<PropinasUiState> = _state.asStateFlow()

    /** Reporte con selector Hoy / 7 / 30 / fechas. */
    val report = RangedReportPresenter<TipsReport>(viewModelScope) { from, to -> repository.report(from, to) }

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val tabs = PropinasTabs.available(sessionStore.userSessionFlow.first()?.restaurantPermissions)
            _state.update { it.copy(tabs = tabs, tab = tabs.first()) }
            loadTab(tabs.first())
        }
    }

    fun selectTab(tab: PropinasTab) {
        _state.update { it.copy(tab = tab) }
        loadTab(tab)
    }

    fun refresh() = loadTab(_state.value.tab, force = true)

    private fun loadTab(tab: PropinasTab, force: Boolean = false) {
        when (tab) {
            PropinasTab.POR_PAGAR -> if (force || _state.value.pending == null) loadPending()
            PropinasTab.REPORTE -> if (force) report.refresh() else report.start()
            PropinasTab.MIS_PROPINAS -> if (force || _state.value.mine == null) loadMine()
        }
    }

    private fun loadPending() {
        _state.update { it.copy(pendingLoading = true, pendingError = null) }
        viewModelScope.launch {
            when (val r = repository.pending()) {
                is AppResult.Success -> _state.update { it.copy(pendingLoading = false, pending = r.data) }
                is AppResult.Error -> _state.update { it.copy(pendingLoading = false, pendingError = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadMine() {
        _state.update { it.copy(mineLoading = true, mineError = null) }
        viewModelScope.launch {
            when (val r = repository.mine()) {
                is AppResult.Success -> _state.update { it.copy(mineLoading = false, mine = r.data) }
                is AppResult.Error -> _state.update { it.copy(mineLoading = false, mineError = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun askPayout(row: PendingTipStaff) = _state.update { it.copy(confirm = row, payError = null) }

    fun dismissPayout() {
        if (!_state.value.paying) _state.update { it.copy(confirm = null, payError = null) }
    }

    fun confirmPayout() {
        val row = _state.value.confirm ?: return
        if (_state.value.paying) return
        _state.update { it.copy(paying = true, payError = null) }
        viewModelScope.launch {
            when (val r = repository.payout(row.staffId)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            paying = false,
                            confirm = null,
                            snack = "Pagaste ${TipsText.money(r.data.amount.takeIf { a -> a > 0 } ?: row.total)} en propinas a ${TipsText.staffName(row)}.",
                        )
                    }
                    loadPending()
                }
                // TIP_NO_CASH_SESSION / TIP_NOTHING_TO_PAY llegan con su texto del catálogo.
                is AppResult.Error -> {
                    _state.update { it.copy(paying = false, payError = r.message) }
                    if (r.code == "TIP_NOTHING_TO_PAY") loadPending()
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnack() = _state.update { it.copy(snack = null) }
}
