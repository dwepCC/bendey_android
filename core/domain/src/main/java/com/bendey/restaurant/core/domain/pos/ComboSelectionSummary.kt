package com.bendey.restaurant.core.domain.pos

import com.bendey.restaurant.core.domain.catalog.ComboSlot

data class ComboSelectionSummaryItem(
    val optionId: Int? = null,
    val optionName: String,
    val quantity: Double,
    val presentationName: String? = null,
    val modifiersJson: String? = null,
)

data class SlotQuantityStatus(
    val prompt: String,
    val progress: String,
    val hint: String?,
    val complete: Boolean,
)

data class KitchenComponentDisplay(
    val displayName: String,
    val displayQuantity: Double,
)

fun normalizeSummaryQuantity(quantity: Double): Double {
    if (quantity <= 0) return 1.0
    return quantity
}

fun formatQuantitySuffix(quantity: Double): String {
    val q = normalizeSummaryQuantity(quantity)
    val label = if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()
    return "×$label"
}

/** Americana | Americana ×2 */
fun formatSelectionLabel(name: String, quantity: Double): String {
    val base = name.trim()
    if (base.isEmpty()) return ""
    val q = normalizeSummaryQuantity(quantity)
    if (q <= 1.0) return base
    return "${base} ${formatQuantitySuffix(q)}"
}

private fun summaryGroupKey(item: ComboSelectionSummaryItem): String =
    listOf(
        item.optionId?.toString().orEmpty(),
        item.optionName.trim().lowercase(),
        item.presentationName?.trim()?.lowercase().orEmpty(),
        item.modifiersJson?.trim().orEmpty(),
    ).joinToString("|")

fun groupSelections(items: List<ComboSelectionSummaryItem>): List<ComboSelectionSummaryItem> {
    val map = linkedMapOf<String, ComboSelectionSummaryItem>()
    for (item in items) {
        val name = item.optionName.trim()
        if (name.isEmpty()) continue
        val normalized = item.copy(optionName = name, quantity = normalizeSummaryQuantity(item.quantity))
        val key = summaryGroupKey(normalized)
        val existing = map[key]
        if (existing != null) {
            map[key] = existing.copy(quantity = existing.quantity + normalized.quantity)
        } else {
            map[key] = normalized
        }
    }
    return map.values.toList()
}

fun formatGroupedSelectionLabels(items: List<ComboSelectionSummaryItem>): List<String> =
    groupSelections(items).map { formatSelectionLabel(it.optionName, it.quantity) }

fun snapshotComponentName(productName: String, presentationName: String?): String {
    val name = productName.trim()
    val pres = presentationName?.trim().orEmpty()
    if (name.isEmpty()) return ""
    return if (pres.isNotEmpty()) "$name ($pres)" else name
}

fun snapshotComponentsToSummaryItems(
    components: List<ComboSnapshotComponentInput>,
): List<ComboSelectionSummaryItem> =
    components.mapNotNull { comp ->
        val optionName = snapshotComponentName(comp.productName, comp.presentationName)
        if (optionName.isEmpty()) return@mapNotNull null
        ComboSelectionSummaryItem(
            optionName = optionName,
            quantity = normalizeSummaryQuantity(comp.quantity),
            presentationName = comp.presentationName?.trim()?.takeIf { it.isNotEmpty() },
            modifiersJson = comp.modifiersJson?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

data class ComboSnapshotComponentInput(
    val productName: String,
    val presentationName: String? = null,
    val quantity: Double = 1.0,
    val modifiersJson: String? = null,
)

fun formatSnapshotComponentLines(
    components: List<ComboSnapshotComponentInput>,
    lineMultiplier: Double = 1.0,
): List<String> {
    val mult = if (lineMultiplier > 0) lineMultiplier else 1.0
    return groupSelections(snapshotComponentsToSummaryItems(components)).map { item ->
        formatSelectionLabel(item.optionName, item.quantity * mult)
    }
}

fun calculateTotalUnits(slot: ComboSlot, selections: List<ComboSlotSelection>): Int =
    slotPickUnits(slot, selections)

fun remainingUnits(slot: ComboSlot, selections: List<ComboSlotSelection>): Int {
    val required = if (slot.maxPick > 0) slot.maxPick else slot.minPick.coerceAtLeast(1)
    return (required - calculateTotalUnits(slot, selections)).coerceAtLeast(0)
}

fun isSlotSelectionComplete(slot: ComboSlot, selections: List<ComboSlotSelection>): Boolean {
    val selected = calculateTotalUnits(slot, selections)
    val min = slot.minPick.coerceAtLeast(1)
    val required = if (slot.maxPick > 0) slot.maxPick else min
    return selected >= required && selected >= min
}

fun canDecrease(currentQuantity: Int): Boolean = currentQuantity > 0

fun canIncrease(slot: ComboSlot, selections: List<ComboSlotSelection>): Boolean {
    val max = slot.maxPick
    if (max <= 0) return true
    return calculateTotalUnits(slot, selections) < max
}

fun formatSlotQuantityStatus(slot: ComboSlot, selections: List<ComboSlotSelection>): SlotQuantityStatus {
    val required = if (slot.maxPick > 0) slot.maxPick else slot.minPick.coerceAtLeast(1)
    val selected = calculateTotalUnits(slot, selections)
    val remaining = (required - selected).coerceAtLeast(0)
    val complete = isSlotSelectionComplete(slot, selections)
    return SlotQuantityStatus(
        prompt = "Seleccione $required ${slot.name.trim()}",
        progress = "$selected / $required seleccionadas",
        hint = when {
            complete -> "✓ Completo"
            remaining > 0 -> "Falta elegir $remaining"
            else -> null
        },
        complete = complete,
    )
}

fun formatKitchenComponentDisplay(
    productName: String,
    componentQuantity: Double,
    comandaLineQty: Double = 1.0,
): KitchenComponentDisplay {
    val mult = if (comandaLineQty > 0) comandaLineQty else 1.0
    val total = normalizeSummaryQuantity(componentQuantity) * mult
    return if (total <= 1.0) {
        KitchenComponentDisplay(displayName = productName.trim(), displayQuantity = 1.0)
    } else {
        KitchenComponentDisplay(
            displayName = formatSelectionLabel(productName.trim(), total),
            displayQuantity = 1.0,
        )
    }
}

const val COMBO_QUANTITY_MODE_HELP =
    "Modo cantidad\n\n" +
        "El cliente podrá seleccionar varias unidades de un mismo producto.\n\n" +
        "Ejemplos:\n\n" +
        "✓ Americana ×2\n\n" +
        "✓ Americana ×1 + Margarita ×1\n\n" +
        "✓ Cualquier combinación cuya suma respete el mínimo y máximo configurado."
