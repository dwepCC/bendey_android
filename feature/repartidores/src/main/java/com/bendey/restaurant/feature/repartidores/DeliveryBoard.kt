package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardItem
import com.bendey.restaurant.core.domain.delivery.DeliveryTone
import com.bendey.restaurant.core.domain.delivery.deliveryElapsedText
import com.bendey.restaurant.core.domain.delivery.deliveryNeedsAttention
import com.bendey.restaurant.core.domain.delivery.deliveryStatusLabel
import com.bendey.restaurant.core.domain.delivery.deliveryStatusTone
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyListRow
import com.bendey.restaurant.core.ui.components.BendeyListRowSubtitle
import com.bendey.restaurant.core.ui.components.BendeyListRowTitle
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import java.time.Instant

/** Cada cuánto avanza el "hace N min" sin pedir nada al servidor. */
internal const val DELIVERY_CLOCK_TICK_MS = 30_000L

/**
 * Entregas activas (R10.9, SOLO LECTURA): estado, repartidor y tiempo desde que se asignó. Sin acciones de
 * escritura: el repartidor avanza cada entrega desde Bendey Delivery. Estados: cargando / error con
 * Reintentar / vacío / lista (R5: un fallo de carga nunca se muestra como «No hay entregas»).
 */
@Composable
internal fun DeliveryBoard(
    items: List<DeliveryBoardItem>,
    loading: Boolean,
    error: String?,
    now: Instant,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        items.isEmpty() && loading -> Column(modifier.fillMaxSize().padding(BendeySpacing.md), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            repeat(3) { BendeySkeletonListRow() }
        }
        items.isEmpty() && !error.isNullOrBlank() -> BendeyEmptyState(
            title = DeliveryBoardCopy.ERROR_TITLE,
            description = DeliveryBoardCopy.ERROR_DESCRIPTION,
            modifier = modifier,
            action = { BendeyPrimaryButton(text = DeliveryBoardCopy.RETRY, onClick = onRetry, fillWidth = false) },
        )
        items.isEmpty() -> BendeyEmptyState(
            title = DeliveryBoardCopy.EMPTY_TITLE,
            description = DeliveryBoardCopy.EMPTY_DESCRIPTION,
            modifier = modifier,
        )
        else -> BendeyLazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(BendeySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            modifier = modifier.fillMaxWidth(),
        ) {
            item(key = "hint") {
                Text(
                    DeliveryBoardCopy.READ_ONLY_HINT,
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            // Si el último refresco falló pero hay datos, se siguen mostrando y se avisa.
            if (!error.isNullOrBlank()) {
                item(key = "stale") {
                    Text(
                        DeliveryBoardCopy.ERROR_TITLE + ". Mostramos lo último que cargó.",
                        style = MaterialTheme.typography.labelMedium,
                        color = BendeyColors.WarningText,
                    )
                }
            }
            items(items, key = { it.assignmentId }) { item -> DeliveryBoardRow(item, now) }
        }
    }
}

@Composable
private fun DeliveryBoardRow(item: DeliveryBoardItem, now: Instant) {
    val elapsed = deliveryElapsedText(item.assignedAt, now)
    val attention = deliveryNeedsAttention(item, now)
    BendeyListRow {
        BendeyListRowTitle(item.customerName.ifBlank { DeliveryBoardCopy.NO_NAME })
        BendeyListRowSubtitle(DeliveryBoardCopy.driverLine(item.deliveryAddress, item.driverName))
        BendeyStatusChip(deliveryStatusLabel(item.status), toneColor(deliveryStatusTone(item.status)))
        if (elapsed.isNotBlank()) {
            Text(
                elapsed,
                style = MaterialTheme.typography.labelSmall,
                color = if (attention) BendeyColors.WarningText else BendeyColors.OnSurfaceVariant,
            )
        }
    }
}

private fun toneColor(tone: DeliveryTone): Color = when (tone) {
    DeliveryTone.WARNING -> BendeyColors.Warning
    DeliveryTone.INFO -> BendeyColors.Info
    DeliveryTone.SUCCESS -> BendeyColors.Success
    DeliveryTone.DANGER -> BendeyColors.Error
    DeliveryTone.NEUTRAL -> BendeyColors.OnSurfaceVariant
}
