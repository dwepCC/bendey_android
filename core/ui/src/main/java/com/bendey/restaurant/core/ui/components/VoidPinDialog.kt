package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun VoidPinDialog(
    open: Boolean,
    title: String,
    description: String? = null,
    itemLabel: String? = null,
    reason: String,
    pin: String,
    loading: Boolean,
    error: String? = null,
    confirmLabel: String = "Anular",
    onReasonChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                itemLabel?.let {
                    Text(it, color = BendeyColors.Primary, style = MaterialTheme.typography.bodyMedium)
                }
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                BendeyTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = "Motivo de anulación",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                BendeyTextField(
                    value = pin,
                    onValueChange = { onPinChange(it.filter { ch -> ch.isDigit() }.take(6)) },
                    label = "PIN de operaciones",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Mismo PIN configurado en Ajustes del restaurante.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Anulando…" else confirmLabel,
                onClick = onConfirm,
                enabled = !loading && reason.isNotBlank() && pin.isNotBlank(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cerrar")
            }
        },
    )
}
