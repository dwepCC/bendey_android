package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.ComandaCheckoutRow
import com.bendey.restaurant.core.domain.billing.TaxConfig
import com.bendey.restaurant.core.domain.billing.comandaPayableTotal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CheckoutComandaPicker(
    pending: List<ComandaCheckoutRow>,
    billed: List<ComandaCheckoutRow>,
    selectedIds: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    taxRatePercent: Double,
    taxConfig: TaxConfig,
    modifier: Modifier = Modifier,
) {
    if (pending.isEmpty() && billed.isEmpty()) return

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    var billedOpen by remember { mutableStateOf(false) }
    val allPendingSelected = pending.isNotEmpty() && pending.all { selectedIds.contains(it.comanda.id) }
    val somePendingSelected = pending.any { selectedIds.contains(it.comanda.id) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BendeyShapeTokens.md,
        color = BendeyColors.SurfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(BendeySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "COMANDAS A COBRAR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (pending.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onSelectionChange(
                                if (allPendingSelected) emptyList() else pending.map { it.comanda.id },
                            )
                        },
                    ) {
                        Text(if (allPendingSelected) "Quitar todo" else "Seleccionar todo")
                    }
                }
            }

            if (pending.isNotEmpty()) {
                Text(
                    text = "Pendientes",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                pending.forEach { row ->
                    val id = row.comanda.id
                    val checked = selectedIds.contains(id)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = BendeyShapeTokens.sm,
                        color = if (checked) BendeyColors.Primary.copy(alpha = 0.08f) else BendeyColors.Surface,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    onSelectionChange(
                                        if (checked) selectedIds - id else selectedIds + id,
                                    )
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                if (row.orderNumber > 0) {
                                    Text(
                                        text = "Pedido #${row.orderNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                }
                                Text(
                                    text = "${row.comanda.quantity.toInt()}× ${row.comanda.productName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = currency.format(comandaPayableTotal(row.comanda, taxRatePercent, taxConfig)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.Primary,
                                )
                            }
                        }
                    }
                }
                if (!somePendingSelected) {
                    Text(
                        text = "Selecciona al menos una comanda pendiente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.Warning,
                    )
                }
            } else {
                Text(
                    text = "No hay comandas pendientes en mesa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }

            if (billed.isNotEmpty()) {
                TextButton(
                    onClick = { billedOpen = !billedOpen },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = if (billedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                        )
                        Text(
                            text = "Cobradas (${billed.size})",
                            color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                }
                if (billedOpen) {
                    billed.forEach { row ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = BendeyShapeTokens.sm,
                            color = BendeyColors.SurfaceVariant.copy(alpha = 0.25f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(BendeySpacing.sm),
                            ) {
                                if (row.orderNumber > 0) {
                                    Text(
                                        text = "Pedido #${row.orderNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                                Text(
                                    text = "${row.comanda.quantity.toInt()}× ${row.comanda.productName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.75f),
                                )
                                Text(
                                    text = "Cobrado",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BendeyColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
