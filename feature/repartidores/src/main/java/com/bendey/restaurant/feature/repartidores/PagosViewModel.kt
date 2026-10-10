package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.PaymentReview
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Pegamento Hilt de la bandeja Pagos por validar; la lógica está en [PagosPresenter]. */
@HiltViewModel
class PagosViewModel @Inject constructor(
    repository: ManualPaymentRepository,
    private val documentPrintService: DocumentPrintService,
    private val boardStore: DeliveryBoardStore,
    productImageRepository: ProductImageRepository,
) : ViewModel() {

    private val presenter = PagosPresenter(viewModelScope, repository) { data ->
        documentPrintService.printSaleDocument(data)?.message
    }

    val state: StateFlow<PagosUiState> = presenter.state
    val messages: SharedFlow<String> = presenter.messages

    /** Origen público de /storage para armar la URL de la captura. */
    val assetsBaseUrl: String? = productImageRepository.tenantAssetsBaseUrl()

    private var started = false

    /** Quien puede validar (o.ch) arranca la bandeja; el repartidor y el mozo nunca la consultan. */
    fun start() {
        if (started) return
        started = true
        presenter.refresh()
        // delivery.board.updated (reason payment_review) refresca el tablero: cada consulta nueva recarga la bandeja.
        viewModelScope.launch {
            boardStore.state.map { it.fetchedAtMs }.distinctUntilChanged().drop(1).collect { presenter.refresh() }
        }
    }

    fun refresh() = presenter.refresh()
    fun openProof(item: PaymentReview) = presenter.openProof(item)
    fun closeProof() = presenter.closeProof()
    fun askApprove(item: PaymentReview) = presenter.askApprove(item)
    fun dismissApprove() = presenter.dismissApprove()
    fun confirmApprove() = presenter.confirmApprove()
    fun openReject(item: PaymentReview) = presenter.openReject(item)
    fun setRejectReason(text: String) = presenter.setRejectReason(text)
    fun dismissReject() = presenter.dismissReject()
    fun confirmReject() = presenter.confirmReject()
}
