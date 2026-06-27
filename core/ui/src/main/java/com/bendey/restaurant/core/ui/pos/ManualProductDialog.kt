package com.bendey.restaurant.core.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.pos.ManualProductInput
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
import com.bendey.restaurant.core.ui.components.BendeyConfigureFullscreenDialog
import com.bendey.restaurant.core.ui.components.BendeyTextField

private val MANUAL_IGV_OPTIONS = listOf(
    "10" to "10 - Gravado IGV",
    "20" to "20 - Exonerado",
    "30" to "30 - Inafecto",
)

private fun isGravadoIgv(code: String): Boolean {
    val c = code.trim()
    return c !in setOf("20", "21", "30", "31", "32", "33", "34", "35", "36", "40")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualProductDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onAdd: (ManualProductInput) -> Unit,
) {
    if (!open) return
    var form by remember(open) { mutableStateOf(ManualProductInput()) }
    val canConfirm = form.description.trim().isNotBlank() && form.unitPrice.trim().isNotBlank()

    BendeyConfigureFullscreenDialog(
        onDismissRequest = onDismiss,
        title = "Producto manual",
        subtitle = "Ítem sin catálogo (comanda, precuenta y cobro).",
        confirmText = "Agregar al carrito",
        confirmEnabled = canConfirm,
        onConfirm = {
            if (!canConfirm) return@BendeyConfigureFullscreenDialog
            onAdd(form)
            onDismiss()
        },
        onDismiss = onDismiss,
    ) {
        BendeyTextField(
            value = form.description,
            onValueChange = { form = form.copy(description = it) },
            label = "Descripción *",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextField(
                value = form.quantity,
                onValueChange = { form = form.copy(quantity = it.filter { ch -> ch.isDigit() }.ifBlank { "1" }) },
                label = "Cantidad",
                modifier = Modifier.weight(1f),
            )
            BendeyTextField(
                value = form.unitPrice,
                onValueChange = { form = form.copy(unitPrice = it) },
                label = "Precio unit. (S/) *",
                modifier = Modifier.weight(1f),
            )
        }
        BendeyTextField(
            value = form.notes,
            onValueChange = { form = form.copy(notes = it) },
            label = "Observaciones",
            singleLine = false,
        )
        ConfigureSectionHeader(title = "Afectación IGV")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MANUAL_IGV_OPTIONS.forEach { (code, label) ->
                FilterChip(
                    selected = form.igvAffectationType == code,
                    onClick = { form = form.copy(igvAffectationType = code) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        if (isGravadoIgv(form.igvAffectationType)) {
            BendeyCheckboxRow(
                label = "Precio incluye IGV",
                checked = form.priceIncludesIgv,
                onCheckedChange = { checked -> form = form.copy(priceIncludesIgv = checked) },
            )
            if (form.unitPrice.trim().isNotBlank()) {
                Text(
                    if (form.priceIncludesIgv) {
                        "El monto es precio final con IGV incluido."
                    } else {
                        "El monto es base; se sumará IGV al total."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        } else {
            Text(
                "Esta afectación no aplica IGV al total.",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
    }
}
