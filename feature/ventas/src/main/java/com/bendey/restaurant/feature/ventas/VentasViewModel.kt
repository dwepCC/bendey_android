package com.bendey.restaurant.feature.ventas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.data.receipt.ReceiptPdfService
import com.bendey.restaurant.core.domain.billing.BillingDocumentKind
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.canCancelNotaVenta
import com.bendey.restaurant.core.domain.sales.canIssueElectronicFromNota
import com.bendey.restaurant.core.domain.sales.canVoidWithCreditNote
import com.bendey.restaurant.core.domain.permission.RestaurantFeature
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.billing.BillingEventsClient
import com.bendey.restaurant.core.realtime.billing.BillingStatusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class VoidAction {
    CREDIT_NOTE,
    CANCEL_NOTA,
}

enum class VentasDatePreset(val label: String) {
    TODAY("Hoy"),
    WEEK("7 días"),
    MONTH("Mes"),
    CUSTOM("Personalizado"),
}

data class VentasUiState(
    val loading: Boolean = false,
    val tab: VentasTab = VentasTab.NOTAS,
    val sales: List<com.bendey.restaurant.core.domain.sales.SaleSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val searchQuery: String = "",
    val fromDate: String = currentMonthStart(),
    val toDate: String = todayPeru(),
    val datePreset: VentasDatePreset = VentasDatePreset.MONTH,
    val paymentMethodFilter: String = "",
    val billingStatusFilter: String = "",
    val checkoutMeta: CheckoutMeta? = null,
    val sunatEnabled: Boolean = false,
    val selectedSaleId: Int? = null,
    val detailLoading: Boolean = false,
    val detail: SaleDetail? = null,
    val detailPrinting: Boolean = false,
    val billingBusy: String? = null,
    val voidDialogOpen: Boolean = false,
    val voidAction: VoidAction? = null,
    val voidReason: String = "",
    val voidSubmitting: Boolean = false,
    val emitDialogOpen: Boolean = false,
    val emitDocKind: String = "03",
    val emitSeriesId: Int? = null,
    val emitIssueDate: String = todayPeru(),
    val emitSubmitting: Boolean = false,
    val receiptModalOpen: Boolean = false,
    val receiptPrintData: SalePrintData? = null,
    val receiptSaleNumber: String = "",
    val receiptTotal: Double = 0.0,
    val receiptHasPrinter: Boolean = false,
    val receiptBusy: String? = null,
    val error: String? = null,
    val snackMessage: String? = null,
    val exportBusy: String? = null,
    val xmlViewOpen: Boolean = false,
    val xmlViewTitle: String = "",
    val xmlViewContent: String = "",
    val listSummary: com.bendey.restaurant.core.domain.sales.SaleListSummary = com.bendey.restaurant.core.domain.sales.SaleListSummary(),
) {
    val canFetchList: Boolean
        get() = tab == VentasTab.NOTAS || sunatEnabled
    val hasMore: Boolean get() = sales.size < total

    val electronicSeries: List<DocumentSeries>
        get() = checkoutMeta?.series.orEmpty().filter {
            val code = it.sunatCode?.trim().orEmpty()
            code == "01" || code == "03"
        }

    val filteredEmitSeries: List<DocumentSeries>
        get() = electronicSeries.filter { it.sunatCode?.trim() == emitDocKind }
}

@HiltViewModel
class VentasViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val billingRepository: BillingRepository,
    private val documentPrintService: DocumentPrintService,
    private val receiptPdfService: ReceiptPdfService,
    private val fileShareService: BendeyFileShareService,
    private val billingEventsClient: BillingEventsClient,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            billingEventsClient.events.collect(::applyBillingEvent)
        }
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .map { session ->
                    val perms = session?.restaurantPermissions.orEmpty()
                    val et = session?.user?.employeeType
                    RestaurantPermissions.canAccessFeature(perms, RestaurantFeature.VENTAS, et)
                }
                .distinctUntilChanged()
                .collect { canAccess ->
                    if (canAccess) {
                        warmCheckoutMeta()
                        syncBillingStream(_uiState.value.tab)
                        refresh()
                    } else {
                        _uiState.update { it.copy(loading = false, sales = emptyList(), error = null) }
                    }
                }
        }
    }

    override fun onCleared() {
        billingEventsClient.disconnect()
        super.onCleared()
    }

    fun selectTab(tab: VentasTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update {
            it.copy(
                tab = tab,
                page = 1,
                sales = emptyList(),
                billingStatusFilter = "",
                paymentMethodFilter = "",
            )
        }
        syncBillingStream(tab)
        refresh()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, page = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            refresh()
        }
    }

    fun setDatePreset(preset: VentasDatePreset) {
        val today = LocalDate.now()
        val formatter = dateFormatter
        val (from, to) = when (preset) {
            VentasDatePreset.TODAY -> today to today
            VentasDatePreset.WEEK -> today.minusDays(6) to today
            VentasDatePreset.MONTH -> today.withDayOfMonth(1) to today
            VentasDatePreset.CUSTOM -> {
                val state = _uiState.value
                LocalDate.parse(state.fromDate) to LocalDate.parse(state.toDate)
            }
        }
        _uiState.update {
            it.copy(
                datePreset = preset,
                fromDate = from.format(formatter),
                toDate = to.format(formatter),
                page = 1,
            )
        }
        if (preset != VentasDatePreset.CUSTOM) refresh()
    }

    fun setFromDate(value: String) {
        _uiState.update { it.copy(fromDate = value, datePreset = VentasDatePreset.CUSTOM, page = 1) }
        refresh()
    }

    fun setToDate(value: String) {
        _uiState.update { it.copy(toDate = value, datePreset = VentasDatePreset.CUSTOM, page = 1) }
        refresh()
    }

    fun setPaymentMethodFilter(code: String) {
        _uiState.update { it.copy(paymentMethodFilter = code, page = 1) }
        refresh()
    }

    fun setBillingStatusFilter(status: String) {
        _uiState.update { it.copy(billingStatusFilter = status, page = 1) }
        refresh()
    }

    private fun syncBillingStream(tab: VentasTab) {
        if (tab == VentasTab.FACTURACION || tab == VentasTab.CREDITOS) {
            billingEventsClient.connect()
        } else {
            billingEventsClient.disconnect()
        }
    }

    private fun warmCheckoutMeta() {
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            when (val result = billingRepository.loadCheckoutMeta(branchId)) {
                is AppResult.Success -> {
                    val hasPrinter = documentPrintService.hasConfiguredPrinter()
                    _uiState.update {
                        it.copy(
                            checkoutMeta = result.data,
                            sunatEnabled = result.data.sunatEnabled,
                            receiptHasPrinter = hasPrinter,
                        )
                    }
                }
                else -> Unit
            }
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
            if (!state.canFetchList) {
                _uiState.update {
                    it.copy(loading = false, sales = emptyList(), total = 0, listSummary = com.bendey.restaurant.core.domain.sales.SaleListSummary())
                }
                return@launch
            }
            _uiState.update { it.copy(loading = true, error = null) }
            when (
                val result = salesRepository.listSales(
                    from = state.fromDate,
                    to = state.toDate,
                    tab = state.tab,
                    page = 1,
                    perPage = 30,
                    query = state.searchQuery,
                    paymentMethod = state.paymentMethodFilter,
                    billingStatus = state.billingStatusFilter,
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        sales = result.data.sales,
                        total = result.data.total,
                        listSummary = result.data.summary,
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
        if (state.loading || !state.hasMore || !state.canFetchList) return
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
                    query = state.searchQuery,
                    paymentMethod = state.paymentMethodFilter,
                    billingStatus = state.billingStatusFilter,
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        sales = it.sales + result.data.sales,
                        total = result.data.total,
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
                    syncBillingStream(VentasTab.CREDITOS)
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

    fun openEmitElectronic() {
        val detail = _uiState.value.detail ?: return
        if (!detail.canIssueElectronicFromNota(_uiState.value.sunatEnabled)) {
            _uiState.update { it.copy(snackMessage = "Esta nota no puede convertirse a comprobante") }
            return
        }
        val defaultSeries = _uiState.value.electronicSeries.firstOrNull { it.sunatCode?.trim() == "03" }
            ?: _uiState.value.electronicSeries.firstOrNull()
        _uiState.update {
            it.copy(
                emitDialogOpen = true,
                emitDocKind = defaultSeries?.sunatCode?.trim() ?: "03",
                emitSeriesId = defaultSeries?.id,
                emitIssueDate = todayPeru(),
                error = null,
            )
        }
    }

    fun dismissEmitDialog() {
        if (_uiState.value.emitSubmitting) return
        _uiState.update { it.copy(emitDialogOpen = false, error = null) }
    }

    fun setEmitDocKind(kind: String) {
        val series = _uiState.value.electronicSeries.filter { it.sunatCode?.trim() == kind }
        _uiState.update {
            it.copy(
                emitDocKind = kind,
                emitSeriesId = series.firstOrNull()?.id,
            )
        }
    }

    fun setEmitSeriesId(seriesId: Int) {
        _uiState.update { it.copy(emitSeriesId = seriesId) }
    }

    fun setEmitIssueDate(date: String) {
        _uiState.update { it.copy(emitIssueDate = date) }
    }

    fun confirmEmitElectronic() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val seriesId = state.emitSeriesId
        if (seriesId == null) {
            _uiState.update { it.copy(error = "Seleccione una serie") }
            return
        }
        if (state.emitDocKind == "01" && detail.contact?.hasValidRuc() != true) {
            _uiState.update { it.copy(error = "Para factura el cliente debe tener RUC (11 dígitos)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(emitSubmitting = true, error = null) }
            when (
                val result = salesRepository.issueElectronicFromNota(
                    saleId = detail.id,
                    seriesId = seriesId,
                    issueDate = state.emitIssueDate,
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            emitSubmitting = false,
                            emitDialogOpen = false,
                            selectedSaleId = null,
                            detail = null,
                            snackMessage = result.data.message
                                ?: "Comprobante generado · envíelo a SUNAT desde Facturación",
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(emitSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun sendToSunat() {
        val saleId = _uiState.value.detail?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(billingBusy = "send", error = null) }
            when (val result = billingRepository.sendToSunat(saleId)) {
                is AppResult.Success -> {
                    val status = result.data.billingStatus
                    _uiState.update { state ->
                        state.copy(
                            billingBusy = null,
                            snackMessage = result.data.message ?: "Enviado a SUNAT",
                            detail = state.detail?.copy(billingStatus = status ?: state.detail.billingStatus),
                            sales = state.sales.map { sale ->
                                if (sale.id == saleId) sale.copy(billingStatus = status ?: sale.billingStatus) else sale
                            },
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(billingBusy = null, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun resendToSunat() {
        val saleId = _uiState.value.detail?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(billingBusy = "resend", error = null) }
            when (val result = billingRepository.resendToSunat(saleId)) {
                is AppResult.Success -> {
                    val status = result.data.billingStatus
                    _uiState.update { state ->
                        state.copy(
                            billingBusy = null,
                            snackMessage = result.data.message ?: "Reenviado a SUNAT",
                            detail = state.detail?.copy(billingStatus = status ?: state.detail.billingStatus),
                            sales = state.sales.map { sale ->
                                if (sale.id == saleId) sale.copy(billingStatus = status ?: sale.billingStatus) else sale
                            },
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(billingBusy = null, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openOfficialSunatPdf(context: Context) {
        downloadBillingDocument(context, BillingDocumentKind.PDF, openPdf = true)
    }

    fun downloadXmlSent(context: Context) {
        downloadBillingDocument(context, BillingDocumentKind.XML, openPdf = false)
    }

    fun downloadXmlGenerated(context: Context) {
        downloadBillingDocument(context, BillingDocumentKind.XML_GENERATED, openPdf = false)
    }

    fun downloadCdr(context: Context) {
        downloadBillingDocument(context, BillingDocumentKind.CDR, openPdf = false)
    }

    fun viewXmlSent() {
        viewBillingXml(BillingDocumentKind.XML, "XML enviado")
    }

    fun viewXmlGenerated() {
        viewBillingXml(BillingDocumentKind.XML_GENERATED, "XML generado")
    }

    fun dismissXmlView() {
        _uiState.update { it.copy(xmlViewOpen = false, xmlViewContent = "", xmlViewTitle = "") }
    }

    private fun viewBillingXml(kind: BillingDocumentKind, title: String) {
        val saleId = _uiState.value.detail?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(billingBusy = kind.pathSegment) }
            when (val result = billingRepository.loadBillingDocumentText(saleId, kind)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        billingBusy = null,
                        xmlViewOpen = true,
                        xmlViewTitle = title,
                        xmlViewContent = result.data,
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(billingBusy = null, snackMessage = result.message ?: "XML no disponible")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun downloadBillingDocument(context: Context, kind: BillingDocumentKind, openPdf: Boolean) {
        val saleId = _uiState.value.detail?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(billingBusy = kind.pathSegment) }
            when (val result = billingRepository.downloadBillingDocument(saleId, kind)) {
                is AppResult.Success -> {
                    val shareResult = withContext(Dispatchers.Main) {
                        if (openPdf) {
                            fileShareService.openPdf(context, result.data)
                        } else {
                            shareBillingDocumentFile(
                                context,
                                fileShareService,
                                result.data,
                                kind.mimeType,
                                kind.defaultFileName,
                            )
                        }
                    }
                    _uiState.update {
                        when (shareResult) {
                            ExportShareResult.Success -> it.copy(
                                billingBusy = null,
                                snackMessage = when (kind) {
                                    BillingDocumentKind.PDF -> "PDF oficial abierto"
                                    BillingDocumentKind.CDR -> "CDR listo para compartir"
                                    else -> "XML listo para compartir"
                                },
                            )
                            is ExportShareResult.Failure -> it.copy(
                                billingBusy = null,
                                snackMessage = shareResult.userMessage,
                            )
                        }
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(billingBusy = null, snackMessage = result.message ?: "Documento no disponible")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun exportListPdf(context: Context) {
        exportList(context, format = "pdf")
    }

    fun exportListExcel(context: Context) {
        exportList(context, format = "excel")
    }

    private fun exportList(context: Context, format: String) {
        val state = _uiState.value
        if (!state.canFetchList) {
            _uiState.update { it.copy(snackMessage = "Facturación electrónica no habilitada") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(exportBusy = format, error = null) }
            when (
                val result = salesRepository.listAllSalesForExport(
                    from = state.fromDate,
                    to = state.toDate,
                    tab = state.tab,
                    query = state.searchQuery,
                    paymentMethod = state.paymentMethodFilter,
                    billingStatus = state.billingStatusFilter,
                )
            ) {
                is AppResult.Success -> {
                    val shareResult = withContext(Dispatchers.Main) {
                        if (format == "pdf") {
                            exportSalesListPdf(context, fileShareService, state.tab, result.data, state.fromDate, state.toDate)
                        } else {
                            exportSalesListCsv(context, fileShareService, state.tab, result.data, state.fromDate, state.toDate)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            exportBusy = null,
                            snackMessage = when (shareResult) {
                                ExportShareResult.Success ->
                                    if (format == "pdf") "Listado PDF exportado" else "Listado Excel exportado"
                                is ExportShareResult.Failure -> shareResult.userMessage
                            },
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(exportBusy = null, snackMessage = result.message ?: "No se pudo exportar")
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

    fun openReceiptModal() {
        val detail = _uiState.value.detail ?: return
        val printData = detail.printData ?: run {
            _uiState.update { it.copy(snackMessage = "Sin datos para generar PDF") }
            return
        }
        viewModelScope.launch {
            val hasPrinter = documentPrintService.hasConfiguredPrinter()
            _uiState.update {
                it.copy(
                    receiptModalOpen = true,
                    receiptPrintData = printData,
                    receiptSaleNumber = detail.displayNumber,
                    receiptTotal = detail.total,
                    receiptHasPrinter = hasPrinter,
                )
            }
        }
    }

    fun dismissReceiptModal() {
        _uiState.update {
            it.copy(receiptModalOpen = false, receiptPrintData = null, receiptBusy = null)
        }
    }

    fun printFromReceiptModal() {
        val data = _uiState.value.receiptPrintData ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(receiptBusy = "print") }
            val ok = documentPrintService.printSaleDocument(data, force = true)
            _uiState.update {
                it.copy(
                    receiptBusy = null,
                    snackMessage = when (ok) {
                        true -> "Comprobante enviado a la ticketera"
                        false -> "No se pudo imprimir"
                        null -> "Configura impresora en Ajustes"
                    },
                )
            }
        }
    }

    fun shareReceiptViaWhatsApp(context: Context) {
        val data = _uiState.value.receiptPrintData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(receiptBusy = "share") }
            try {
                val file = receiptPdfService.generate(data, ReceiptPdfFormat.TICKET)
                val shareResult = withContext(Dispatchers.Main) {
                    fileShareService.sharePdfWhatsApp(context, file, "Comprobante ${data.number}")
                }
                if (shareResult is ExportShareResult.Failure) {
                    _uiState.update { it.copy(snackMessage = shareResult.userMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = fileShareService.failureFrom(e).userMessage) }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
            }
        }
    }

    fun openReceiptPdf(context: Context, format: ReceiptPdfFormat) {
        val data = _uiState.value.receiptPrintData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(receiptBusy = "pdf") }
            try {
                val file = receiptPdfService.generate(data, format)
                val shareResult = withContext(Dispatchers.Main) {
                    fileShareService.openPdf(context, file)
                }
                if (shareResult is ExportShareResult.Failure) {
                    _uiState.update { it.copy(snackMessage = shareResult.userMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = fileShareService.failureFrom(e).userMessage) }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
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
