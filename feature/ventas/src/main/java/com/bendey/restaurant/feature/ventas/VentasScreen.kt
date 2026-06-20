package com.bendey.restaurant.feature.ventas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.canCancelNotaVenta
import com.bendey.restaurant.core.domain.sales.canVoidWithCreditNote
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    modifier: Modifier = Modifier,
    viewModel: VentasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    val billingHint = when (state.tab) {
        VentasTab.FACTURACION, VentasTab.CREDITOS ->
            " · Estados SUNAT en vivo"
        else -> ""
    }
    PullToRefreshBox(
        isRefreshing = state.loading && state.sales.isEmpty(),
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Ventas",
                subtitle = "${state.fromDate} → ${state.toDate} · ${state.total} registros$billingHint",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
            )
            VentasTabRow(
                selected = state.tab,
                onSelect = viewModel::selectTab,
            )
            if (state.error != null && state.sales.isEmpty() && state.selectedSaleId == null) {
                Text(
                    state.error.orEmpty(),
                    color = BendeyColors.Error,
                    modifier = Modifier.padding(16.dp),
                )
            } else if (state.sales.isEmpty() && !state.loading) {
                Text(
                    "Sin ventas en el periodo",
                    color = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.sales, key = { it.id }) { sale ->
                        SaleRow(
                            sale = sale,
                            currency = currency,
                            onClick = { viewModel.openSaleDetail(sale.id) },
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Text(
                                "Desliza para cargar más…",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.selectedSaleId != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSaleDetail,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SaleDetailSheet(
                loading = state.detailLoading,
                detail = state.detail,
                printing = state.detailPrinting,
                currency = currency,
                error = if (state.voidDialogOpen) null else state.error,
                onReprint = viewModel::reprintSelectedSale,
                onVoidCreditNote = viewModel::openVoidCreditNote,
                onCancelNota = viewModel::openCancelNota,
            )
        }
    }

    if (state.voidDialogOpen) {
        VoidReasonDialog(
            action = state.voidAction,
            reason = state.voidReason,
            loading = state.voidSubmitting,
            error = state.error,
            onReasonChange = viewModel::setVoidReason,
            onDismiss = viewModel::dismissVoidDialog,
            onConfirm = viewModel::confirmVoid,
        )
    }
}

@Composable
private fun VentasTabRow(
    selected: VentasTab,
    onSelect: (VentasTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VentasTab.entries.forEach { tab ->
            FilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun SaleRow(
    sale: SaleSummary,
    currency: NumberFormat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currency.format(sale.total),
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
                BendeyStatusChip(
                    label = sale.billingStatus ?: sale.status,
                    accentColor = sale.status.accentColor(),
                )
            }
        }
    }
}

@Composable
private fun SaleDetailSheet(
    loading: Boolean,
    detail: SaleDetail?,
    printing: Boolean,
    currency: NumberFormat,
    error: String?,
    onReprint: () -> Unit,
    onVoidCreditNote: () -> Unit,
    onCancelNota: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            loading -> Text("Cargando detalle…", color = BendeyColors.OnSurfaceVariant)
            detail == null -> Text("No se pudo cargar la venta", color = BendeyColors.Error)
            else -> {
                Text(detail.displayNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${detail.docType} · ${detail.issueDate}", color = BendeyColors.OnSurfaceVariant)
                detail.contactName?.let { Text("Cliente: $it") }
                HorizontalDivider()
                Text("Ítems", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text(currency.format(detail.total), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
                }
                if (detail.payments.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Pagos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                if (detail.canVoidWithCreditNote()) {
                    OutlinedButton(
                        onClick = onVoidCreditNote,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Anular con nota de crédito", color = BendeyColors.Error)
                    }
                }
                if (detail.canCancelNotaVenta()) {
                    OutlinedButton(
                        onClick = onCancelNota,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Anular nota de venta", color = BendeyColors.Error)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun VoidReasonDialog(
    action: VoidAction?,
    reason: String,
    loading: Boolean,
    error: String?,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = when (action) {
        VoidAction.CREDIT_NOTE -> "Anular con nota de crédito"
        VoidAction.CANCEL_NOTA -> "Anular nota de venta"
        null -> "Anular"
    }
    val confirmLabel = when (action) {
        VoidAction.CREDIT_NOTE -> if (loading) "Procesando…" else "Generar nota de crédito"
        VoidAction.CANCEL_NOTA -> if (loading) "Anulando…" else "Confirmar anulación"
        null -> "Confirmar"
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Indique el motivo de anulación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                BendeyTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = "Motivo",
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = !loading && reason.isNotBlank(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancelar")
            }
        },
    )
}

private fun String.accentColor() = when {
    contains("paid", ignoreCase = true) || contains("emit", ignoreCase = true) ||
        contains("accept", ignoreCase = true) -> BendeyColors.Success
    contains("cancel", ignoreCase = true) || contains("void", ignoreCase = true) -> BendeyColors.Error
    else -> BendeyColors.AccentTeal
}
