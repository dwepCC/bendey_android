package com.bendey.restaurant.core.ui.cash

import com.bendey.restaurant.core.domain.locale.Money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.CashSuggestedMethodBalance
import com.bendey.restaurant.core.domain.copy.CashCopy
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

data class OpenCashFormState(
    val openingBalance: String = "0",
    val notes: String = "",
    /** Otros métodos (Yape, Plin…) con lo que el cajero cerró la última vez. Vacío = no se muestran. */
    val methods: List<CashSuggestedMethodBalance> = emptyList(),
    /** Monto de apertura digitado por método; lo vacío abre con lo sugerido. */
    val methodAmounts: Map<String, String> = emptyMap(),
    /** Con cuánto terminó la sesión anterior; null en la primera. */
    val lastClosing: Double? = null,
    /** Por qué abre con otro monto que el del último cierre. */
    val openingReason: String = "",
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
    /** Efectivo contado en el último cierre (ya prellenado en el monto), para explicar de dónde sale. */
    prefillAmount: String? = null,
    /** "Abrir con S/ 0": abre sin teclear nada. null = no se ofrece. */
    onOpenZero: (() -> Unit)? = null,
) {
    // Se usa el overload flexible de BendeyAlertDialog (no BendeyFormDialog) a propósito: cuando
    // `mandatory` es true no debe existir botón de cancelar — el turno no puede arrancar sin
    // abrir caja — y BendeyFormDialog siempre dibuja los dos botones. Este es exactamente el caso
    // "acción de diálogo puntual" donde el wrapper flexible es el correcto, no el de formulario.
    BendeyAlertDialog(
        onDismissRequest = { if (!mandatory) onDismiss() },
        title = { Text(CashCopy.OPEN_TITLE) },
        text = {
            // Con varios métodos el contenido no entra en un teléfono: se desplaza.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            ) {
                Text("Monto inicial en efectivo para iniciar el turno.")
                prefillAmount?.let {
                    Text(
                        CashCopy.openPrefillHint(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                BendeyTextField(
                    value = form.openingBalance,
                    onValueChange = { onFormChange(form.copy(openingBalance = it)) },
                    label = CashCopy.OPEN_AMOUNT_LABEL + " (${Money.symbol})",
                )
                CashOpeningJumpFields(
                    lastClosing = form.lastClosing,
                    openingBalance = form.openingBalance,
                    reason = form.openingReason,
                    onReasonChange = { onFormChange(form.copy(openingReason = it)) },
                    enabled = !loading,
                )
                CashMethodOpeningFields(
                    methods = form.methods,
                    values = form.methodAmounts,
                    enabled = !loading,
                    onChange = { method, value -> onFormChange(form.copy(methodAmounts = form.methodAmounts + (method to value))) },
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
                text = if (loading) "Abriendo…" else CashCopy.OPEN_SUBMIT,
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                if (onOpenZero != null) {
                    BendeyTextButton(text = CashCopy.OPEN_ZERO, onClick = onOpenZero, enabled = !loading)
                }
                if (!mandatory) {
                    BendeyTextButton(text = "Cancelar", onClick = onDismiss)
                }
            }
        },
    )
}
