package com.bendey.restaurant.core.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.CashChipState
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.cash.formatCashSince
import com.bendey.restaurant.core.domain.copy.CashCopy
import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabelEs
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import java.text.NumberFormat

/**
 * Chip de estado de caja para la barra superior (R9). Abierta: verde con "desde - vendido"; cerrada:
 * naranja "Abrir"; lectura fallida: gris "Caja..." (NO es "cerrada"). Para el mozo no se dibuja.
 * Los textos son los de Tauri (`cashCopy.ts`). Nunca depende solo del color: siempre lleva texto.
 */
@Composable
fun CashStatusChip(
    chip: CashChipState,
    soldNet: Double?,
    currency: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chip == CashChipState.Hidden) return
    val dot: Color
    val label: String
    when (chip) {
        is CashChipState.Open -> {
            val since = formatCashSince(chip.sinceIso)
            val sold = soldNet?.let { currency.format(it) }
            dot = BendeyColors.Success
            label = when {
                since != null && sold != null -> CashCopy.chipOpenFull(since, sold)
                sold != null -> CashCopy.chipOpenShort(sold)
                since != null -> "${CashCopy.CHIP_OPEN} · desde $since"
                else -> CashCopy.CHIP_OPEN
            }
        }
        CashChipState.Closed -> {
            dot = BendeyColors.Warning
            label = "${CashCopy.CHIP_CLOSED} — ${CashCopy.CHIP_CLOSED_ACTION}"
        }
        else -> {
            dot = BendeyColors.NavInactive
            label = CashCopy.CHIP_UNKNOWN
        }
    }
    val description = if (chip == CashChipState.Unknown) CashCopy.CHIP_UNKNOWN_HINT else label
    Row(
        modifier = modifier
            .widthIn(max = 220.dp)
            .defaultMinSize(minHeight = 36.dp)
            .clip(BendeyShapeTokens.sm)
            .background(Color.White.copy(alpha = 0.12f))
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onClick)
            .padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Franja fija "Caja cerrada. Abre tu caja para cobrar en efectivo. [Abrir caja]" (no bloquea nada). */
@Composable
fun CashClosedBanner(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.WarningContainer)
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        Text(
            CashCopy.BANNER_CLOSED,
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.WarningText,
            modifier = Modifier.weight(1f),
        )
        BendeyOutlinedButton(text = CashCopy.BANNER_ACTION, onClick = onOpen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Hoja del chip: apertura, vendido neto, por método, ingresos/gastos y Efectivo esperado. NO muestra la
 * diferencia (solo existe dentro del cierre, tras contar). "Cerrar caja" y "Arqueo parcial" llevan a Caja.
 */
@Composable
fun CashStatusSheet(
    session: CashSession?,
    report: CashSessionReport?,
    loading: Boolean,
    error: Boolean,
    currency: NumberFormat,
    onClose: () -> Unit,
    onPartialCount: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    BendeyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            Text(CashCopy.POPOVER_TITLE, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (session != null) {
                val since = formatCashSince(session.openedAt)
                SheetRow(
                    CashCopy.POPOVER_OPENING,
                    listOfNotNull(currency.format(session.openingBalance), since?.let { "desde las $it" })
                        .joinToString(" · "),
                )
                session.openedByName?.takeIf { it.isNotBlank() }?.let { SheetRow(CashCopy.POPOVER_CASHIER, it) }
            }
            HorizontalDivider(color = BendeyColors.Outline)
            when {
                loading && report == null -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(CashCopy.POPOVER_LOADING, style = MaterialTheme.typography.bodySmall)
                    }
                }
                error && report == null -> {
                    Text(CashCopy.POPOVER_ERROR, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                    BendeyOutlinedButton(text = "Reintentar", onClick = onRetry)
                }
                report != null -> {
                    SheetRow(CashCopy.POPOVER_SOLD_NET, currency.format(report.totalNetSales), bold = true)
                    if (report.salesByMethod.isNotEmpty()) {
                        Text(
                            CashCopy.POPOVER_BY_METHOD,
                            style = MaterialTheme.typography.labelMedium,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        report.salesByMethod.forEach {
                            SheetRow(salePaymentMethodLabelEs(it.method), currency.format(it.total))
                        }
                    }
                }
            }
            if (session != null) {
                SheetRow(CashCopy.POPOVER_INCOME, currency.format(session.totalIncome))
                SheetRow(CashCopy.POPOVER_EXPENSE, currency.format(session.totalExpense))
                HorizontalDivider(color = BendeyColors.Outline)
                SheetRow(CashCopy.POPOVER_EXPECTED, currency.format(session.expectedBalance), bold = true)
                Text(
                    CashCopy.POPOVER_EXPECTED_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                modifier = Modifier.fillMaxWidth().padding(top = BendeySpacing.xs),
            ) {
                BendeyOutlinedButton(
                    text = CashCopy.POPOVER_PARTIAL_COUNT,
                    onClick = onPartialCount,
                    modifier = Modifier.weight(1f),
                )
                BendeyPrimaryButton(text = CashCopy.POPOVER_CLOSE, onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, value: String, bold: Boolean = false) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = if (bold) BendeyColors.OnSurface else BendeyColors.OnSurfaceVariant,
            fontWeight = if (bold) FontWeight.SemiBold else null,
        )
        Text(value, fontWeight = if (bold) FontWeight.Bold else null)
    }
}
