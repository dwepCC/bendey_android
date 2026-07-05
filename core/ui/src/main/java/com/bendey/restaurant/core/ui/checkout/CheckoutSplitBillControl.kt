package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.ComandaCheckoutRow
import com.bendey.restaurant.core.domain.billing.TaxConfig

@Composable
fun CheckoutSplitBillControl(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    showOption: Boolean,
    pending: List<ComandaCheckoutRow>,
    billed: List<ComandaCheckoutRow>,
    selectedIds: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    taxRatePercent: Double,
    taxConfig: TaxConfig,
    modifier: Modifier = Modifier,
) {
    if (!showOption) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BendeyShapeTokens.md,
            color = if (enabled) BendeyColors.Primary.copy(alpha = 0.08f) else BendeyColors.Surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
                Text(
                    text = "Dividir cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (enabled) {
            CheckoutComandaPicker(
                pending = pending,
                billed = billed,
                selectedIds = selectedIds,
                onSelectionChange = onSelectionChange,
                taxRatePercent = taxRatePercent,
                taxConfig = taxConfig,
            )
        }
    }
}
