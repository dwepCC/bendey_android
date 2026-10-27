package com.bendey.restaurant.core.ui.pos

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.Money

import androidx.compose.foundation.layout.Arrangement
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
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import com.bendey.restaurant.core.ui.pos.PosPolishTokens

// Etiquetas por país (I5): en Perú «10 - Gravado IGV», «20 - Exonerado», «30 - Inafecto».
private val MANUAL_IGV_OPTIONS: List<Pair<String, String>>
    get() = listOf(
        "10" to "10 - ${LabelKey.TAX_TAXED.text} ${LabelKey.TAX_NAME.text}",
        "20" to "20 - ${LabelKey.TAX_EXEMPT.text}",
        "30" to "30 - ${LabelKey.TAX_UNAFFECTED.text}",
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
    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val useTwoColumns = PosPolishTokens.usesPosTabletDialogLayout(profile, physicalPortrait)

    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Producto manual",
        confirmText = "Agregar al carrito",
        dismissText = "Cancelar",
        confirmEnabled = canConfirm,
        enableContentScroll = true,
        posTabletOptimized = true,
        onConfirm = {
            if (!canConfirm) return@BendeyFormDialog
            onAdd(form)
            onDismiss()
        },
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Ítem sin catálogo (comanda, precuenta y cobro).",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
        BendeyTextField(
            value = form.description,
            onValueChange = { form = form.copy(description = it) },
            label = "Descripción *",
        )
        if (useTwoColumns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.md),
            ) {
                BendeyTextField(
                    value = form.quantity,
                    onValueChange = { form = form.copy(quantity = it.filter { ch -> ch.isDigit() }.ifBlank { "1" }) },
                    label = "Cantidad",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = form.unitPrice,
                    onValueChange = { form = form.copy(unitPrice = it) },
                    label = "Precio unit. (${Money.symbol}) *",
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            BendeyTextField(
                value = form.quantity,
                onValueChange = { form = form.copy(quantity = it.filter { ch -> ch.isDigit() }.ifBlank { "1" }) },
                label = "Cantidad",
            )
            BendeyTextField(
                value = form.unitPrice,
                onValueChange = { form = form.copy(unitPrice = it) },
                label = "Precio unit. (${Money.symbol}) *",
            )
        }
        BendeyTextField(
            value = form.notes,
            onValueChange = { form = form.copy(notes = it) },
            label = "Observaciones",
            singleLine = false,
        )
        ConfigureSectionHeader(title = "Afectación ${LabelKey.TAX_NAME.text}")
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
            BendeySwitchRow(
                label = "Precio incluye ${LabelKey.TAX_NAME.text}",
                checked = form.priceIncludesIgv,
                onCheckedChange = { checked -> form = form.copy(priceIncludesIgv = checked) },
            )
            if (form.unitPrice.trim().isNotBlank()) {
                Text(
                    if (form.priceIncludesIgv) {
                        "El monto es precio final con ${LabelKey.TAX_NAME.text} incluido."
                    } else {
                        "El monto es base; se sumará ${LabelKey.TAX_NAME.text} al total."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        } else {
            Text(
                "Esta afectación no aplica ${LabelKey.TAX_NAME.text} al total.",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
    }
}
