package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.RefundPending
import com.bendey.restaurant.core.domain.delivery.SettlementGroup
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import java.util.Locale

private fun soles(v: Double) = String.format(Locale.US, "S/ %.2f", v)

/** Pestaña «Rendiciones» de Entregas (D2c): recibir el efectivo que cobraron los repartidores. */
@Composable
fun RendicionesPanel(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: RendicionesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.messages.collect { onShowMessage(it) } }

    when {
        state.loadError != null && state.pending.groups.isEmpty() && state.pending.refunds.isEmpty() ->
            BendeyLoadError(onRetry = viewModel::refresh, message = state.loadError ?: "No se pudieron cargar las rendiciones.")
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            item {
                Text(
                    "Recibe el efectivo que cobraron los repartidores. Se factura como Nota de Venta y entra a tu caja abierta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            if (state.docRequests.isNotEmpty()) {
                item { Text("Comprobantes pedidos en la puerta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                items(state.docRequests, key = { "d-${it.id}" }) { r ->
                    BendeyManagementCard {
                        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                            Text("${r.label} · #${r.orderCode} · ${soles(r.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                (if (r.clientDocNumber.isNotBlank()) "${r.clientDocNumber} · " else "Sin documento · ") + r.clientName.ifBlank { "Cliente" },
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Text(
                                "Al emitirla, el efectivo entra a tu caja; el repartidor te lo entrega luego en la rendicion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                                BendeyOutlinedButton(text = "Cancelar", onClick = { viewModel.openCancelDoc(r) }, enabled = state.issuingId == null)
                                BendeyPrimaryButton(
                                    text = "Emitir",
                                    onClick = { viewModel.issueDoc(r) },
                                    enabled = state.issuingId == null,
                                    loading = state.issuingId == r.id,
                                    fillWidth = false,
                                )
                            }
                        }
                    }
                }
            }
            if (!state.loading && state.pending.groups.isEmpty()) {
                item { Text("No hay efectivo pendiente de rendir.", style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant) }
            }
            items(state.pending.groups, key = { "${it.collectorKind}-${it.collectorId}" }) { g ->
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                        Text(g.collectorName.ifBlank { "Repartidor" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${g.orders.size} pedido(s) · debe rendir ${soles(g.total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        g.orders.forEach { o ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "#${o.orderCode} · ${o.customerName.ifBlank { "Sin nombre" }}" + if (o.alreadyBilled) " · ya facturado ${o.saleNumber}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(soles(o.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                        BendeyPrimaryButton(text = "Recibir efectivo", onClick = { viewModel.openReceive(g) })
                    }
                }
            }
            if (state.pending.refunds.isNotEmpty()) {
                item { Text("Por devolver al cliente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                items(state.pending.refunds, key = { "r-${it.paymentId}" }) { r ->
                    BendeyManagementCard {
                        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                            Text(
                                "#${r.orderCode} · ${r.collectorName.ifBlank { "Repartidor" }} · ${soles(r.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (r.cancelReason.isNotBlank()) {
                                Text(r.cancelReason, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            }
                            BendeyOutlinedButton(text = "Ya devuelto", onClick = { viewModel.openRefund(r) })
                        }
                    }
                }
            }
            if (state.history.isNotEmpty()) {
                item { Text("Ultimas rendiciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                items(state.history, key = { "h-${it.id}" }) { h ->
                    Column {
                        Text(
                            "${h.collectorName.ifBlank { "Repartidor" }} · ${h.ordersCount} pedido(s) · ${soles(h.receivedTotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val diff = if (kotlin.math.abs(h.difference) >= 0.01) (if (h.difference < 0) "Falto " else "Sobro ") + soles(kotlin.math.abs(h.difference)) else ""
                        Text(
                            listOf("Recibio ${h.receivedByName.ifBlank { "-" }}", diff).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (h.difference < -0.009) BendeyColors.Error else BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    state.receive?.let { ReceiveDialog(it, viewModel) }
    state.refund?.let { RefundDialog(it, viewModel) }
    state.cancelDoc?.let { CancelDocDialog(it, viewModel) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiveDialog(d: ReceiveDialogState, viewModel: RendicionesViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!d.busy) viewModel.dismissReceive() },
        title = { Text("Recibir efectivo de ${d.group.collectorName.ifBlank { "repartidor" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    d.group.orders.forEach { o ->
                        BendeyFilterChip(
                            selected = o.paymentId in d.selected,
                            onClick = { viewModel.toggleOrder(o.paymentId) },
                            text = "#${o.orderCode} · ${soles(o.amount)}",
                        )
                    }
                }
                Text("Debe entregarte: ${soles(d.expected)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                BendeyTextField(
                    value = d.receivedText,
                    onValueChange = viewModel::setReceived,
                    label = "Efectivo que recibes (S/)",
                    enabled = !d.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (d.receivedValue != null && d.hasDifference) {
                    Text(
                        (if (d.difference < 0) "Faltan " else "Sobran ") + soles(kotlin.math.abs(d.difference)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (d.difference < 0) BendeyColors.Error else BendeyColors.OnSurface,
                    )
                }
                if (d.hasDifference) {
                    BendeyTextField(
                        value = d.reason,
                        onValueChange = viewModel::setReason,
                        label = "Motivo de la diferencia (obligatorio)",
                        enabled = !d.busy,
                    )
                }
                d.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Recibir efectivo", onClick = viewModel::confirmReceive, enabled = !d.busy, loading = d.busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissReceive, enabled = !d.busy) },
    )
}

@Composable
private fun RefundDialog(d: RefundDialogState, viewModel: RendicionesViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!d.busy) viewModel.dismissRefund() },
        title = { Text("Cerrar devolucion #${d.item.orderCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                Text("${soles(d.item.amount)} por devolver al cliente.", style = MaterialTheme.typography.bodyMedium)
                BendeyTextField(
                    value = d.note,
                    onValueChange = viewModel::setRefundNote,
                    label = "Nota (quien devolvio y como)",
                    enabled = !d.busy,
                )
                d.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Ya devuelto", onClick = viewModel::confirmRefund, enabled = !d.busy, loading = d.busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissRefund, enabled = !d.busy) },
    )
}

@Composable
private fun CancelDocDialog(d: CancelDocDialogState, viewModel: RendicionesViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!d.busy) viewModel.dismissCancelDoc() },
        title = { Text("Cancelar solicitud #${d.item.orderCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                BendeyTextField(
                    value = d.reason,
                    onValueChange = viewModel::setCancelDocReason,
                    label = "Motivo",
                    enabled = !d.busy,
                )
                d.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Cancelar solicitud", onClick = viewModel::confirmCancelDoc, enabled = !d.busy, loading = d.busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissCancelDoc, enabled = !d.busy) },
    )
}
