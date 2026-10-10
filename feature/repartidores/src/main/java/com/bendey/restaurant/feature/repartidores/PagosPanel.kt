package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRules
import com.bendey.restaurant.core.domain.delivery.PaymentReview
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

/** Pestaña «Pagos» de Entregas (D2a): validar o rechazar las capturas de pago de los clientes. */
@Composable
fun PagosPanel(
    viewModel: PagosViewModel,
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.messages.collect { onShowMessage(it) } }
    // Los minutos restantes se recalculan solos cada 30 s.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    when {
        state.loadError != null && state.items.isEmpty() ->
            BendeyLoadError(onRetry = viewModel::refresh, message = state.loadError ?: "No se pudieron cargar los pagos.")
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            item {
                Text(
                    "Revisa la captura y valida el pago: el pedido pasa a cocina y se genera la venta. Si no coincide, recházalo con un motivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            if (!state.loading && state.items.isEmpty()) {
                item { Text("No hay pagos por validar.", style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant) }
            }
            items(state.items, key = { it.paymentId }) { p ->
                PaymentCard(p, nowMs, viewModel)
            }
        }
    }

    state.approve?.let { ApproveDialog(it, state.approveBusy, viewModel) }
    state.reject?.let { RejectDialog(it, viewModel) }
    state.viewer?.let { ProofViewer(it, viewModel) }
}

@Composable
private fun PaymentCard(p: PaymentReview, nowMs: Long, viewModel: PagosViewModel) {
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.orderCode}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(soles(p.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                listOf(p.customerName.ifBlank { "Cliente" }, p.customerPhone).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOf(
                    p.methodLabel,
                    if (p.operationNumber.isNotBlank()) "Operación ${p.operationNumber}" else "Sin N° de operación",
                    if (p.attempts > 1) "Intento ${p.attempts}" else "",
                ).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            ManualPaymentRules.minutesLeft(p.expiresAtMs, nowMs)?.let { left ->
                Text(
                    if (left > 0) "Plazo de pago: $left min" else "Plazo de pago cumplido",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            if (p.wantsDocument) {
                Text(
                    "${p.docLabel} · " + (if (p.clientDocNumber.isNotBlank()) "${p.clientDocNumber} · " else "") + p.clientName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (p.proofUrl.isNotBlank()) {
                ProofImage(
                    url = ManualPaymentRules.resolveAssetUrl(viewModel.assetsBaseUrl, p.proofUrl),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                        .clip(BendeyShapeTokens.xs)
                        .background(BendeyColors.SurfaceVariant)
                        .clickable(onClickLabel = "Ver la captura ampliada") { viewModel.openProof(p) },
                    contentDescription = "Captura del pago del pedido ${p.orderCode}",
                )
                Text("Toca la captura para verla ampliada.", style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
            } else {
                Text("Sin captura adjunta.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                BendeyOutlinedButton(text = "Rechazar", onClick = { viewModel.openReject(p) })
                BendeyPrimaryButton(text = "Validar", onClick = { viewModel.askApprove(p) }, fillWidth = false)
            }
        }
    }
}

@Composable
private fun ProofImage(url: String, modifier: Modifier, contentDescription: String, scale: ContentScale = ContentScale.Fit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = scale,
        )
    }
}

@Composable
private fun ProofViewer(p: PaymentReview, viewModel: PagosViewModel) {
    Dialog(
        onDismissRequest = viewModel::closeProof,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ProofImage(
                url = ManualPaymentRules.resolveAssetUrl(viewModel.assetsBaseUrl, p.proofUrl),
                modifier = Modifier.fillMaxSize().clickable(onClickLabel = "Cerrar") { viewModel.closeProof() },
                contentDescription = "Captura ampliada del pedido ${p.orderCode}",
            )
            Column(
                Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(BendeySpacing.s12),
            ) {
                Text("#${p.orderCode} · ${soles(p.amount)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(p.methodLabel, if (p.operationNumber.isNotBlank()) "Operación ${p.operationNumber}" else "").filter { it.isNotBlank() }.joinToString(" · "),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            BendeyPrimaryButton(
                text = "Cerrar",
                onClick = viewModel::closeProof,
                fillWidth = false,
                modifier = Modifier.align(Alignment.BottomCenter).padding(BendeySpacing.s16).size(width = 160.dp, height = 48.dp),
            )
        }
    }
}

@Composable
private fun ApproveDialog(p: PaymentReview, busy: Boolean, viewModel: PagosViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!busy) viewModel.dismissApprove() },
        title = { Text("Validar pago #${p.orderCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                Text("Confirma que ya viste ${soles(p.amount)} en ${p.methodLabel}.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "El pedido pasará a cocina y se generará " +
                        (if (p.wantsDocument) "una ${p.docLabel.lowercase()}" else "una Nota de Venta") + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Validar pago", onClick = viewModel::confirmApprove, enabled = !busy, loading = busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissApprove, enabled = !busy) },
    )
}

@Composable
private fun RejectDialog(d: RejectPaymentDialogState, viewModel: PagosViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!d.busy) viewModel.dismissReject() },
        title = { Text("Rechazar pago #${d.item.orderCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                Text("El cliente verá el motivo y podrá subir otra captura.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                BendeyTextField(
                    value = d.reason,
                    onValueChange = viewModel::setRejectReason,
                    label = "Motivo (obligatorio)",
                    enabled = !d.busy,
                )
                d.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Rechazar pago", onClick = viewModel::confirmReject, enabled = !d.busy, loading = d.busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissReject, enabled = !d.busy) },
    )
}
