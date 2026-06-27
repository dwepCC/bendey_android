package com.bendey.restaurant.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeySessionOrderCard

@Composable
fun PosSentOrdersSection(
    orders: List<SessionOrderSummary>,
    reprintingOrderId: Int?,
    reprintingAll: Boolean,
    onReprint: (SessionOrderSummary) -> Unit,
    onReprintAll: () -> Unit,
    onVoidComanda: (SessionComandaSummary) -> Unit,
    onEditComandaNotes: (SessionComandaSummary) -> Unit = {},
    canAnularComanda: Boolean = false,
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
        val listState = rememberLazyListState()
        BendeyLazyColumn(
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(max = 200.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                BendeySessionOrderCard(
                    order = order,
                    reprinting = reprintingOrderId == order.id,
                    reprintEnabled = reprintingOrderId != order.id && !reprintingAll,
                    onReprint = { onReprint(order) },
                    comandaActions = { comanda ->
                        BendeyIconButton(
                            onClick = { onEditComandaNotes(comanda) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar notas",
                                tint = BendeyColors.Primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (canAnularComanda) {
                            BendeyIconButton(
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
                    },
                )
            }
        }
    }
}
