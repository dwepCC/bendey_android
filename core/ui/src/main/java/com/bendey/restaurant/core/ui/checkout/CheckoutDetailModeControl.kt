package com.bendey.restaurant.core.ui.checkout

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

/**
 * Checkbox "Venta por consumo" en el checkout. NO decide nada por su cuenta: solo manda
 * `detail_mode` al backend, que es quien valida contra la configuracion de la sucursal y quien
 * realmente agrupa (o no) el comprobante. Ver internal/saledetail en el backend.
 */
@Composable
fun CheckoutDetailModeControl(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    showOption: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showOption) return

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                text = "Venta por consumo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
