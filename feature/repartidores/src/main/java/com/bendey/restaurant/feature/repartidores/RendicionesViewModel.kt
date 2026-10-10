package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.delivery.DocRequest
import com.bendey.restaurant.core.domain.delivery.RefundPending
import com.bendey.restaurant.core.domain.delivery.SettlementGroup
import com.bendey.restaurant.core.domain.delivery.SettlementRecord
import com.bendey.restaurant.core.domain.delivery.SettlementRepository
import com.bendey.restaurant.core.domain.delivery.SettlementsPending
import com.bendey.restaurant.core.domain.delivery.settlementDifference
import com.bendey.restaurant.core.domain.delivery.settlementHasDifference
import com.bendey.restaurant.core.domain.model.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiveDialogState(
    val group: SettlementGroup,
    val selected: Set<Int>,
    /** null = sigue al monto esperado; en cuanto el cajero escribe, queda lo que escribio. */
    val receivedEdit: String? = null,
    val reason: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val expected: Double get() = Math.round(group.orders.filter { it.paymentId in selected }.sumOf { it.amount } * 100.0) / 100.0
    val receivedText: String get() = receivedEdit ?: expected.toString()
    val receivedValue: Double? get() = receivedText.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
    val difference: Double get() = receivedValue?.let { settlementDifference(expected, it) } ?: 0.0
    val hasDifference: Boolean get() = receivedValue?.let { settlementHasDifference(expected, it) } ?: false
}

data class CancelDocDialogState(val item: DocRequest, val reason: String = "", val busy: Boolean = false, val error: String? = null)

data class RefundDialogState(val item: RefundPending, val note: String = "", val busy: Boolean = false, val error: String? = null)

data class RendicionesUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val pending: SettlementsPending = SettlementsPending(emptyList(), emptyList()),
    val history: List<SettlementRecord> = emptyList(),
    val receive: ReceiveDialogState? = null,
    val refund: RefundDialogState? = null,
    val docRequests: List<DocRequest> = emptyList(),
    val issuingId: Int? = null,
    val cancelDoc: CancelDocDialogState? = null,
) {
    val pendingOrders: Int get() = pending.groups.sumOf { it.orders.size }
}

/** Rendicion de efectivo (D2c): recibir lo que cobraron los repartidores y cerrar devoluciones pendientes. */
@HiltViewModel
class RendicionesViewModel @Inject constructor(
    private val repository: SettlementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RendicionesUiState())
    val state: StateFlow<RendicionesUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            when (val p = repository.pending()) {
                is AppResult.Success -> {
                    val h = (repository.history() as? AppResult.Success)?.data.orEmpty()
                    val d = (repository.docRequests() as? AppResult.Success)?.data.orEmpty()
                    _state.update { it.copy(loading = false, pending = p.data, history = h, docRequests = d) }
                }
                is AppResult.Error -> _state.update { it.copy(loading = false, loadError = p.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- recibir efectivo
    fun openReceive(group: SettlementGroup) = _state.update {
        it.copy(receive = ReceiveDialogState(group = group, selected = group.orders.map { o -> o.paymentId }.toSet()))
    }

    fun toggleOrder(paymentId: Int) = _state.update { st ->
        st.copy(receive = st.receive?.let { r ->
            r.copy(selected = if (paymentId in r.selected) r.selected - paymentId else r.selected + paymentId, error = null)
        })
    }

    fun setReceived(text: String) = _state.update { st ->
        st.copy(receive = st.receive?.copy(receivedEdit = text.filter { it.isDigit() || it == '.' || it == ',' }.take(12), error = null))
    }

    fun setReason(text: String) = _state.update { st -> st.copy(receive = st.receive?.copy(reason = text.take(255), error = null)) }

    fun dismissReceive() = _state.update { it.copy(receive = null) }

    fun confirmReceive() {
        val r = _state.value.receive ?: return
        if (r.busy) return
        val received = r.receivedValue
        val error = when {
            r.selected.isEmpty() -> "Elige al menos un pedido."
            received == null -> "Escribe el monto que recibiste."
            r.hasDifference && r.reason.trim().length < 3 -> "Indica el motivo de la diferencia (minimo 3 caracteres)."
            else -> null
        }
        if (error != null || received == null) {
            _state.update { it.copy(receive = r.copy(error = error)) }
            return
        }
        _state.update { it.copy(receive = r.copy(busy = true, error = null)) }
        viewModelScope.launch {
            when (val res = repository.receive(r.group, r.selected.toList(), received, if (r.hasDifference) r.reason.trim() else null)) {
                is AppResult.Success -> {
                    _messages.tryEmit("Rendicion recibida: ${r.selected.size} pedido(s)")
                    res.data?.let { _messages.tryEmit(it) }
                    _state.update { it.copy(receive = null) }
                    refresh()
                }
                is AppResult.Error -> _state.update { it.copy(receive = _state.value.receive?.copy(busy = false, error = res.message)) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- boleta / factura pedida en la puerta
    fun issueDoc(item: DocRequest) {
        if (_state.value.issuingId != null) return
        _state.update { it.copy(issuingId = item.id) }
        viewModelScope.launch {
            when (val res = repository.issueDocRequest(item.id)) {
                is AppResult.Success -> {
                    _messages.tryEmit("${item.label} emitida ${res.data}. Imprimela o enviala por WhatsApp desde Ventas.")
                    _state.update { it.copy(issuingId = null) }
                    refresh()
                }
                is AppResult.Error -> {
                    _messages.tryEmit(res.message)
                    _state.update { it.copy(issuingId = null) }
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCancelDoc(item: DocRequest) = _state.update { it.copy(cancelDoc = CancelDocDialogState(item)) }
    fun setCancelDocReason(text: String) = _state.update { st -> st.copy(cancelDoc = st.cancelDoc?.copy(reason = text.take(255), error = null)) }
    fun dismissCancelDoc() = _state.update { it.copy(cancelDoc = null) }

    fun confirmCancelDoc() {
        val d = _state.value.cancelDoc ?: return
        if (d.busy) return
        if (d.reason.trim().length < 3) {
            _state.update { it.copy(cancelDoc = d.copy(error = "Escribe el motivo (minimo 3 caracteres).")) }
            return
        }
        _state.update { it.copy(cancelDoc = d.copy(busy = true, error = null)) }
        viewModelScope.launch {
            when (val res = repository.cancelDocRequest(d.item.id, d.reason.trim())) {
                is AppResult.Success -> {
                    _messages.tryEmit("Solicitud cancelada")
                    _state.update { it.copy(cancelDoc = null) }
                    refresh()
                }
                is AppResult.Error -> _state.update { it.copy(cancelDoc = _state.value.cancelDoc?.copy(busy = false, error = res.message)) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- devolucion pendiente
    fun openRefund(item: RefundPending) = _state.update { it.copy(refund = RefundDialogState(item)) }
    fun setRefundNote(text: String) = _state.update { st -> st.copy(refund = st.refund?.copy(note = text.take(255), error = null)) }
    fun dismissRefund() = _state.update { it.copy(refund = null) }

    fun confirmRefund() {
        val d = _state.value.refund ?: return
        if (d.busy) return
        if (d.note.trim().length < 3) {
            _state.update { it.copy(refund = d.copy(error = "Escribe una nota (minimo 3 caracteres).")) }
            return
        }
        _state.update { it.copy(refund = d.copy(busy = true, error = null)) }
        viewModelScope.launch {
            when (val res = repository.resolveRefund(d.item.paymentId, d.note.trim())) {
                is AppResult.Success -> {
                    _messages.tryEmit("Devolucion registrada")
                    _state.update { it.copy(refund = null) }
                    refresh()
                }
                is AppResult.Error -> _state.update { it.copy(refund = _state.value.refund?.copy(busy = false, error = res.message)) }
                AppResult.Loading -> Unit
            }
        }
    }
}
