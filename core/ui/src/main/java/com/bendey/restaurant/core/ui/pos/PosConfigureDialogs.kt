package com.bendey.restaurant.core.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.pos.CartModifierEntry
import com.bendey.restaurant.core.domain.pos.ComboSlotSelection
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.domain.pos.calcUnitPriceWithModifiers
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat

@Composable
fun PosCatalogTabRow(
    selected: PosCatalogTab,
    onSelect: (PosCatalogTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PosCatalogTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
fun ProductConfigureDialog(
    product: PosProduct,
    loading: Boolean,
    presentations: List<ProductPresentation>,
    extraGroups: List<ModifierGroup>,
    selected: List<CartModifierEntry>,
    kitchenNote: String,
    validationError: String?,
    currency: NumberFormat,
    onSelectPresentation: (ProductPresentation) -> Unit,
    onToggleExtra: (ModifierGroup, Int) -> Unit,
    onKitchenNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val unitPrice = calcUnitPriceWithModifiers(product.salePrice, selected)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (loading) Text("Cargando opciones…", color = BendeyColors.OnSurfaceVariant)
                if (presentations.isNotEmpty()) {
                    Text("Presentación", style = MaterialTheme.typography.labelLarge)
                    presentations.filter { it.active && it.name.isNotBlank() }.forEach { pres ->
                        val picked = selected.any { it.type == "variant" && it.optionId == (pres.id ?: 0) }
                        FilterChip(
                            selected = picked,
                            onClick = { onSelectPresentation(pres) },
                            label = { Text("${pres.name} · ${currency.format(pres.salePrice)}") },
                        )
                    }
                }
                extraGroups.forEach { group ->
                    Text(group.name + if (group.required) " *" else "", style = MaterialTheme.typography.labelLarge)
                    group.options.forEach { opt ->
                        val optionId = opt.id ?: return@forEach
                        val picked = selected.any { it.type == "modifier" && it.optionId == optionId }
                        FilterChip(
                            selected = picked,
                            onClick = { onToggleExtra(group, optionId) },
                            label = {
                                val extra = if (opt.extraPrice > 0) " +${currency.format(opt.extraPrice)}" else ""
                                Text("${opt.name}$extra")
                            },
                        )
                    }
                }
                BendeyTextField(kitchenNote, onKitchenNoteChange, "Notas para cocina", singleLine = false)
                validationError?.let { Text(it, color = BendeyColors.Error) }
                Text("Precio unitario: ${currency.format(unitPrice)}", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { BendeyPrimaryButton("Agregar", onConfirm, enabled = !loading, fillWidth = false) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ComboConfigureDialog(
    combo: PosComboItem,
    loading: Boolean,
    form: ComboFormInput?,
    selections: List<ComboSlotSelection>,
    kitchenNote: String,
    validationError: String?,
    resolvedPrice: Double?,
    currency: NumberFormat,
    onToggleSlot: (ComboSlot, Int) -> Unit,
    onKitchenNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(combo.name, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (loading) Text("Cargando combo…", color = BendeyColors.OnSurfaceVariant)
                form?.slots?.forEach { slot ->
                    Text(slot.name, style = MaterialTheme.typography.labelLarge)
                    slot.options.forEach { opt ->
                        val optionId = opt.id ?: return@forEach
                        val picked = selections.any { it.slotId == (slot.id ?: 0) && it.optionId == optionId }
                        FilterChip(
                            selected = picked,
                            onClick = { onToggleSlot(slot, optionId) },
                            label = {
                                val label = opt.productName ?: "Opción $optionId"
                                val upgrade = if (opt.upgradePrice > 0) " +${currency.format(opt.upgradePrice)}" else ""
                                Text("$label$upgrade")
                            },
                        )
                    }
                }
                BendeyTextField(kitchenNote, onKitchenNoteChange, "Notas para cocina", singleLine = false)
                validationError?.let { Text(it, color = BendeyColors.Error) }
                Text("Precio: ${currency.format(resolvedPrice ?: combo.basePrice)}", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { BendeyPrimaryButton("Agregar", onConfirm, enabled = !loading, fillWidth = false) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
