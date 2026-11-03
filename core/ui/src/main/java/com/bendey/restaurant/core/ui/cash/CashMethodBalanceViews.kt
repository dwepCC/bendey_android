package com.bendey.restaurant.core.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.CashMethodBalance
import com.bendey.restaurant.core.domain.cash.CashSuggestedMethodBalance
import com.bendey.restaurant.core.domain.locale.Money
import com.bendey.restaurant.core.ui.components.BendeyTextField
import kotlin.math.abs

// SALDOS POR MÉTODO DEL CAJERO (Yape, Plin, tarjeta…).
//
// Es un control por cajero: con cuánto abre y cierra él cada método. El saldo de la cuenta del negocio
// no cambia con lo que se digite acá.

/** Solo dígitos y un separador decimal: el monto de un método nunca es negativo. */
private fun cleanAmount(raw: String): String = raw.replace(',', '.').filter { it.isDigit() || it == '.' }

/**
 * Montos de apertura de cada método. Vienen prellenados con lo que el cajero cerró la última vez; si
 * los cambia (rindió lo de Yape y empieza en 0) se avisa de cuánto había cerrado.
 */
@Composable
fun CashMethodOpeningFields(
    methods: List<CashSuggestedMethodBalance>,
    values: Map<String, String>,
    onChange: (method: String, value: String) -> Unit,
    enabled: Boolean = true,
) {
    if (methods.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        Text("Otros métodos de pago", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Con cuánto abres cada uno. Viene con lo que cerraste la última vez.",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
        methods.forEach { m ->
            val raw = values[m.method].orEmpty()
            val changed = raw.isNotBlank() && raw.toDoubleOrNull()?.let { abs(it - m.suggested) > 0.004 } == true
            BendeyTextField(
                value = raw,
                onValueChange = { onChange(m.method, cleanAmount(it)) },
                label = "${m.name.ifBlank { m.method }} (${Money.symbol})",
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (changed) {
                    "Cerraste con ${Money.format(m.suggested)}"
                } else {
                    "Último cierre: ${Money.format(m.suggested)}"
                },
            )
        }
    }
}

/**
 * Saldo de cada método: con cuánto abrió, entradas, salidas y esperado. Con [editable] el cajero puede
 * confirmar lo que ve en su app (vacío = sin confirmar, que no es lo mismo que 0). Sin [editable] muestra
 * lo guardado, para el reporte de una caja cerrada.
 */
@Composable
fun CashMethodBalancesList(
    rows: List<CashMethodBalance>,
    editable: Boolean = false,
    values: Map<String, String> = emptyMap(),
    onChange: (method: String, value: String) -> Unit = { _, _ -> },
    enabled: Boolean = true,
) {
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        rows.forEachIndexed { index, r ->
            if (index > 0) HorizontalDivider(color = BendeyColors.Outline)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        r.name.ifBlank { r.method },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Esperado ${Money.format(r.expected)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "Abrió con ${Money.format(r.openingBalance)} · +${Money.format(r.inflows)} · −${Money.format(r.outflows)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (abs(r.openingBalance - r.openingSuggested) > 0.004) {
                    Text(
                        "Apertura ajustada: había cerrado con ${Money.format(r.openingSuggested)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.WarningText,
                    )
                }
                val counted = if (editable) {
                    values[r.method]?.trim()?.toDoubleOrNull()
                } else {
                    r.counted
                }
                if (editable) {
                    BendeyTextField(
                        value = values[r.method].orEmpty(),
                        onValueChange = { onChange(r.method, cleanAmount(it)) },
                        label = "Lo que ves en tu app (opcional)",
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                } else {
                    Text(
                        if (counted == null) "Sin confirmar" else "Confirmado ${Money.format(counted)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                counted?.let { c ->
                    val diff = Math.round((c - r.expected) * 100) / 100.0
                    val (texto, color) = when {
                        abs(diff) < 0.005 -> "Cuadra" to BendeyColors.SuccessText
                        diff > 0 -> "Sobra ${Money.format(diff)}" to BendeyColors.InfoText
                        else -> "Falta ${Money.format(-diff)}" to BendeyColors.ErrorText
                    }
                    Text(texto, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}
