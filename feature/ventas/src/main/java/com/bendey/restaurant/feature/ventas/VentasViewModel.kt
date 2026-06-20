package com.bendey.restaurant.feature.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.canCancelNotaVenta
import com.bendey.restaurant.core.domain.sales.canVoidWithCreditNote
import com.bendey.restaurant.core.realtime.billing.BillingEventsClient
import com.bendey.restaurant.core.realtime.billing.BillingStatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class VoidAction {
    CREDIT_NOTE,
    CANCEL_NOTA,
}

data class VentasUiState(
    val loading: Boolean = false,
    val tab: VentasTab = VentasTab.NOTAS,
    val sales: List<SaleSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val fromDate: String = currentMonthStart(),
    val toDate: String = todayPeru(),
    val selectedSaleId: Int? = null,
    val detailLoading: Boolean = false,
    val detail: SaleDetail? = null,
    val detailPrinting: Boolean = false,
    val voidDialogOpen: Boolean = false,
    val voidAction: VoidAction? = null,
    val voidReason: String = "",
    val voidSubmitting: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val hasMore: Boolean get() = sales.size < total
}

@HiltViewModel
class VentasViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val billingRepository: BillingRepository,
    private val documentPrintService: DocumentPrintService,
    private val billingEventsClient: BillingEventsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            billingEventsClient.events.collect(::applyBillingEvent)
        }
        syncBillingStream(_uiState.value.tab)
        refresh()
    }

    override fun onCleared() {
        billingEventsClient.disconnect()
        super.onCleared()
    }

    fun selectTab(tab: VentasTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update { it.copy(tab = tab, page = 1, sales = emptyList()) }
        syncBillingStream(tab)
        refresh()
    }

    private fun syncBillingStream(tab: VentasTab) {
        if (tab == VentasTab.FACTURACION || tab == VentasTab.CREDITOS) {
            billingEventsClient.connect()
        } else {
            billingEventsClient.disconnect()
        }
    }

    private fun applyBillingEvent(event: BillingStatusEvent) {
        _uiState.update { state ->
            state.copy(
                sales = state.sales.map { sale ->
                    if (sale.id == event.saleId) sale.copy(billingStatus = event.status) else sale
                },
                detail = state.detail?.takeIf { it.id == event.saleId }?.copy(
                    billingStatus = event.status,
                ) ?: state.detail,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(loading = true, error = null) }
            when (
                val result = salesRepository.listSales(
                    from = state.fromDate,
                    to = state.toDate,
                    tab = state.tab,
                    page = 1,
                    perPage = 30,
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        sales = result.data.first,
                        total = result.data.second,
                        page = 1,
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val nextPage = state.page + 1
            when (
                val result = salesRepository.listSales(
                    from = state.fromDate,
                    to = state.toDate,
                    tab = state.tab,
                    page = nextPage,
                    perPage = 30,
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        sales = it.sales + result.data.first,
                        total = result.data.second,
                        page = nextPage,
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openSaleDetail(saleId: Int) {
        _uiState.update { it.copy(selectedSaleId = saleId, detail = null, detailLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = salesRepository.getSaleDetail(saleId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(detailLoading = false, detail = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(detailLoading = false, error = result.message, selectedSaleId = null)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissSaleDetail() {
        _uiState.update { it.copy(selectedSaleId = null, detail = null, detailLoading = false) }
    }

    fun openVoidCreditNote() {
        _uiState.update {
            it.copy(voidDialogOpen = true, voidAction = VoidAction.CREDIT_NOTE, voidReason = "", error = null)
        }
    }

    fun openCancelNota() {
        _uiState.update {
            it.copy(voidDialogOpen = true, voidAction = VoidAction.CANCEL_NOTA, voidReason = "", error = null)
        }
    }

    fun dismissVoidDialog() {
        if (_uiState.value.voidSubmitting) return
        _uiState.update { it.copy(voidDialogOpen = false, voidAction = null, voidReason = "") }
    }

    fun setVoidReason(reason: String) {
        _uiState.update { it.copy(voidReason = reason) }
    }

    fun confirmVoid() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val reason = state.voidReason.trim()
        if (reason.isBlank()) {
            _uiState.update { it.copy(error = "Indique el motivo de anulación") }
            return
        }
        when (state.voidAction) {
            VoidAction.CREDIT_NOTE -> {
                if (!detail.canVoidWithCreditNote()) {
                    _uiState.update { it.copy(error = "Esta venta no puede anularse con nota de crédito") }
                    return
                }
                submitVoidCreditNote(detail.id, reason)
            }
            VoidAction.CANCEL_NOTA -> {
                if (!detail.canCancelNotaVenta()) {
                    _uiState.update { it.copy(error = "Esta nota no puede anularse") }
                    return
                }
                submitCancelNota(detail.id, reason)
            }
            null -> Unit
        }
    }

    private fun submitVoidCreditNote(saleId: Int, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(voidSubmitting = true, error = null) }
            when (val result = billingRepository.voidWithCreditNote(saleId, reason)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            voidSubmitting = false,
                            voidDialogOpen = false,
                            voidAction = null,
                            voidReason = "",
                            selectedSaleId = null,
                            detail = null,
                            tab = VentasTab.CREDITOS,
                            snackMessage = result.data.message ?: "Nota de crédito encolada",
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voidSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun submitCancelNota(saleId: Int, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(voidSubmitting = true, error = null) }
            when (val result = salesRepository.cancelNotaVenta(saleId, reason)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            voidSubmitting = false,
                            voidDialogOpen = false,
                            voidAction = null,
                            voidReason = "",
                            selectedSaleId = null,
                            detail = null,
                            snackMessage = result.data.message ?: "Nota de venta anulada",
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voidSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun reprintSelectedSale() {
        val detail = _uiState.value.detail ?: return
        val printData = detail.printData ?: run {
            _uiState.update { it.copy(snackMessage = "Esta venta no tiene datos de impresión") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(detailPrinting = true) }
            val printed = documentPrintService.printSaleDocument(printData, force = true)
            _uiState.update {
                it.copy(
                    detailPrinting = false,
                    snackMessage = when (printed) {
                        true -> "Documento reimpreso"
                        false -> "Error al reimprimir"
                        null -> "Configura impresora de documentos"
                    },
                )
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun todayPeru(): String = LocalDate.now().format(dateFormatter)

private fun currentMonthStart(): String = LocalDate.now().withDayOfMonth(1).format(dateFormatter)
