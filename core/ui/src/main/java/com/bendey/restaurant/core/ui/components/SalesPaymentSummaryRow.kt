package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.sales.SaleListSummary
import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabel
import java.text.NumberFormat

@Composable
fun SalesPaymentSummaryRow(
    summary: SaleListSummary,
    paymentMethodNames: Map<String, String>,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    BendeyHorizontalScrollRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = BendeySpacing.md,
            vertical = BendeySpacing.xxs,
        ),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        BendeyCard(
            containerColor = BendeyColors.PrimaryContainer,
            contentPadding = PaddingValues(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
        ) {
            Text("Total", style = MaterialTheme.typography.labelMedium, color = BendeyColors.Primary)
            Text(
                currency.format(summary.sumActive.takeIf { it > 0 } ?: summary.sumTotal),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BendeyColors.Primary,
            )
            if (summary.countActive > 0) {
                Text(
                    "${summary.countActive} comprobantes",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
        summary.paymentTotals.forEach { pt ->
            BendeyManagementCard {
                Column {
                    Text(
                        salePaymentMethodLabel(pt.method, paymentMethodNames),
                        style = MaterialTheme.typography.labelMedium,
                        color = BendeyColors.OnSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        currency.format(pt.total),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = BendeyColors.OnSurface,
                    )
                    if (pt.count > 0) {
                        Text(
                            "${pt.count} ventas",
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
