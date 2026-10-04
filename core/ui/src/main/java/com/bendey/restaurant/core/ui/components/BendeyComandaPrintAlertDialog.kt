package com.bendey.restaurant.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

/**
 * Aviso accionable cuando el pedido salió a cocina pero la comanda no se imprimió.
 * Con [canReprint] ofrece "Reimprimir"; sin impresora configurada solo se puede entender el aviso.
 */
@Composable
fun BendeyComandaPrintAlertDialog(
    message: String,
    canReprint: Boolean,
    reprinting: Boolean,
    onReprint: () -> Unit,
    onDismiss: () -> Unit,
) {
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Comanda sin imprimir",
                style = MaterialTheme.typography.titleLarge,
                color = BendeyColors.OnSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
        },
        confirmButton = {
            if (canReprint) {
                BendeyPrimaryButton(
                    text = "Reimprimir",
                    onClick = onReprint,
                    enabled = !reprinting,
                    loading = reprinting,
                    fillWidth = false,
                )
            } else {
                BendeyPrimaryButton(text = "Entendido", onClick = onDismiss, fillWidth = false)
            }
        },
        dismissButton = {
            if (canReprint) BendeyTextButton(text = "Cerrar", onClick = onDismiss)
        },
    )
}
