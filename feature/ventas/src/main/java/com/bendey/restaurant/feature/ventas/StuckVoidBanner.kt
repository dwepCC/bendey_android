package com.bendey.restaurant.feature.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.StuckVoidBannerState
import com.bendey.restaurant.core.domain.billing.StuckVoidCopy
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import java.text.NumberFormat

/**
 * Aviso «N comprobantes necesitan atención» (R10.9, SOLO LECTURA): anulaciones cuya nota de crédito SUNAT no
 * aceptó, así que la venta sigue vigente y cuenta en los reportes. Equivale a `StuckVoidCreditNotesPanel` de
 * Tauri. No ocupa lugar si no hay nada; si la consulta falla lo dice y ofrece Reintentar.
 */
@Composable
internal fun StuckVoidBanner(
    state: StuckVoidBannerState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    when (state) {
        StuckVoidBannerState.Hidden -> Unit
        is StuckVoidBannerState.Failed -> BannerBox(modifier) {
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurface)
            BendeyTextButton(text = StuckVoidCopy.LOAD_ERROR_RETRY, onClick = onRetry)
        }
        is StuckVoidBannerState.Pending -> BannerBox(modifier) {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = BendeyColors.WarningText)
                Column(Modifier.weight(1f)) {
                    Text(
                        StuckVoidCopy.title(state.rows.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BendeyColors.OnSurface,
                    )
                    Text(
                        StuckVoidCopy.description(state.rows.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurface,
                    )
                }
            }
            BendeyTextButton(text = if (expanded) StuckVoidCopy.HIDE else StuckVoidCopy.SHOW, onClick = onToggle)
            if (expanded) {
                state.rows.forEach { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(BendeyColors.Surface, RoundedCornerShape(8.dp))
                            .padding(BendeySpacing.sm),
                    ) {
                        Text(
                            StuckVoidCopy.line1(row, currency.format(row.originalTotal)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            StuckVoidCopy.line2(row, StuckVoidCopy.date(row.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        if (row.sunatMessage.isNotBlank()) {
                            Text(
                                row.sunatMessage.take(160),
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                }
                Text(
                    StuckVoidCopy.RESOLVE_HINT,
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BannerBox(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs)
            .background(BendeyColors.WarningContainer, RoundedCornerShape(12.dp))
            .padding(BendeySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        content()
    }
}
