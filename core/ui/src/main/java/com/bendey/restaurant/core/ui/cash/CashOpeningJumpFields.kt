package com.bendey.restaurant.core.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.openingJump
import com.bendey.restaurant.core.domain.copy.CashCopy
import com.bendey.restaurant.core.domain.locale.Money
import com.bendey.restaurant.core.ui.components.BendeyTextField

/**
 * Aviso al abrir con otro monto que el del último cierre, con el motivo. El backend registra esa diferencia
 * como retiro o ingreso de la caja: el cajero tiene que saberlo antes de confirmar.
 */
@Composable
fun CashOpeningJumpFields(
    lastClosing: Double?,
    openingBalance: String,
    reason: String,
    onReasonChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    val salto = openingJump(lastClosing, openingBalance)
    if (Math.abs(salto) < 0.01) return
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        val monto = Money.format(Math.abs(salto))
        Text(
            if (salto < 0) CashCopy.openJumpOut(monto) else CashCopy.openJumpIn(monto),
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.WarningText,
        )
        BendeyTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = CashCopy.OPEN_REASON_LABEL,
            enabled = enabled,
        )
    }
}
