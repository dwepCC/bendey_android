package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsRepository
import com.bendey.restaurant.core.domain.delivery.PendingEarning
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GananciasUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val items: List<PendingEarning> = emptyList(),
    /** o.ch: sin esto la pestaña es de solo lectura (el servidor solo deja pagar a quien cobra). */
    val canPay: Boolean = false,
    /** Repartidor cuyo pago se está confirmando. */
    val confirm: PendingEarning? = null,
    val busy: Boolean = false,
) {
    /** Total que se debe a todos los repartidores. */
    val grandTotal: Double get() = items.sumOf { it.total }
}

/**
 * Pestaña «Ganancias» de Entregas (§27): lo que se debe a cada repartidor y «Pagar ganancias» con confirmación
 * (el efectivo sale de la caja de quien paga). Sin Android: la usa [GananciasViewModel] y los tests.
 */
class GananciasPresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryEarningsRepository,
) {
    private val _state = MutableStateFlow(GananciasUiState())
    val state: StateFlow<GananciasUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setCanPay(value: Boolean) = _state.update { it.copy(canPay = value) }

    fun refresh() {
        scope.launch { load() }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = it.items.isEmpty(), loadError = null) }
        when (val r = repository.pending()) {
            is AppResult.Success -> _state.update { st ->
                st.copy(
                    loading = false,
                    items = r.data.filter { it.total > 0.0 || it.deliveries > 0 },
                    // Si otro cajero ya pagó a quien se estaba confirmando, el diálogo se cierra.
                    confirm = st.confirm?.let { c -> r.data.firstOrNull { it.driverId == c.driverId } },
                )
            }
            is AppResult.Error -> _state.update { it.copy(loading = false, loadError = r.message) }
            AppResult.Loading -> Unit
        }
    }

    fun askPay(item: PendingEarning) {
        if (_state.value.canPay) _state.update { it.copy(confirm = item) }
    }

    fun dismissPay() = _state.update { if (it.busy) it else it.copy(confirm = null) }

    fun confirmPay() {
        val s = _state.value
        val item = s.confirm ?: return
        if (s.busy || !s.canPay) return
        _state.update { it.copy(busy = true) }
        scope.launch {
            when (val r = repository.payout(item.driverId)) {
                is AppResult.Success -> {
                    _messages.tryEmit(DeliveryEarningsCopy.paidMessage(r.data.copy(name = r.data.name.ifBlank { item.name })))
                    _state.update { it.copy(confirm = null, busy = false) }
                    load()
                }
                is AppResult.Error -> {
                    // EARNINGS_NO_CASH_SESSION / EARNINGS_NOTHING_TO_PAY llegan ya traducidos en r.message.
                    _messages.tryEmit(r.message)
                    _state.update { it.copy(confirm = null, busy = false) }
                    load()
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
