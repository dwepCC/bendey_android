package com.bendey.restaurant.core.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

data class OpenCashFormState(
    val openingBalance: String = "0",
    val notes: String = "",
)

@Composable
fun OpenCashSessionDialog(
    form: OpenCashFormState,
    loading: Boolean,
    mandatory: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: (OpenCashFormState) -> Unit,
) {
    // Se usa el overload flexible de BendeyAlertDialog (no BendeyFormDialog) a propósito: cuando
    // `mandatory` es true no debe existir botón de cancelar — el turno no puede arrancar sin
    // abrir caja — y BendeyFormDialog siempre dibuja los dos botones. Este es exactamente el caso
    // "acción de diálogo puntual" donde el wrapper flexible es el correcto, no el de formulario.
    BendeyAlertDialog(
        onDismissRequest = { if (!mandatory) onDismiss() },
        title = { Text("Abrir caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                Text("Monto inicial en efectivo para iniciar el turno.")
                BendeyTextField(
                    value = form.openingBalance,
                    onValueChange = { onFormChange(form.copy(openingBalance = it)) },
                    label = "Monto de apertura (S/)",
                )
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { onFormChange(form.copy(notes = it)) },
                    label = "Notas (opcional)",
                    singleLine = false,
                )
                error?.let {
                    Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Abriendo…" else "Abrir caja",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            if (!mandatory) {
                BendeyTextButton(text = "Cancelar", onClick = onDismiss)
            }
        },
    )
}
