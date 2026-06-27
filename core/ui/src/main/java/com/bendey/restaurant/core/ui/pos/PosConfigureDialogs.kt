package com.bendey.restaurant.core.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.pos.CartModifierEntry
import com.bendey.restaurant.core.domain.pos.ComboSlotSelection
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.domain.pos.calcUnitPriceWithModifiers
import com.bendey.restaurant.core.domain.pos.canDecrease
import com.bendey.restaurant.core.domain.pos.canIncrease
import com.bendey.restaurant.core.domain.pos.formatSlotQuantityStatus
import com.bendey.restaurant.core.domain.pos.selectionPickQuantity
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.ui.components.BendeyConfigureFullscreenDialog
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
                colors = BendeyChipDefaults.posFilterChipColors(),
                shape = BendeyShapeTokens.chip,
                border = null,
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
    val activePresentations = presentations.filter { it.active && it.name.isNotBlank() }

    BendeyConfigureFullscreenDialog(
        onDismissRequest = onDismiss,
        title = product.name,
        loading = loading,
        confirmEnabled = !loading,
        footerSummary = currency.format(unitPrice),
        validationError = validationError,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        if (activePresentations.isNotEmpty()) {
            ConfigureOptionsGroup(title = "Presentación") {
                activePresentations.forEach { pres ->
                    val picked = selected.any { it.type == "variant" && it.optionId == (pres.id ?: 0) }
                    ConfigureSelectionChip(
                        label = "${pres.name} · ${currency.format(pres.salePrice)}",
                        selected = picked,
                        onClick = { onSelectPresentation(pres) },
                    )
                }
            }
        }

        extraGroups.forEach { group ->
            ConfigureOptionsGroup(title = group.name, required = group.required) {
                group.options.forEach { opt ->
                    val optionId = opt.id ?: return@forEach
                    val picked = selected.firstOrNull { it.type == "modifier" && it.optionId == optionId }
                    if (group.selectionMode == ModifierSelectionMode.QUANTITY) {
                        val qty = picked?.quantity ?: 0
                        val extraLabel = if (opt.extraPrice > 0) "(+${currency.format(opt.extraPrice)})" else null
                        ConfigureQuantityOptionRow(
                            name = opt.name,
                            extraPriceLabel = extraLabel,
                            quantity = qty,
                            onDecrease = {
                                onSetExtraQuantity(group, optionId, qty - 1)
                            },
                            onIncrease = {
                                onSetExtraQuantity(group, optionId, qty + 1)
                            },
                            decreaseEnabled = qty > 0,
                            increaseEnabled = true,
                        )
                    } else {
                        val extra = if (opt.extraPrice > 0) " (+${currency.format(opt.extraPrice)})" else ""
                        ConfigureSelectionChip(
                            label = "${opt.name}$extra",
                            selected = picked != null,
                            onClick = { onToggleExtra(group, optionId) },
                        )
                    }
                }
            }
        }

        ConfigureKitchenNoteField(kitchenNote, onKitchenNoteChange)
    }
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
    onSetSlotQuantity: (ComboSlot, Int, Int) -> Unit = { _, _, _ -> },
    onKitchenNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuredProductIds = buildSet {
        componentModifierGroups.keys.forEach { add(it) }
        componentPresentations.keys.forEach { add(it) }
    }
    val displayPrice = resolvedPrice ?: combo.basePrice

    BendeyConfigureFullscreenDialog(
        onDismissRequest = onDismiss,
        title = combo.name,
        subtitle = combo.description?.takeIf { it.isNotBlank() },
        loading = loading,
        loadingMessage = "Cargando combo…",
        confirmEnabled = !loading,
        footerSummary = currency.format(displayPrice),
        validationError = validationError,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        form?.fixedItems?.takeIf { it.isNotEmpty() }?.let { fixedItems ->
            ConfigureOptionsGroup(title = "Incluye") {
                fixedItems.forEach { item ->
                    val name = componentProductNames[item.productId] ?: "Producto #${item.productId}"
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }

        form?.slots?.forEach { slot ->
            val slotId = slot.id ?: 0
            val mode = slot.selectionMode
            ConfigureOptionsGroup(title = slot.name) {
                if (mode == ModifierSelectionMode.QUANTITY) {
                    val status = formatSlotQuantityStatus(slot, selections)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = status.progress,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        status.hint?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status.complete) BendeyColors.Success else BendeyColors.Warning,
                            )
                        }
                    }
                }
                slot.options.forEach { opt ->
                    val optionId = opt.id ?: return@forEach
                    val picked = selections.firstOrNull { it.slotId == slotId && it.optionId == optionId }
                    val label = opt.productName ?: componentProductNames[opt.productId] ?: "Opción $optionId"
                    val upgradeLabel = if (opt.upgradePrice > 0) "(+${currency.format(opt.upgradePrice)})" else null
                    if (mode == ModifierSelectionMode.QUANTITY) {
                        val qty = picked?.let { selectionPickQuantity(it) } ?: 0
                        ConfigureQuantityOptionRow(
                            name = label,
                            extraPriceLabel = upgradeLabel,
                            quantity = qty,
                            onDecrease = { onSetSlotQuantity(slot, optionId, qty - 1) },
                            onIncrease = { onSetSlotQuantity(slot, optionId, qty + 1) },
                            decreaseEnabled = canDecrease(qty),
                            increaseEnabled = canIncrease(slot, selections),
                        )
                    } else {
                        val upgrade = if (opt.upgradePrice > 0) " (+${currency.format(opt.upgradePrice)})" else ""
                        ConfigureSelectionChip(
                            label = "$label$upgrade",
                            selected = picked != null,
                            onClick = { onToggleSlot(slot, optionId) },
                        )
                    }
                }
            }
        }

        configuredProductIds.forEach { productId ->
            val productName = componentProductNames[productId] ?: "Producto #$productId"
            val groups = componentModifierGroups[productId].orEmpty()
            val presentations = componentPresentations[productId].orEmpty()
            if (groups.isEmpty() && presentations.isEmpty()) return@forEach

            ConfigureOptionsGroup(title = productName) {
                presentations.forEach { pres ->
                    val picked = componentModifiers[productId]?.any {
                        it.type == "variant" && it.optionId == (pres.id ?: 0)
                    } == true
                    ConfigureSelectionChip(
                        label = "${pres.name} · ${currency.format(pres.salePrice)}",
                        selected = picked,
                        onClick = { onSelectComponentPresentation(productId, pres) },
                    )
                }
                groups.forEach { group ->
                    if (presentations.isNotEmpty() || groups.size > 1) {
                        Text(
                            text = group.name + if (group.required) " *" else "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    group.options.forEach { opt ->
                        val optionId = opt.id ?: return@forEach
                        val picked = componentModifiers[productId]?.any {
                            it.type == "modifier" && it.optionId == optionId
                        } == true
                        val extra = if (opt.extraPrice > 0) " (+${currency.format(opt.extraPrice)})" else ""
                        ConfigureSelectionChip(
                            label = "${opt.name}$extra",
                            selected = picked,
                            onClick = { onToggleComponentModifier(productId, group, optionId) },
                        )
                    }
                }
            }
        }

        ConfigureKitchenNoteField(kitchenNote, onKitchenNoteChange)
    }
}
