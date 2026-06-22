package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary

@Composable
fun BendeySessionOrderCard(
    order: SessionOrderSummary,
    reprinting: Boolean,
    reprintEnabled: Boolean,
    onReprint: () -> Unit,
    modifier: Modifier = Modifier,
    comandaActions: @Composable (SessionComandaSummary) -> Unit = {},
) {
    BendeyManagementCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(BendeySpacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Comanda #${order.orderNumber}",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = BendeySpacing.sm),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = BendeyColors.OnSurface,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(enabled = reprintEnabled, onClick = onReprint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (reprinting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = BendeyColors.Primary,
                        )
                    } else {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Reimprimir",
                            tint = if (reprintEnabled) BendeyColors.Primary else BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            order.comandas.forEach { comanda ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    Text(
                        text = "${comanda.quantity.toInt()}× ${comanda.productName}",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BendeyColors.OnSurface,
                    )
                    comandaActions(comanda)
                    BendeyStatusChip(
                        label = comanda.status.label,
                        accentColor = comanda.status.accentColor(),
                    )
                }
            }
        }
    }
}
