package com.bendey.restaurant.feature.caja

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat

@Composable
fun ArqueoDialogContent(
    values: Map<String, Int>,
    expectedBalance: Double,
    currency: NumberFormat,
    onQtyChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = sumArqueo(values)
    val diff = total - expectedBalance
    BendeyVerticalScrollColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Saldo sistema", color = BendeyColors.OnSurfaceVariant)
            Text(currency.format(expectedBalance), fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total contado", fontWeight = FontWeight.SemiBold)
            Text(currency.format(total), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Diferencia")
            Text(
                currency.format(diff),
                color = if (kotlin.math.abs(diff) < 0.01) BendeyColors.Success else BendeyColors.Error,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider()
        ArqueoSection("Billetes", ARQUEO_DENOMINATIONS.filter { it.kind == ArqueoKind.BILL }, values, currency, onQtyChange)
        ArqueoSection("Monedas", ARQUEO_DENOMINATIONS.filter { it.kind == ArqueoKind.COIN }, values, currency, onQtyChange)
    }
}

@Composable
private fun ArqueoSection(
    title: String,
    items: List<ArqueoDenomination>,
    values: Map<String, Int>,
    currency: NumberFormat,
    onQtyChange: (String, Int) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    items.forEach { denom ->
        val qty = values[denom.value] ?: 0
        val subtotal = denom.value.toDouble() * qty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(denom.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            // Ancho fijo (no weight): con proporciones el campo quedaba tan angosto que
            // la etiqueta "Cant." se partía en dos líneas y desalineaba todo lo demás.
            ArqueoQtyField(qty = qty, onCommit = { n -> onQtyChange(denom.value, n) })
            Text(
                currency.format(subtotal),
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Campo de cantidad del arqueo. Al enfocar limpia el valor para escribir libremente (sin pelear
 * con el 0). Al salir: si escribió algo lo guarda; si lo dejó vacío, restaura el valor anterior.
 */
@Composable
private fun ArqueoQtyField(qty: Int, onCommit: (Int) -> Unit) {
    var text by remember(qty) { mutableStateOf(qty.toString()) }
    var focused by remember { mutableStateOf(false) }
    BendeyTextField(
        value = text,
        onValueChange = { v -> text = v.filter { it.isDigit() } },
        label = "Cant.",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        fillWidth = false,
        modifier = Modifier
            .width(90.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !focused) {
                    focused = true
                    text = ""
                } else if (!focusState.isFocused && focused) {
                    focused = false
                    if (text.isBlank()) {
                        text = qty.toString() // sin cambios → restaura el valor anterior
                    } else {
                        onCommit(text.toIntOrNull()?.coerceAtLeast(0) ?: 0)
                    }
                }
            },
    )
}

@Composable
fun ArqueoDialog(
    open: Boolean,
    values: Map<String, Int>,
    expectedBalance: Double,
    loading: Boolean,
    currency: NumberFormat,
    onQtyChange: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arqueo de caja") },
        text = {
            ArqueoDialogContent(values, expectedBalance, currency, onQtyChange)
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Guardando…" else "Guardar arqueo",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            BendeyTextButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}

@Composable
fun CloseCashDialog(
    form: CloseCashForm,
    expectedBalance: Double,
    loading: Boolean,
    currency: NumberFormat,
    operationalStatus: com.bendey.restaurant.core.domain.restaurant.BranchOperationalStatus? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((CloseCashForm) -> CloseCashForm) -> Unit,
    onArqueoQtyChange: (String, Int) -> Unit,
) {
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Revise el resumen. Puede cerrar con arqueo para registrar el efectivo contado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                operationalStatus?.takeIf { it.hasActiveOperations }?.let { op ->
                    Text(
                        buildString {
                            append("Advertencia: hay operaciones activas.")
                            if (op.openTablesCount > 0) append(" ${op.openTablesCount} mesa(s) abiertas.")
                            if (op.openSessionsCount > 0) append(" ${op.openSessionsCount} sesión(es).")
                            if (op.activeComandasCount > 0) append(" ${op.activeComandasCount} comanda(s) activas.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.Warning,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo sistema")
                    Text(currency.format(expectedBalance), fontWeight = FontWeight.Bold)
                }
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BendeyFilterChip(
                        selected = form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = true) } },
                        text = "Con arqueo",
                    )
                    BendeyFilterChip(
                        selected = !form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = false) } },
                        text = "Sin arqueo",
                    )
                }
                if (form.useArqueo) {
                    ArqueoDialogContent(form.arqueo, expectedBalance, currency, onArqueoQtyChange)
                } else {
                    BendeyTextField(
                        value = form.closingBalance,
                        onValueChange = { v -> onFormChange { it.copy(closingBalance = v) } },
                        label = "Efectivo contado (S/)",
                    )
                }
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { v -> onFormChange { it.copy(notes = v) } },
                    label = "Notas de cierre",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Cerrando…" else "Cerrar caja",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            BendeyTextButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
