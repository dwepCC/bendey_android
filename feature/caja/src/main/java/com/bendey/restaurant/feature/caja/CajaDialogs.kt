package com.bendey.restaurant.feature.caja

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
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
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
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
        ) {
            Column(Modifier.weight(1f)) {
                Text(denom.label, style = MaterialTheme.typography.bodySmall)
                Text("S/ ${denom.value}", style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
            }
            BendeyTextField(
                value = qty.toString(),
                onValueChange = { v -> onQtyChange(denom.value, v.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                label = "Cant.",
                modifier = Modifier.weight(0.35f),
            )
            Text(
                currency.format(subtotal),
                modifier = Modifier.weight(0.3f).padding(top = 24.dp),
                fontWeight = FontWeight.Medium,
            )
        }
    }
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
    AlertDialog(
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
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
    AlertDialog(
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
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = true) } },
                        label = { Text("Con arqueo") },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = !form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = false) } },
                        label = { Text("Sin arqueo") },
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
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
