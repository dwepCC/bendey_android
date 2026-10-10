package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRules
import com.bendey.restaurant.core.domain.delivery.PaymentReview
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

data class RejectPaymentDialogState(
    val item: PaymentReview,
    val reason: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

data class PagosUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val items: List<PaymentReview> = emptyList(),
    /** Pago que se está validando (pide confirmación primero). */
    val approve: PaymentReview? = null,
    val approveBusy: Boolean = false,
    val reject: RejectPaymentDialogState? = null,
    /** Captura abierta a pantalla completa. */
    val viewer: PaymentReview? = null,
) {
    /** Pendientes por validar: alimenta la insignia de la pestaña. */
    val pendingCount: Int get() = items.size
}

/**
 * Bandeja «Pagos por validar» de Entregas (D2a). Sin Android: la usa [PagosViewModel] y los tests.
 * `printReceipt` imprime el comprobante de la venta recién creada y devuelve un aviso (o null).
 */
class PagosPresenter(
    private val scope: CoroutineScope,
    private val repository: ManualPaymentRepository,
    private val printReceipt: suspend (SalePrintData?) -> String?,
) {
    private val _state = MutableStateFlow(PagosUiState())
    val state: StateFlow<PagosUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun refresh() {
        scope.launch { load() }
    }

    private suspend fun load() {
        // No se muestra el indicador de carga si ya hay lista: el refresco por tiempo real es silencioso.
        _state.update { it.copy(loading = it.items.isEmpty(), loadError = null) }
        when (val r = repository.paymentsReview()) {
            is AppResult.Success -> _state.update { st ->
                st.copy(
                    loading = false,
                    items = r.data,
                    // Si el pago abierto ya lo atendió otro, se cierran sus diálogos.
                    approve = st.approve?.takeIf { a -> r.data.any { it.paymentId == a.paymentId } },
                    viewer = st.viewer?.takeIf { v -> r.data.any { it.paymentId == v.paymentId } },
                    reject = st.reject?.takeIf { d -> r.data.any { it.paymentId == d.item.paymentId } },
                )
            }
            is AppResult.Error -> _state.update { it.copy(loading = false, loadError = r.message) }
            AppResult.Loading -> Unit
        }
    }

    // ---- ver la captura
    fun openProof(item: PaymentReview) = _state.update { it.copy(viewer = item) }
    fun closeProof() = _state.update { it.copy(viewer = null) }

    // ---- validar
    fun askApprove(item: PaymentReview) = _state.update { it.copy(approve = item) }
    fun dismissApprove() = _state.update { if (it.approveBusy) it else it.copy(approve = null) }

    fun confirmApprove() {
        val item = _state.value.approve ?: return
        if (_state.value.approveBusy) return
        _state.update { it.copy(approveBusy = true) }
        scope.launch {
            when (val r = repository.approve(item.paymentId)) {
                is AppResult.Success -> {
                    val note = runCatching { printReceipt(r.data.printData) }.getOrNull()
                    val sale = if (r.data.saleNumber.isNotBlank()) " Comprobante ${r.data.saleNumber}." else ""
                    _messages.tryEmit("Pago validado: el pedido #${item.orderCode} pasó a cocina.$sale${note?.let { " $it" }.orEmpty()}")
                    _state.update { it.copy(approve = null, approveBusy = false) }
                    load()
                }
                is AppResult.Error -> {
                    _messages.tryEmit(r.message)
                    _state.update { it.copy(approve = null, approveBusy = false) }
                    load()
                }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- rechazar
    fun openReject(item: PaymentReview) = _state.update { it.copy(reject = RejectPaymentDialogState(item)) }
    fun setRejectReason(text: String) = _state.update { st -> st.copy(reject = st.reject?.copy(reason = text.take(255), error = null)) }
    fun dismissReject() = _state.update { if (it.reject?.busy == true) it else it.copy(reject = null) }

    fun confirmReject() {
        val d = _state.value.reject ?: return
        if (d.busy) return
        if (!ManualPaymentRules.isRejectReasonValid(d.reason)) {
            _state.update { it.copy(reject = d.copy(error = "Indica por qué rechazas el pago (mínimo ${ManualPaymentRules.MIN_REJECT_REASON} caracteres).")) }
            return
        }
        _state.update { it.copy(reject = d.copy(busy = true, error = null)) }
        scope.launch {
            when (val r = repository.reject(d.item.paymentId, d.reason.trim())) {
                is AppResult.Success -> {
                    _messages.tryEmit("Pago rechazado. El cliente verá el motivo y podrá subir otra captura.")
                    _state.update { it.copy(reject = null) }
                    load()
                }
                is AppResult.Error -> {
                    _messages.tryEmit(r.message)
                    _state.update { it.copy(reject = it.reject?.copy(busy = false, error = r.message)) }
                    load()
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
