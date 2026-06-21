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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
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
            .padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    onSetExtraQuantity: (ModifierGroup, Int, Int) -> Unit = { _, _, _ -> },
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
                        val picked = selected.firstOrNull { it.type == "modifier" && it.optionId == optionId }
                        if (group.selectionMode == ModifierSelectionMode.QUANTITY) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${opt.name} (+${currency.format(opt.extraPrice)})")
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        onSetExtraQuantity(group, optionId, (picked?.quantity ?: 0) - 1)
                                    }) { Text("-") }
                                    Text((picked?.quantity ?: 0).toString())
                                    TextButton(onClick = {
                                        onSetExtraQuantity(group, optionId, (picked?.quantity ?: 0) + 1)
                                    }) { Text("+") }
                                }
                            }
                        } else {
                            FilterChip(
                                selected = picked != null,
                                onClick = { onToggleExtra(group, optionId) },
                                label = {
                                    val extra = if (opt.extraPrice > 0) " +${currency.format(opt.extraPrice)}" else ""
                                    Text("${opt.name}$extra")
                                },
                            )
                        }
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
    componentModifierGroups: Map<Int, List<ModifierGroup>> = emptyMap(),
    componentModifiers: Map<Int, List<CartModifierEntry>> = emptyMap(),
    componentProductNames: Map<Int, String> = emptyMap(),
    componentPresentations: Map<Int, List<ProductPresentation>> = emptyMap(),
    onToggleComponentModifier: (Int, ModifierGroup, Int) -> Unit = { _, _, _ -> },
    onSelectComponentPresentation: (Int, ProductPresentation) -> Unit = { _, _ -> },
    onToggleSlot: (ComboSlot, Int) -> Unit,
    onKitchenNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuredProductIds = buildSet {
        componentModifierGroups.keys.forEach { add(it) }
        componentPresentations.keys.forEach { add(it) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(combo.name, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (loading) Text("Cargando combo…", color = BendeyColors.OnSurfaceVariant)
                form?.fixedItems?.takeIf { it.isNotEmpty() }?.let { fixedItems ->
                    Text("Incluye", style = MaterialTheme.typography.labelLarge)
                    fixedItems.forEach { item ->
                        val name = componentProductNames[item.productId] ?: "Producto #${item.productId}"
                        Text("· $name", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    }
                }
                form?.slots?.forEach { slot ->
                    Text(slot.name, style = MaterialTheme.typography.labelLarge)
                    slot.options.forEach { opt ->
                        val optionId = opt.id ?: return@forEach
                        val picked = selections.any { it.slotId == (slot.id ?: 0) && it.optionId == optionId }
                        FilterChip(
                            selected = picked,
                            onClick = { onToggleSlot(slot, optionId) },
                            label = {
                                val label = opt.productName ?: componentProductNames[opt.productId] ?: "Opción $optionId"
                                val upgrade = if (opt.upgradePrice > 0) " +${currency.format(opt.upgradePrice)}" else ""
                                Text("$label$upgrade")
                            },
                        )
                    }
                }
                configuredProductIds.forEach { productId ->
                    val productName = componentProductNames[productId] ?: "Producto #$productId"
                    val groups = componentModifierGroups[productId].orEmpty()
                    val presentations = componentPresentations[productId].orEmpty()
                    if (groups.isEmpty() && presentations.isEmpty()) return@forEach
                    Text("Extras: $productName", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (presentations.isNotEmpty()) {
                        Text("Presentación", style = MaterialTheme.typography.bodySmall)
                        presentations.forEach { pres ->
                            val picked = componentModifiers[productId]?.any {
                                it.type == "variant" && it.optionId == (pres.id ?: 0)
                            } == true
                            FilterChip(
                                selected = picked,
                                onClick = { onSelectComponentPresentation(productId, pres) },
                                label = { Text("${pres.name} · ${currency.format(pres.salePrice)}") },
                            )
                        }
                    }
                    groups.forEach { group ->
                        Text(group.name + if (group.required) " *" else "", style = MaterialTheme.typography.bodySmall)
                        group.options.forEach { opt ->
                            val optionId = opt.id ?: return@forEach
                            val picked = componentModifiers[productId]?.any {
                                it.type == "modifier" && it.optionId == optionId
                            } == true
                            FilterChip(
                                selected = picked,
                                onClick = { onToggleComponentModifier(productId, group, optionId) },
                                label = {
                                    val extra = if (opt.extraPrice > 0) " +${currency.format(opt.extraPrice)}" else ""
                                    Text("${opt.name}$extra")
                                },
                            )
                        }
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
