package com.bendey.restaurant.feature.ventas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.saleStatusAccentColor
import com.bendey.restaurant.core.domain.billing.ContactBrief
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.billing.exceedsSunatMaxMontoSinRuc
import com.bendey.restaurant.core.domain.billing.sunatMaxMontoSinRucMessage
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.domain.billing.isFacturaDocType
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.domain.sales.BILLING_FILTER_STATUSES
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleListSummary
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.atencionConMesa
import com.bendey.restaurant.core.domain.sales.incluyeElectronicos
import com.bendey.restaurant.core.domain.sales.isCreditNoteDoc
import com.bendey.restaurant.core.domain.sales.isNotaVenta
import com.bendey.restaurant.core.domain.sales.TipoDeAtencion
import com.bendey.restaurant.core.domain.sales.billingStatusLabel
import com.bendey.restaurant.core.domain.sales.canCancelNotaVenta
import com.bendey.restaurant.core.domain.sales.canRegisterRefund
import com.bendey.restaurant.core.domain.sales.isRefunded
import com.bendey.restaurant.core.domain.sales.canIssueElectronicFromNota
import com.bendey.restaurant.core.domain.sales.canResendToSunat
import com.bendey.restaurant.core.domain.sales.sunatSendActionLabel
import com.bendey.restaurant.core.domain.sales.canSendToSunat
import com.bendey.restaurant.core.domain.sales.canShowCdr
import com.bendey.restaurant.core.domain.sales.canShowXmlGenerated
import com.bendey.restaurant.core.domain.sales.canShowXmlSent
import com.bendey.restaurant.core.domain.sales.canVoidWithCreditNote
import com.bendey.restaurant.core.domain.sales.convertedToLabel
import com.bendey.restaurant.core.domain.sales.isConverted
import com.bendey.restaurant.core.domain.sales.notaVentaListStatusLabel
import com.bendey.restaurant.core.domain.sales.saleStatusDisplayLabel
import com.bendey.restaurant.core.ui.subscription.BendeyExportActionsRow
import com.bendey.restaurant.core.ui.checkout.ReceiptPdfFormatUi
import com.bendey.restaurant.core.ui.checkout.ReceiptPrintModal
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.SalesPaymentSummaryRow
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: VentasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val context = LocalContext.current
    val listState = rememberLazyListState()

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    LaunchedEffect(listState, state.hasMore, state.loading) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && state.hasMore && !state.loading) viewModel.loadMore()
            }
    }

    // El aviso de «en vivo» corresponde a cualquier filtro que traiga comprobantes: el flujo de
    // eventos se conecta por capacidad, no por pestaña.
    val billingHint = if (state.tab.incluyeElectronicos() && state.sunatEnabled) " · SUNAT en vivo" else ""
    val detailError = if (state.voidDialogOpen || state.emitDialogOpen) null else state.error

    BendeyListScreenLayout(
            modifier = modifier.fillMaxSize(),
            isRefreshing = state.loading && state.sales.isEmpty(),
            onRefresh = viewModel::refresh,
            header = {
                BendeyScreenToolbar(
                    title = "Ventas",
                    subtitle = "${state.total} registros$billingHint",
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    },
                )
                VentasTabRow(
                    selected = state.tab,
                    sunatEnabled = state.sunatEnabled,
                    onSelect = viewModel::selectTab,
                )
            },
        ) { contentModifier ->
            VentasSalesList(
                state = state,
                listState = listState,
                currency = currency,
                selectedSaleId = state.selectedSaleId,
                onSaleClick = viewModel::openSaleDetail,
                modifier = contentModifier,
                // Filtros + resumen ya NO son header fijo (ocupaban toda la pantalla y tapaban la lista):
                // van como primer ítem del LazyColumn para que TODO scrollee junto y la lista sea
                // alcanzable. Las pestañas (Todas/Notas/Boletas) siguen fijas arriba.
                leadingContent = {
                    VentasFiltersSection(
                        state = state,
                        paymentMethods = state.checkoutMeta?.paymentMethods.orEmpty(),
                        onSearchChange = viewModel::setSearchQuery,
                        onDatePreset = viewModel::setDatePreset,
                        onFromDateChange = viewModel::setFromDate,
                        onToDateChange = viewModel::setToDate,
                        onPaymentMethodChange = viewModel::setPaymentMethodFilter,
                        onOrderTypeChange = viewModel::selectOrderType,
                        onBillingStatusChange = viewModel::setBillingStatusFilter,
                        onExportPdf = { viewModel.exportListPdf(context) },
                        onExportExcel = { viewModel.exportListExcel(context) },
                        onLockedExportClick = onNavigateToSubscription,
                    )
                    VentasListHintsAndSummary(
                        state = state,
                        paymentMethods = state.checkoutMeta?.paymentMethods.orEmpty(),
                        currency = currency,
                    )
                },
            )
        }

    if (state.selectedSaleId != null) {
        BendeyBottomSheet(
            onDismissRequest = viewModel::dismissSaleDetail,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            BendeyVerticalScrollColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SaleDetailContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
                sunatEnabled = state.sunatEnabled,
                loading = state.detailLoading,
                detail = state.detail,
                printing = state.detailPrinting,
                billingBusy = state.billingBusy,
                currency = currency,
                error = detailError,
                onReprint = viewModel::reprintSelectedSale,
                onOpenPdf = viewModel::openReceiptModal,
                onVoidCreditNote = viewModel::openVoidCreditNote,
                onCancelNota = viewModel::openCancelNota,
                onRefund = viewModel::openRefund,
                onEmitElectronic = viewModel::openEmitElectronic,
                onSendSunat = viewModel::sendToSunat,
                onResendSunat = viewModel::resendToSunat,
                onDownloadPdf = { viewModel.downloadReceiptPdf(ReceiptPdfFormat.A4) },
                onViewXmlSent = viewModel::viewXmlSent,
                onViewXmlGenerated = viewModel::viewXmlGenerated,
                onDownloadXmlSent = { viewModel.downloadXmlSent(context) },
                onDownloadXmlGenerated = { viewModel.downloadXmlGenerated(context) },
                onDownloadCdr = { viewModel.downloadCdr(context) },
                )
            }
        }
    }

    if (state.voidDialogOpen) {
        VoidReasonDialog(
            action = state.voidAction,
            refundAmount = state.detail?.refundableAmount ?: 0.0,
            reason = state.voidReason,
            pin = state.voidPin,
            loading = state.voidSubmitting,
            error = state.error,
            onReasonChange = viewModel::setVoidReason,
            onPinChange = viewModel::setVoidPin,
            onDismiss = viewModel::dismissVoidDialog,
            onConfirm = viewModel::confirmVoid,
        )
    }

    if (state.emitDialogOpen) {
        val emitMeta = state.emitCheckoutMeta ?: state.checkoutMeta
        val emitContacts = emitMeta?.contacts.orEmpty()
        val selectedEmitContact = state.emitContactId?.let { id -> emitContacts.firstOrNull { it.id == id } }
        val sunatLimitWarning = if (
            state.emitDocKind == "03" &&
            selectedEmitContact != null &&
            exceedsSunatMaxMontoSinRuc(state.detail?.total ?: 0.0, selectedEmitContact, state.emitDocKind)
        ) {
            sunatMaxMontoSinRucMessage(state.detail?.total ?: 0.0)
        } else {
            null
        }
        EmitElectronicDialog(
            docKind = state.emitDocKind,
            seriesId = state.emitSeriesId,
            issueDate = state.emitIssueDate,
            series = state.filteredEmitSeries,
            contacts = emitContacts,
            contactId = state.emitContactId,
            metaLoading = state.emitMetaLoading,
            sunatLimitWarning = sunatLimitWarning,
            loading = state.emitSubmitting,
            error = state.error,
            onDocKindChange = viewModel::setEmitDocKind,
            onSeriesChange = viewModel::setEmitSeriesId,
            onIssueDateChange = viewModel::setEmitIssueDate,
            onContactChange = viewModel::setEmitContactId,
            onAddClient = viewModel::openEmitClientForm,
            onDismiss = viewModel::dismissEmitDialog,
            onConfirm = viewModel::confirmEmitElectronic,
        )
    }

    if (state.emitClientFormOpen) {
        EmitClientQuickAddDialog(
            form = state.emitClientForm,
            consulting = state.emitClientConsulting,
            saving = state.emitClientSaving,
            error = state.emitClientError,
            onFormChange = viewModel::updateEmitClientForm,
            onConsult = viewModel::consultEmitClientDoc,
            onDismiss = viewModel::dismissEmitClientForm,
            onSave = viewModel::saveEmitClient,
        )
    }

    ReceiptPrintModal(
        open = state.receiptModalOpen,
        printData = state.receiptPrintData,
        saleNumber = state.receiptSaleNumber,
        total = state.receiptTotal,
        hasPrinter = state.receiptHasPrinter,
        busyAction = state.receiptBusy,
        onPrint = viewModel::printFromReceiptModal,
        onShareWhatsApp = { viewModel.shareReceiptViaWhatsApp(context) },
        onOpenPdf = { format ->
            val pdfFormat = when (format) {
                ReceiptPdfFormatUi.TICKET -> ReceiptPdfFormat.TICKET
                ReceiptPdfFormatUi.A4 -> ReceiptPdfFormat.A4
            }
            viewModel.openReceiptPdf(context, pdfFormat)
        },
        onDismiss = viewModel::dismissReceiptModal,
    )

    if (state.xmlViewOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissXmlView,
            title = { Text(state.xmlViewTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(state.xmlViewContent, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissXmlView) { Text("Cerrar") }
            },
        )
    }
}

@Composable
private fun VentasTabRow(
    selected: VentasTab,
    sunatEnabled: Boolean,
    onSelect: (VentasTab) -> Unit,
) {
    BendeyHorizontalScrollRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = BendeySpacing.md,
            vertical = BendeySpacing.xs,
        ),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        VentasTab.entries.filter { tab ->
            tab != VentasTab.CREDITOS || sunatEnabled
        }.forEach { tab ->
            BendeyFilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = tab.label,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VentasFiltersSection(
    state: VentasUiState,
    paymentMethods: List<PaymentMethodOption>,
    onSearchChange: (String) -> Unit,
    onDatePreset: (VentasDatePreset) -> Unit,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onOrderTypeChange: (String) -> Unit,
    onBillingStatusChange: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onLockedExportClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        // EL BUSCADOR, SIEMPRE. Antes solo salia en notas de credito: en una lista que junta notas
        // de venta, boletas y facturas, encontrar un numero entre cientos deja de ser opcional.
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            label = "Buscar por número",
            modifier = Modifier.fillMaxWidth(),
        )
        BendeyHorizontalScrollRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            VentasDatePreset.entries.forEach { preset ->
                BendeyFilterChip(
                    selected = state.datePreset == preset,
                    onClick = { onDatePreset(preset) },
                    text = preset.label,
                )
            }
        }
        if (state.datePreset == VentasDatePreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyTextField(
                    value = state.fromDate,
                    onValueChange = onFromDateChange,
                    label = "Desde (AAAA-MM-DD)",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = state.toDate,
                    onValueChange = onToDateChange,
                    label = "Hasta (AAAA-MM-DD)",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FilterDropdown(
            label = "Método de pago",
            value = state.paymentMethodFilter,
            options = listOf("" to "Todos") + paymentMethods.map { it.code to it.name },
            onSelect = onPaymentMethodChange,
        )
        FilterDropdown(
            label = "Atención",
            value = state.orderTypeFilter,
            options = TipoDeAtencion.entries.map { it.codigo to it.label },
            onSelect = onOrderTypeChange,
        )
        if (state.tab.incluyeElectronicos() && state.sunatEnabled) {
            FilterDropdown(
                label = "Estado SUNAT",
                value = state.billingStatusFilter,
                options = listOf("" to "Todos") + BILLING_FILTER_STATUSES.map {
                    it to billingStatusLabel(it)
                },
                onSelect = onBillingStatusChange,
            )
        }
        if (state.canFetchList) {
            BendeyExportActionsRow(
                allowsExport = state.allowsReportExport,
                exportBusy = state.exportBusy,
                onExportPdf = onExportPdf,
                onExportExcel = onExportExcel,
                onLockedClick = onLockedExportClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second ?: label

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (code, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun VentasListHintsAndSummary(
    state: VentasUiState,
    paymentMethods: List<PaymentMethodOption>,
    currency: NumberFormat,
) {
    // «Todas» y «Solo notas de venta» funcionan sin facturación: traen notas de venta. El aviso solo
    // corresponde cuando el filtro elegido no puede devolver nada sin SUNAT.
    if (state.tab.incluyeElectronicos() && state.tab != VentasTab.TODAS && !state.sunatEnabled) {
        Text(
            "La facturación electrónica no está habilitada. Actívala en Configuración para ver boletas y facturas.",
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
    } else if (state.tab != VentasTab.CREDITOS && state.listSummary.paymentTotals.isNotEmpty()) {
        SalesPaymentSummaryRow(
            summary = state.listSummary,
            paymentMethodNames = paymentMethods.associate { it.code.lowercase() to it.name },
            currency = currency,
        )
    }
}

@Composable
private fun VentasSalesList(
    state: VentasUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    currency: NumberFormat,
    selectedSaleId: Int?,
    onSaleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
) {
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    // UN SOLO SCROLL: los filtros/resumen (leadingContent) van como primer ítem, y debajo la lista o el
    // estado vacío/error — también como ítems. Así los filtros SIEMPRE se ven (aunque no haya ventas) y la
    // lista es alcanzable scrolleando. Antes el estado vacío/error hacía return temprano y, con los filtros
    // en un header fijo, la lista nunca aparecía.
    BendeyLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = BendeySpacing.md,
            end = BendeySpacing.md,
            top = BendeySpacing.md,
            bottom = BendeySpacing.md + bottomScrollPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        item(key = "ventas-header") { leadingContent() }

        val showError = state.error != null && state.sales.isEmpty() && selectedSaleId == null
        when {
            showError -> item(key = "ventas-error") {
                Text(
                    state.error.orEmpty(),
                    color = BendeyColors.Error,
                    modifier = Modifier.fillMaxWidth().padding(BendeySpacing.md),
                )
            }
            !state.canFetchList || (state.sales.isEmpty() && !state.loading) -> item(key = "ventas-empty") {
                BendeyEmptyState(
                    title = "No hay comprobantes en esta sección",
                    inline = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                items(state.sales, key = { it.id }) { sale ->
                    SaleRow(
                        sale = sale,
                        currency = currency,
                        selected = sale.id == selectedSaleId,
                        onClick = { onSaleClick(sale.id) },
                    )
                }
                if (state.loading && state.sales.isNotEmpty()) {
                    item(key = "ventas-loading-more") {
                        Text(
                            "Cargando más…",
                            modifier = Modifier.fillMaxWidth().padding(BendeySpacing.sm),
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(
    sale: SaleSummary,
    currency: NumberFormat,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    BendeyCard(
        onClick = onClick,
        containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sale.displayNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sale.docType.ifBlank { "Comprobante" },
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(
                    sale.contactName ?: "Sin cliente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sale.issueDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                // Como se atendio: en una lista que junta notas de venta y comprobantes, saber si
                // fue mesa, delivery o mostrador es lo que le da contexto a la fila.
                Text(
                    atencionConMesa(sale.orderType, sale.tableName),
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isNotaVenta(sale.docType, sale.sunatCode) && sale.isConverted()) {
                    convertedToLabel(sale.convertedTo, sale.electronicIssueSaleId)?.let { ref ->
                        BendeyStatusChip(
                            label = "Convertida a $ref",
                            accentColor = BendeyColors.Info,
                            modifier = Modifier.padding(top = BendeySpacing.xxs),
                        )
                    }
                }
                // Anulada no implica devuelta: el dinero puede seguir en la caja. Son dos estados
                // distintos y hay que poder verlos por separado. El que importa avisar es el
                // pendiente: ese es el que termina en un faltante al cerrar la caja.
                if (sale.isRefunded()) {
                    BendeyStatusChip(
                        label = "Devuelto",
                        accentColor = BendeyColors.Error,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                if (sale.canRegisterRefund()) {
                    BendeyStatusChip(
                        label = "Pendiente de devolución",
                        accentColor = BendeyColors.Warning,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currency.format(sale.total),
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
                // EL ESTADO ES DEL DOCUMENTO, NO DEL FILTRO. Una nota de venta no va a SUNAT, asi
                // que solo se avisa si esta anulada; el comprobante si muestra en que quedo alla.
                // La nota de credito tiene estado propio y no arrastra el de la venta que anula.
                when {
                    isNotaVenta(sale.docType, sale.sunatCode) ->
                        notaVentaListStatusLabel(sale.status)?.let { label ->
                            BendeyStatusChip(label = label, accentColor = BendeyColors.Error)
                        }
                    isCreditNoteDoc(sale.docType) -> BendeyStatusChip(
                        label = saleStatusDisplayLabel(sale.status, null),
                        accentColor = saleStatusAccentColor(sale.status, null),
                    )
                    else -> BendeyStatusChip(
                        label = saleStatusDisplayLabel(sale.status, sale.billingStatus),
                        accentColor = saleStatusAccentColor(sale.status, sale.billingStatus),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleDetailContent(
    modifier: Modifier = Modifier,
    sunatEnabled: Boolean,
    loading: Boolean,
    detail: SaleDetail?,
    printing: Boolean,
    billingBusy: String?,
    currency: NumberFormat,
    error: String?,
    onReprint: () -> Unit,
    onOpenPdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onVoidCreditNote: () -> Unit,
    onCancelNota: () -> Unit,
    onRefund: () -> Unit,
    onEmitElectronic: () -> Unit,
    onSendSunat: () -> Unit,
    onResendSunat: () -> Unit,
    onViewXmlSent: () -> Unit,
    onViewXmlGenerated: () -> Unit,
    onDownloadXmlSent: () -> Unit,
    onDownloadXmlGenerated: () -> Unit,
    onDownloadCdr: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        when {
            loading -> Text("Cargando detalle…", color = BendeyColors.OnSurfaceVariant)
            detail == null -> Text("No se pudo cargar la venta", color = BendeyColors.Error)
            else -> {
                Text(detail.displayNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${detail.docType} · ${detail.issueDate}", color = BendeyColors.OnSurfaceVariant)
                detail.billingStatus?.let {
                    // Una nota de venta no va a SUNAT: mostrarle un estado seria inventarle un
                    // trámite que no tiene.
                    if (!isNotaVenta(detail.docType, detail.sunatCode) && sunatEnabled) {
                        Text("Estado SUNAT: ${billingStatusLabel(it)}", color = BendeyColors.OnSurfaceVariant)
                    }
                }
                detail.contactName?.let { Text("Cliente: $it") }
                if (detail.isConverted()) {
                    convertedToLabel(detail.convertedTo, detail.electronicIssueSaleId)?.let { ref ->
                        Text("Convertida a $ref", color = BendeyColors.Info)
                    }
                }
                HorizontalDivider()
                BendeySectionTitle(
                    text = "Ítems",
                    style = MaterialTheme.typography.titleSmall,
                )
                detail.items.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${line.quantity.toInt()}× ${line.description}",
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(currency.format(line.total))
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal")
                    Text(currency.format(detail.subtotal))
                }
                if (detail.taxAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGV")
                        Text(currency.format(detail.taxAmount))
                    }
                }
                // Recargo al Consumo: monto YA CALCULADO por el backend — nunca se recalcula acá.
                // Se omite si es 0 (sucursal sin RC), así el detalle no cambia para quien no lo activó.
                if (detail.serviceChargeAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recargo al Consumo")
                        Text(currency.format(detail.serviceChargeAmount))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text(currency.format(detail.total), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
                }
                if (detail.payments.isNotEmpty()) {
                    HorizontalDivider()
                    BendeySectionTitle(
                        text = "Pagos",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    detail.payments.forEach { payment ->
                        Text("${payment.method}: ${currency.format(payment.amount)}")
                    }
                }
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
                BendeyPrimaryButton(
                    text = if (printing) "Imprimiendo…" else "Reimprimir ticket",
                    onClick = onReprint,
                    enabled = detail.printData != null && !printing,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onOpenPdf,
                    enabled = detail.printData != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ver / compartir PDF")
                }
                // PDF local (A4) guardado en Descargas. Funciona en cualquier estado (aunque no se
                // haya enviado a SUNAT); nunca se solicita al facturador.
                OutlinedButton(
                    onClick = onDownloadPdf,
                    enabled = detail.printData != null && billingBusy != "pdf",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (billingBusy == "pdf") "Guardando PDF…" else "Descargar PDF")
                }
                // LAS ACCIONES SON DEL DOCUMENTO ABIERTO, NO DEL FILTRO ELEGIDO. La lista ahora mezcla
                // notas de venta y comprobantes, asi que preguntarle a la pestana que ofrecer daria
                // botones equivocados: «emitir boleta» sobre una boleta ya emitida, o el envio a
                // SUNAT sobre una nota de venta que nunca va a ir.
                if (isNotaVenta(detail.docType, detail.sunatCode)) {
                    if (detail.canIssueElectronicFromNota(sunatEnabled)) {
                        BendeyPrimaryButton(
                            text = "Emitir boleta o factura",
                            onClick = onEmitElectronic,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (detail.canCancelNotaVenta()) {
                        OutlinedButton(onClick = onCancelNota, modifier = Modifier.fillMaxWidth()) {
                            Text("Anular nota de venta", color = BendeyColors.Error)
                        }
                    }
                } else {
                    if (canSendToSunat(detail.billingStatus)) {
                        BendeyPrimaryButton(
                            text = if (billingBusy == "send") "Enviando a SUNAT…" else "Enviar a SUNAT",
                            onClick = onSendSunat,
                            enabled = billingBusy == null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (canResendToSunat(detail.billingStatus)) {
                        OutlinedButton(
                            onClick = onResendSunat,
                            enabled = billingBusy == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (billingBusy == "resend") "Reenviando…" else sunatSendActionLabel(detail.billingStatus),
                            )
                        }
                    }
                    // Los visores de XML son para el comprobante de venta; en una nota de credito
                    // sobran, que era la unica diferencia entre las dos ramas viejas.
                    SunatDocumentActions(
                        billingStatus = detail.billingStatus,
                        billingBusy = billingBusy,
                        showXmlViewers = !isCreditNoteDoc(detail.docType),
                        onViewXmlSent = onViewXmlSent,
                        onViewXmlGenerated = onViewXmlGenerated,
                        onDownloadXmlSent = onDownloadXmlSent,
                        onDownloadXmlGenerated = onDownloadXmlGenerated,
                        onDownloadCdr = onDownloadCdr,
                    )
                    if (detail.canVoidWithCreditNote()) {
                        OutlinedButton(onClick = onVoidCreditNote, modifier = Modifier.fillMaxWidth()) {
                            Text("Anular con nota de crédito", color = BendeyColors.Error)
                        }
                    }
                }
                // Devolver el dinero vale para cualquier venta: una anulada por cualquier via puede
                // tener plata pendiente de entregar. Anular y devolver son cosas distintas.
                if (detail.canRegisterRefund()) {
                    OutlinedButton(onClick = onRefund, modifier = Modifier.fillMaxWidth()) {
                        Text("Registrar devolucion", color = BendeyColors.Error)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = BendeySpacing.md))
            }
        }
    }
}

@Composable
private fun SunatDocumentActions(
    billingStatus: String?,
    billingBusy: String?,
    showXmlViewers: Boolean,
    onViewXmlSent: () -> Unit,
    onViewXmlGenerated: () -> Unit,
    onDownloadXmlSent: () -> Unit,
    onDownloadXmlGenerated: () -> Unit,
    onDownloadCdr: () -> Unit,
) {
    if (showXmlViewers && canShowXmlSent(billingStatus)) {
        OutlinedButton(onClick = onViewXmlSent, enabled = billingBusy == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (billingBusy == "xml") "Cargando XML…" else "Ver XML enviado")
        }
    }
    if (showXmlViewers && canShowXmlGenerated(billingStatus)) {
        OutlinedButton(onClick = onViewXmlGenerated, enabled = billingBusy == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (billingBusy == "xml-generated") "Cargando XML…" else "Ver XML generado")
        }
    }
    if (canShowXmlSent(billingStatus)) {
        OutlinedButton(onClick = onDownloadXmlSent, enabled = billingBusy == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (billingBusy == "xml") "Descargando…" else "Descargar XML enviado")
        }
    }
    if (canShowXmlGenerated(billingStatus)) {
        OutlinedButton(onClick = onDownloadXmlGenerated, enabled = billingBusy == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (billingBusy == "xml-generated") "Descargando…" else "Descargar XML generado")
        }
    }
    if (canShowCdr(billingStatus)) {
        OutlinedButton(onClick = onDownloadCdr, enabled = billingBusy == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (billingBusy == "cdr") "Descargando…" else "Descargar CDR")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmitElectronicDialog(
    docKind: String,
    seriesId: Int?,
    issueDate: String,
    series: List<com.bendey.restaurant.core.domain.billing.DocumentSeries>,
    contacts: List<ContactBrief>,
    contactId: Int?,
    metaLoading: Boolean,
    sunatLimitWarning: String?,
    loading: Boolean,
    error: String?,
    onDocKindChange: (String) -> Unit,
    onSeriesChange: (Int) -> Unit,
    onIssueDateChange: (String) -> Unit,
    onContactChange: (Int?) -> Unit,
    onAddClient: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var kindExpanded by remember { mutableStateOf(false) }
    var seriesExpanded by remember { mutableStateOf(false) }
    val kindLabel = if (docKind == "01") "Factura (01)" else "Boleta (03)"
    val seriesLabel = series.firstOrNull { it.id == seriesId }?.displayLabel ?: "Seleccionar serie"
    val requiresRuc = isFacturaDocType(if (docKind == "01") "FACTURA" else "BOLETA", docKind)
    val clientOptions = if (requiresRuc) {
        contacts.filter { it.docType.trim() == "6" || it.docType.equals("ruc", ignoreCase = true) }
    } else {
        contacts
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Emitir comprobante electrónico") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Genera boleta o factura desde esta nota de venta. Puede cambiar el cliente antes de emitir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (metaLoading) {
                    Text("Cargando series y clientes…", style = MaterialTheme.typography.bodySmall)
                } else {
                    BendeySearchableSelect(
                        options = clientOptions.map { BendeySelectOption(it.id, it.displayLabel) },
                        selectedId = contactId,
                        onSelect = { onContactChange(it) },
                        label = if (requiresRuc) "Cliente (RUC obligatorio)" else "Cliente",
                        placeholder = if (requiresRuc) "Seleccione cliente con RUC" else "Seleccione cliente",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (requiresRuc && clientOptions.isEmpty()) {
                        Text(
                            "La factura requiere un cliente con RUC registrado.",
                            color = BendeyColors.Warning,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = onAddClient, enabled = !loading) {
                        Text("+ Registrar nuevo cliente")
                    }
                }
                sunatLimitWarning?.let {
                    Text(it, color = BendeyColors.Warning, style = MaterialTheme.typography.bodySmall)
                }
                ExposedDropdownMenuBox(
                    expanded = kindExpanded,
                    onExpandedChange = { kindExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = kindLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = !loading,
                            ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                    )
                    ExposedDropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Boleta (03)") },
                            onClick = { onDocKindChange("03"); kindExpanded = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Factura (01)") },
                            onClick = { onDocKindChange("01"); kindExpanded = false },
                        )
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = seriesExpanded,
                    onExpandedChange = { seriesExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = seriesLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Serie") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = !loading,
                            ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seriesExpanded) },
                    )
                    ExposedDropdownMenu(expanded = seriesExpanded, onDismissRequest = { seriesExpanded = false }) {
                        series.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.displayLabel) },
                                onClick = {
                                    onSeriesChange(item.id)
                                    seriesExpanded = false
                                },
                            )
                        }
                    }
                }
                BendeyTextField(
                    value = issueDate,
                    onValueChange = onIssueDateChange,
                    label = "Fecha emisión (AAAA-MM-DD)",
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Generando…" else "Emitir",
                onClick = onConfirm,
                enabled = !loading && !metaLoading && seriesId != null,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancelar")
            }
        },
    )
}

/** Alta rápida de cliente sin salir del diálogo de conversión (mismo flujo que el POS). */
@Composable
private fun EmitClientQuickAddDialog(
    form: ContactFormInput,
    consulting: Boolean,
    saving: Boolean,
    error: String?,
    onFormChange: ((ContactFormInput) -> ContactFormInput) -> Unit,
    onConsult: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Registrar cliente",
        confirmText = if (saving) "Registrando…" else "Registrar",
        confirmEnabled = !saving && !consulting && form.businessName.isNotBlank() && form.docNumber.isNotBlank(),
        onConfirm = onSave,
        onDismiss = onDismiss,
    ) {
        BendeySimpleSelect(
            options = ContactDocType.entries.map { BendeyOption(it.name, it.label) },
            selectedValue = form.docType.name,
            onSelect = { value ->
                val docType = ContactDocType.entries.firstOrNull { it.name == value } ?: ContactDocType.RUC
                onFormChange { it.copy(docType = docType) }
            },
            label = "Tipo de documento",
        )
        BendeyTextField(
            value = form.docNumber,
            onValueChange = { v -> onFormChange { it.copy(docNumber = v) } },
            label = "N° documento *",
            modifier = Modifier.fillMaxWidth(),
        )
        if (ContactDocType.supportsConsulta(form.docType.code)) {
            BendeyPrimaryButton(
                text = if (consulting) "Validando…" else "Validar",
                onClick = onConsult,
                enabled = !consulting && !saving,
                loading = consulting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        BendeyTextField(
            value = form.businessName,
            onValueChange = { v -> onFormChange { it.copy(businessName = v) } },
            label = "Nombre / Razón social *",
        )
        BendeyTextField(
            value = form.address,
            onValueChange = { v -> onFormChange { it.copy(address = v) } },
            label = "Dirección",
            singleLine = false,
        )
        error?.let {
            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VoidReasonDialog(
    action: VoidAction?,
    refundAmount: Double,
    reason: String,
    pin: String,
    loading: Boolean,
    error: String?,
    onReasonChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = when (action) {
        VoidAction.CREDIT_NOTE -> "Anular con nota de crédito"
        VoidAction.CANCEL_NOTA -> "Anular nota de venta"
        VoidAction.REFUND -> "Registrar devolución"
        null -> "Anular"
    }
    val confirmLabel = when (action) {
        VoidAction.CREDIT_NOTE -> if (loading) "Procesando…" else "Generar nota de crédito"
        VoidAction.CANCEL_NOTA -> if (loading) "Anulando…" else "Confirmar anulación"
        // El boton nombra lo que hace: aca sale dinero, no es un "Aceptar" cualquiera.
        VoidAction.REFUND -> if (loading) "Procesando devolución…" else "Confirmar devolución"
        null -> "Confirmar"
    }
    val esDevolucion = action == VoidAction.REFUND

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (esDevolucion) {
                    // El importe no es editable ni lo calcula la app: llega en `refundableAmount`,
                    // derivado por el backend de lo que la venta cobro de verdad.
                    Text(
                        "Importe a devolver: S/ %.2f".format(refundAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = BendeyColors.Error,
                    )
                    Text(
                        "Es lo efectivamente cobrado por esta venta, no su total. Esta operación " +
                            "registra una salida real de dinero y afecta la caja o la cuenta correspondiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                Text(
                    if (esDevolucion) "Indique el motivo de la devolución." else "Indique el motivo de anulación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                BendeyTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = "Motivo",
                    modifier = Modifier.fillMaxWidth(),
                )
                // Anular exige el PIN del negocio; devolver el dinero no, y el backend hace la misma
                // distincion. Mostrar el campo donde no se pide solo confundiria a quien cobra.
                if (!esDevolucion) {
                    BendeyTextField(
                        value = pin,
                        onValueChange = onPinChange,
                        label = "PIN de operaciones",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Mismo PIN configurado en Ajustes del restaurante.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = !loading && reason.isNotBlank() && (esDevolucion || pin.isNotBlank()),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancelar")
            }
        },
    )
}
