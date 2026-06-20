package com.bendey.restaurant.core.domain.pos

import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboType
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import kotlin.math.round

enum class PosCatalogTab(val label: String) {
    PRODUCTS("Productos"),
    COMBOS("Combos"),
}

data class ProductConfigureState(
    val product: com.bendey.restaurant.core.domain.restaurant.PosProduct,
    val loading: Boolean = true,
    val presentations: List<ProductPresentation> = emptyList(),
    val extraGroups: List<ModifierGroup> = emptyList(),
    val selected: List<CartModifierEntry> = emptyList(),
    val kitchenNote: String = "",
    val error: String? = null,
)

data class ComboConfigureState(
    val combo: PosComboItem,
    val loading: Boolean = true,
    val form: ComboFormInput? = null,
    val selections: List<ComboSlotSelection> = emptyList(),
    val kitchenNote: String = "",
    val resolvedPrice: Double? = null,
    val error: String? = null,
)

data class CartModifierEntry(
    val groupId: Int,
    val groupName: String,
    val type: String,
    val optionId: Int,
    val optionName: String,
    val extraPrice: Double,
    val quantity: Int = 1,
)

data class ComboSlotSelection(val slotId: Int, val optionId: Int)

data class ComboComponentModifier(val productId: Int, val modifiers: List<CartModifierEntry>)

data class ComboCartConfig(
    val selections: List<ComboSlotSelection> = emptyList(),
    val componentModifiers: List<ComboComponentModifier> = emptyList(),
)

data class PosComboItem(
    val id: Int,
    val name: String,
    val description: String?,
    val comboType: ComboType,
    val basePrice: Double,
    val imageUrl: String?,
)

fun productNeedsConfiguration(product: PosProduct): Boolean =
    product.hasModifiers || product.hasVariants

fun comboNeedsConfiguration(combo: PosComboItem): Boolean =
    combo.comboType == ComboType.CONFIGURABLE || combo.comboType == ComboType.BUILD_YOUR_OWN

fun comboNeedsConfiguration(form: ComboFormInput): Boolean =
    form.comboType == ComboType.CONFIGURABLE || form.comboType == ComboType.BUILD_YOUR_OWN

fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

fun modifierEntryQuantity(entry: CartModifierEntry): Int =
    if (entry.type == "modifier" && entry.quantity > 0) entry.quantity else 1

fun calcUnitPriceWithModifiers(basePrice: Double, modifiers: List<CartModifierEntry>): Double {
    val presentation = modifiers.firstOrNull { it.type == "variant" }
    val extras = modifiers.filter { it.type == "modifier" }
    var unit = basePrice
    if (presentation != null && presentation.extraPrice > 0) {
        unit = presentation.extraPrice
    }
    val extrasSum = extras.sumOf { (it.extraPrice) * modifierEntryQuantity(it) }
    return roundMoney(unit + extrasSum)
}

fun buildConfigureKey(modifiers: List<CartModifierEntry>, kitchenNote: String): String {
    val modPart = modifiers
        .map { "${it.type}:${it.optionId}:q${modifierEntryQuantity(it)}" }
        .sorted()
        .joinToString("|")
    val note = kitchenNote.trim().replace(Regex("\\s+"), " ")
    return "p-$modPart-n-$note"
}

fun buildComboConfigJson(config: ComboCartConfig): String {
    val selections = config.selections.joinToString(",") {
        """{"slot_id":${it.slotId},"option_id":${it.optionId}}"""
    }
    val componentModifiers = config.componentModifiers.joinToString(",") { cm ->
        """{"product_id":${cm.productId},"modifiers_json":${jsonString(modifiersToJson(cm.modifiers))}}"""
    }
    return """{"selections":[$selections],"component_modifiers":[$componentModifiers]}"""
}

fun modifiersToJson(modifiers: List<CartModifierEntry>): String {
    if (modifiers.isEmpty()) return ""
    val items = modifiers.joinToString(",") { m ->
        buildString {
            append("""{"group_id":${m.groupId},"group_name":${jsonString(m.groupName)},"type":${jsonString(m.type)},"option_id":${m.optionId},"option_name":${jsonString(m.optionName)},"extra_price":${m.extraPrice}""")
            if (m.quantity > 1) append(""","quantity":${m.quantity}""")
            append("}")
        }
    }
    return "[$items]"
}

private fun jsonString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun modifierSummaryText(modifiers: List<CartModifierEntry>): String =
    modifiers.joinToString(", ") { entry ->
        val qty = modifierEntryQuantity(entry)
        if (qty > 1) "${entry.optionName} x$qty" else entry.optionName
    }

fun getProductExtraGroups(
    modifierGroupIds: List<Int>,
    allGroups: List<ModifierGroup>,
    product: PosProduct,
): List<ModifierGroup> {
    if (!product.hasModifiers) return emptyList()
    return allGroups.filter { modifierGroupIds.contains(it.id) }
}

fun selectionFromPresentation(presentation: ProductPresentation): CartModifierEntry =
    CartModifierEntry(
        groupId = 0,
        groupName = "Presentación",
        type = "variant",
        optionId = presentation.id ?: 0,
        optionName = presentation.name.trim(),
        extraPrice = presentation.salePrice,
    )

fun toggleExtraSelection(
    selected: List<CartModifierEntry>,
    group: ModifierGroup,
    optionId: Int,
): List<CartModifierEntry> {
    val opt = group.options.firstOrNull { it.id == optionId } ?: return selected
    val entry = CartModifierEntry(
        groupId = group.id,
        groupName = group.name,
        type = "modifier",
        optionId = opt.id ?: optionId,
        optionName = opt.name,
        extraPrice = opt.extraPrice,
    )
    val exists = selected.any { it.type == "modifier" && it.optionId == (opt.id ?: optionId) }
    if (exists) {
        return selected.filterNot { it.type == "modifier" && it.optionId == (opt.id ?: optionId) }
    }
    if (group.selectionMode != ModifierSelectionMode.MULTIPLE && group.selectionMode != ModifierSelectionMode.QUANTITY) {
        return selected.filterNot { it.type == "modifier" && it.groupId == group.id } + entry
    }
    return selected + entry
}

fun selectPresentation(
    selected: List<CartModifierEntry>,
    presentation: ProductPresentation,
): List<CartModifierEntry> {
    val without = selected.filterNot { it.type == "variant" }
    return without + selectionFromPresentation(presentation)
}

fun validateModifierSelection(
    presentations: List<ProductPresentation>,
    extraGroups: List<ModifierGroup>,
    selected: List<CartModifierEntry>,
    product: PosProduct,
): String? {
    val activePres = presentations.filter { it.name.isNotBlank() && it.active }
    if (product.hasVariants && activePres.isNotEmpty()) {
        val picked = selected.count { it.type == "variant" }
        if (picked != 1) return "Elige una presentación del producto"
    }
    for (group in extraGroups) {
        val picked = selected.filter { it.type == "modifier" && it.groupId == group.id }
        val min = if (group.minSelect > 0) group.minSelect else if (group.required) 1 else 0
        val max = group.maxSelect.takeIf { it > 0 }
            ?: if (group.selectionMode == ModifierSelectionMode.SINGLE) 1 else 0
        if (min > 0 && picked.size < min) return "Elige al menos $min en «${group.name}»"
        if (max > 0 && picked.size > max) return "Máximo $max en «${group.name}»"
        if (group.required && picked.isEmpty() && min == 0) return "Elige al menos un extra en «${group.name}»"
    }
    return null
}

fun validateSlotSelections(slots: List<com.bendey.restaurant.core.domain.catalog.ComboSlot>, selections: List<ComboSlotSelection>): String? {
    val bySlot = selections.groupingBy { it.slotId }.eachCount()
    for (slot in slots) {
        val slotId = slot.id ?: 0
        val n = bySlot[slotId] ?: 0
        val min = slot.minPick
        val max = slot.maxPick
        if (n < min) return "Elige al menos $min en «${slot.name}»"
        if (max > 0 && n > max) return "Máximo $max en «${slot.name}»"
    }
    return null
}

fun toggleSlotOption(
    selections: List<ComboSlotSelection>,
    slot: com.bendey.restaurant.core.domain.catalog.ComboSlot,
    optionId: Int,
): List<ComboSlotSelection> {
    val slotId = slot.id ?: 0
    val exists = selections.any { it.slotId == slotId && it.optionId == optionId }
    val max = slot.maxPick
    val multi = max == 0 || max > 1
    if (exists) {
        return selections.filterNot { it.slotId == slotId && it.optionId == optionId }
    }
    val current = selections.count { it.slotId == slotId }
    return when {
        !multi -> selections.filterNot { it.slotId == slotId } + ComboSlotSelection(slotId, optionId)
        max > 0 && current >= max -> selections
        else -> selections + ComboSlotSelection(slotId, optionId)
    }
}

fun posComboFromListItem(item: com.bendey.restaurant.core.domain.catalog.ComboItem) = PosComboItem(
    id = item.id,
    name = item.name,
    description = item.description,
    comboType = item.comboType,
    basePrice = item.basePrice,
    imageUrl = null,
)

fun buildComboConfigureKey(comboId: Int, config: ComboCartConfig, notes: String, unitPrice: Double): String {
    val sel = config.selections.map { "${it.slotId}:${it.optionId}" }.sorted().joinToString("|")
    val mods = config.componentModifiers
        .map { "${it.productId}:${modifiersToJson(it.modifiers)}" }
        .sorted()
        .joinToString("|")
    return "combo-$comboId|$sel|$mods|n-${notes.trim()}|u-${"%.2f".format(unitPrice)}"
}

fun appendCartLine(cart: List<com.bendey.restaurant.core.domain.restaurant.PosCartLine>, line: com.bendey.restaurant.core.domain.restaurant.PosCartLine): List<com.bendey.restaurant.core.domain.restaurant.PosCartLine> {
    val idx = cart.indexOfFirst { it.key == line.key }
    return if (idx >= 0) {
        cart.toMutableList().apply {
            this[idx] = this[idx].copy(quantity = this[idx].quantity + line.quantity)
        }
    } else {
        cart + line
    }
}

fun decrementCartLine(cart: List<com.bendey.restaurant.core.domain.restaurant.PosCartLine>, cartKey: String): List<com.bendey.restaurant.core.domain.restaurant.PosCartLine> =
    cart.mapNotNull { line ->
        when {
            line.key != cartKey -> line
            line.quantity <= 1 -> null
            else -> line.copy(quantity = line.quantity - 1)
        }
    }

fun com.bendey.restaurant.core.domain.restaurant.PosCartLine.toOrderItemInput(): com.bendey.restaurant.core.domain.restaurant.OrderItemInput =
    if (itemKind == "combo") {
        com.bendey.restaurant.core.domain.restaurant.OrderItemInput(
            itemKind = "combo",
            productName = product.name,
            quantity = quantity.toDouble(),
            unitPrice = effectiveUnitPrice,
            notes = notes.takeIf { it.isNotBlank() },
            comboId = comboId,
            comboConfigJson = comboConfigJson,
        )
    } else {
        com.bendey.restaurant.core.domain.restaurant.OrderItemInput(
            itemKind = "product",
            productId = product.id,
            productCode = product.code,
            productName = product.name,
            quantity = quantity.toDouble(),
            unitPrice = effectiveUnitPrice,
            notes = notes.takeIf { it.isNotBlank() },
            modifiersJson = modifiersJson,
            igvAffectationType = product.igvAffectationType,
            priceIncludesIgv = product.priceIncludesIgv,
        )
    }
