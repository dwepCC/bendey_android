package com.bendey.restaurant.feature.productos.reportes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.sales.SoldReportPreset
import com.bendey.restaurant.core.domain.sales.SoldReportRules
import com.bendey.restaurant.core.domain.sales.SoldSale
import com.bendey.restaurant.core.domain.time.PeruDateTime
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
import com.bendey.restaurant.core.ui.components.BendeyDateField
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.core.ui.subscription.BendeyExportActionsRow

private const val ALL = -1

/** Reportes → Vendidos: por defecto hoy; las anuladas en rojo; toca una venta para ver sus productos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VendidosPanel(
    onNavigateToSubscription: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VendidosViewModel = hiltViewModel(),
) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.start() }
    BendeySnackMessage(message = st.snack, onShow = onShowMessage, onConsume = viewModel::consumeSnack)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                SoldReportPreset.entries.forEach { p ->
                    BendeyFilterChip(selected = st.preset == p, onClick = { viewModel.setPreset(p) }, text = p.label)
                }
            }
        }
        if (st.preset == SoldReportPreset.CUSTOM) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    BendeyDateField(st.customFrom, viewModel::setCustomFrom, "Desde", Modifier.weight(1f))
                    BendeyDateField(st.customTo, viewModel::setCustomTo, "Hasta", Modifier.weight(1f))
                }
            }
        }
        st.rangeError?.let { msg -> item { BendeyAlert(message = msg, severity = BendeyAlertSeverity.Warning) } }
        item {
            BendeySearchableSelect(
                options = listOf(BendeySelectOption(ALL, "Todos")) + st.users.map { BendeySelectOption(it.first, it.second) },
                selectedId = st.userId ?: ALL,
                onSelect = { id -> viewModel.setUser(id.takeIf { it != ALL }) },
                label = "Usuario que cobró",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            BendeySearchableSelect(
                options = listOf(BendeySelectOption(ALL, "Todos")) + st.methods.mapIndexed { i, m -> BendeySelectOption(i, m.second) },
                selectedId = st.methods.indexOfFirst { it.first == st.paymentMethod }.takeIf { it >= 0 } ?: ALL,
                onSelect = { i -> viewModel.setPaymentMethod(st.methods.getOrNull(i)?.first) },
                label = "Método de pago",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            BendeySearchableSelect(
                options = listOf(BendeySelectOption(ALL, "Todos")) +
                    SoldReportRules.DOC_KINDS.mapIndexed { i, k -> BendeySelectOption(i, k.second) },
                selectedId = SoldReportRules.DOC_KINDS.indexOfFirst { it.first == st.docKind }.takeIf { it >= 0 } ?: ALL,
                onSelect = { i -> viewModel.setDocKind(SoldReportRules.DOC_KINDS.getOrNull(i)?.first) },
                label = "Comprobante",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { BendeyCheckboxRow(label = "Solo válidas", checked = st.onlyValid, onCheckedChange = viewModel::setOnlyValid) }

        when {
            st.loading && st.sales.isEmpty() -> item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(4) { BendeySkeletonListRow() } }
            }
            st.error != null && st.sales.isEmpty() -> item { BendeyLoadError(onRetry = viewModel::reload, message = st.error) }
            else -> {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                    ) {
                        Stat("Ventas válidas", st.summary.validSales.toString(), Modifier.fillMaxWidth(0.48f))
                        Stat("Total vendido", SoldReportRules.money(st.summary.validAmount), Modifier.fillMaxWidth(0.48f))
                        Stat("Anuladas", st.summary.cancelledSales.toString(), Modifier.fillMaxWidth(0.48f), danger = true)
                        Stat("Monto anulado", SoldReportRules.money(st.summary.cancelledAmount), Modifier.fillMaxWidth(0.48f), danger = true)
                    }
                }
                item {
                    BendeyExportActionsRow(
                        allowsExport = st.allowsExport,
                        exportBusy = st.exportBusy,
                        onExportPdf = { viewModel.export(context, "pdf") },
                        onExportExcel = { viewModel.export(context, "excel") },
                        onLockedClick = onNavigateToSubscription,
                        pdfLabel = "PDF",
                        excelLabel = "Excel",
                    )
                }
                if (st.sales.isEmpty()) {
                    item {
                        BendeyEmptyState(
                            title = "Sin ventas en este rango",
                            description = "Prueba con otras fechas o quita algún filtro.",
                        )
                    }
                }
                items(st.sales, key = { it.saleId }) { sale ->
                    SaleCard(sale = sale, expanded = sale.saleId in st.expanded, onToggle = { viewModel.toggle(sale.saleId) })
                }
                if (st.hasMore) {
                    item {
                        BendeyOutlinedButton(
                            text = if (st.loadingMore) "Cargando…" else "Cargar más (${st.sales.size} de ${st.total})",
                            onClick = viewModel::loadMore,
                            enabled = !st.loadingMore,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, danger: Boolean = false) {
    BendeyManagementCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (danger) BendeyColors.Error else BendeyColors.OnSurface,
            )
        }
    }
}

@Composable
private fun SaleCard(sale: SoldSale, expanded: Boolean, onToggle: () -> Unit) {
    // Anulada: rojo + tachado + la palabra «Anulada» (no se comunica solo con color).
    val amountColor = if (sale.valid) BendeyColors.OnSurface else BendeyColors.Error
    BendeyManagementCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${SoldReportRules.docKindLabel(sale.docKind)} ${sale.number}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                    textDecoration = if (sale.valid) null else TextDecoration.LineThrough,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    SoldReportRules.money(sale.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    textDecoration = if (sale.valid) null else TextDecoration.LineThrough,
                )
            }
            if (!sale.valid) {
                Text("Anulada", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.Error)
            }
            Muted(
                "${PeruDateTime.formatDateOrRaw(sale.issueDate)} · ${PeruDateTime.formatTimeOrRaw(sale.registeredAt)} · " +
                    SoldReportRules.serviceLabel(sale.service, sale.orderType, sale.tableName),
            )
            Muted("Cobró: ${sale.cashierName.ifBlank { "—" }}" + (sale.waiterName?.let { " · Mozo: $it" } ?: ""))
            Muted(SoldReportRules.paymentsLabel(sale.payments))
            if (expanded) {
                sale.items.forEach { it ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${SoldReportRules.qty(it.quantity)}× ${SoldReportRules.itemLabel(it.description, it.components)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(SoldReportRules.money(it.total), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurface)
                    }
                }
            } else {
                Muted("${sale.items.size} " + (if (sale.items.size == 1) "producto" else "productos") + " · toca para ver el detalle")
            }
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
}
