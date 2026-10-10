package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsCopy
import com.bendey.restaurant.core.domain.delivery.PendingEarning
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding

/** Pestaña «Ganancias» de Entregas (§27): lo que se debe a cada repartidor y pagarlo desde tu caja. */
@Composable
fun GananciasPanel(
    viewModel: GananciasViewModel,
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.messages.collect { onShowMessage(it) } }
    LaunchedEffect(viewModel) { viewModel.refresh() }

    if (state.loadError != null && state.items.isEmpty()) {
        BendeyLoadError(onRetry = viewModel::refresh, message = state.loadError ?: "No se pudieron cargar las ganancias.")
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            item {
                Text(
                    DeliveryEarningsCopy.INTRO,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            if (!state.canPay) {
                item {
                    Text(
                        DeliveryEarningsCopy.NO_PAY_PERMISSION,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item {
                    Text(DeliveryEarningsCopy.EMPTY, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
                }
            }
            items(state.items, key = { it.driverId }) { e -> EarningCard(e, state.canPay, viewModel) }
        }
    }
    state.confirm?.let { PayDialog(it, state.busy, viewModel) }
}

@Composable
private fun EarningCard(e: PendingEarning, canPay: Boolean, viewModel: GananciasViewModel) {
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        DeliveryEarningsCopy.deliveriesLabel(e.deliveries),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                Text(DeliveryEarningsCopy.money(e.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (canPay) {
                BendeyPrimaryButton(
                    text = DeliveryEarningsCopy.PAY,
                    onClick = { viewModel.askPay(e) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PayDialog(e: PendingEarning, busy: Boolean, viewModel: GananciasViewModel) {
    BendeyAlertDialog(
        onDismissRequest = { if (!busy) viewModel.dismissPay() },
        title = { Text(DeliveryEarningsCopy.confirmTitle(e)) },
        text = { Text(DeliveryEarningsCopy.confirmText(e), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            BendeyPrimaryButton(
                text = DeliveryEarningsCopy.PAY,
                onClick = viewModel::confirmPay,
                enabled = !busy,
                loading = busy,
                fillWidth = false,
            )
        },
        dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissPay, enabled = !busy) },
    )
}
