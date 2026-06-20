package com.bendey.restaurant.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary

@Composable
fun PosSentOrdersSection(
    orders: List<SessionOrderSummary>,
    reprintingOrderId: Int?,
    reprintingAll: Boolean,
    onReprint: (SessionOrderSummary) -> Unit,
    onReprintAll: () -> Unit,
    onVoidComanda: (SessionComandaSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (orders.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.WarningContainer.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val itemCount = orders.sumOf { order -> order.comandas.sumOf { it.quantity.toInt() } }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = BendeyColors.OnWarning)
            Column(modifier = Modifier.weight(1f)) {
                Text("Ya en cocina", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${orders.size} comanda(s) · $itemCount plato(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onReprintAll,
                enabled = !reprintingAll && reprintingOrderId == null,
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                if (reprintingAll) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 6.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                }
                Text(if (orders.size > 1) "Todas" else "Reimprimir", style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BendeyColors.Outline, RoundedCornerShape(10.dp)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Comanda #${order.orderNumber}", fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = { onReprint(order) },
                                enabled = reprintingOrderId != order.id && !reprintingAll,
                                modifier = Modifier.heightIn(min = 32.dp),
                            ) {
                                if (reprintingOrderId == order.id) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                    )
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                }
                                Text("Reimprimir", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        order.comandas.forEach { comanda ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${comanda.quantity.toInt()}× ${comanda.productName}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                BendeyStatusChip(
                                    label = comanda.status.label,
                                    accentColor = BendeyColors.OnSurfaceVariant,
                                )
                                IconButton(
                                    onClick = { onVoidComanda(comanda) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Anular línea",
                                        tint = BendeyColors.Error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
