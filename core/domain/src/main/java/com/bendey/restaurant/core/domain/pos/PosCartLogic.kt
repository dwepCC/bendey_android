package com.bendey.restaurant.core.domain.pos

import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboType
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.products.toFormInput
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
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
    val componentModifiers: Map<Int, List<CartModifierEntry>> = emptyMap(),
    val componentModifierGroups: Map<Int, List<ModifierGroup>> = emptyMap(),
    val componentProductNames: Map<Int, String> = emptyMap(),
    val componentPresentations: Map<Int, List<ProductPresentation>> = emptyMap(),
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

data class ComboSlotSelection(val slotId: Int, val optionId: Int, val quantity: Int = 1)

const val MAX_COMBO_SLOT_PICK_QTY = 99

fun selectionPickQuantity(sel: ComboSlotSelection): Int {
    val q = sel.quantity
    if (q <= 0) return 1
    return q.coerceAtMost(MAX_COMBO_SLOT_PICK_QTY)
}

fun slotPickUnits(slot: com.bendey.restaurant.core.domain.catalog.ComboSlot, selections: List<ComboSlotSelection>): Int {
    val slotId = slot.id ?: 0
    val slotSels = selections.filter { it.slotId == slotId }
    return if (slot.selectionMode == ModifierSelectionMode.QUANTITY) {
        slotSels.sumOf { selectionPickQuantity(it) }
    } else {
        slotSels.size
    }
}

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
    val selections = config.selections.joinToString(",") { sel ->
        val q = selectionPickQuantity(sel)
        if (q > 1) {
            """{"slot_id":${sel.slotId},"option_id":${sel.optionId},"quantity":$q}"""
        } else {
            """{"slot_id":${sel.slotId},"option_id":${sel.optionId}}"""
        }
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
            when {
                m.type == "modifier" && m.quantity > 0 ->
                    append(""","quantity":${modifierEntryQuantity(m)}""")
                m.quantity > 1 ->
                    append(""","quantity":${m.quantity}""")
            }
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

fun modifierUnits(selected: List<CartModifierEntry>, groupId: Int): Int =
    selected.filter { it.type == "modifier" && it.groupId == groupId }
        .sumOf { modifierEntryQuantity(it) }

fun setOptionQuantity(
    selected: List<CartModifierEntry>,
    group: ModifierGroup,
    optionId: Int,
    quantity: Int,
): List<CartModifierEntry> {
    val opt = group.options.firstOrNull { it.id == optionId } ?: return selected
    val id = opt.id ?: optionId
    val without = selected.filterNot { it.type == "modifier" && it.optionId == id }
    if (quantity <= 0) return without
    val entry = CartModifierEntry(
        groupId = group.id,
        groupName = group.name,
        type = "modifier",
        optionId = id,
        optionName = opt.name,
        extraPrice = opt.extraPrice,
        quantity = quantity.coerceAtLeast(1),
    )
    if (group.selectionMode != ModifierSelectionMode.MULTIPLE && group.selectionMode != ModifierSelectionMode.QUANTITY) {
        return without.filterNot { it.type == "modifier" && it.groupId == group.id } + entry
    }
    return without + entry
}

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
        val units = modifierUnits(selected, group.id)
        val min = if (group.minSelect > 0) group.minSelect else if (group.required) 1 else 0
        val max = group.maxSelect.takeIf { it > 0 }
            ?: if (group.selectionMode == ModifierSelectionMode.SINGLE) 1 else 0
        val countForMin = if (group.selectionMode == ModifierSelectionMode.QUANTITY) units else picked.size
        val countForMax = if (group.selectionMode == ModifierSelectionMode.QUANTITY) units else picked.size
        if (min > 0 && countForMin < min) return "Elige al menos $min en «${group.name}»"
        if (max > 0 && countForMax > max) return "Máximo $max en «${group.name}»"
        if (group.required && picked.isEmpty() && min == 0) return "Elige al menos un extra en «${group.name}»"
    }
    return null
}

fun validateSlotSelections(slots: List<com.bendey.restaurant.core.domain.catalog.ComboSlot>, selections: List<ComboSlotSelection>): String? {
    for (slot in slots) {
        if (slot.selectionMode == ModifierSelectionMode.SINGLE) {
            val slotId = slot.id ?: 0
            for (sel in selections.filter { it.slotId == slotId }) {
                if (selectionPickQuantity(sel) != 1) return "Cantidad inválida en «${slot.name}»"
            }
        }
        val units = slotPickUnits(slot, selections)
        val min = slot.minPick
        val max = slot.maxPick
        if (units < min) return "Elige al menos $min en «${slot.name}»"
        if (max > 0 && units > max) return "Máximo $max en «${slot.name}»"
    }
    return null
}

fun setSlotOptionQuantity(
    selections: List<ComboSlotSelection>,
    slot: com.bendey.restaurant.core.domain.catalog.ComboSlot,
    optionId: Int,
    quantity: Int,
): List<ComboSlotSelection> {
    val slotId = slot.id ?: 0
    val without = selections.filterNot { it.slotId == slotId && it.optionId == optionId }
    val q = quantity.coerceIn(0, MAX_COMBO_SLOT_PICK_QTY)
    if (q <= 0) return without
    val max = slot.maxPick
    if (max > 0) {
        val others = without.filter { it.slotId == slotId }.sumOf { selectionPickQuantity(it) }
        if (others + q > max) return selections
    }
    return without + ComboSlotSelection(slotId, optionId, q)
}

fun toggleSlotOption(
    selections: List<ComboSlotSelection>,
    slot: com.bendey.restaurant.core.domain.catalog.ComboSlot,
    optionId: Int,
): List<ComboSlotSelection> {
    val slotId = slot.id ?: 0
    val exists = selections.any { it.slotId == slotId && it.optionId == optionId }
    val max = slot.maxPick
    val multi = slot.selectionMode == ModifierSelectionMode.MULTIPLE && (max == 0 || max > 1)
    if (exists) {
        return selections.filterNot { it.slotId == slotId && it.optionId == optionId }
    }
    return when {
        slot.selectionMode == ModifierSelectionMode.SINGLE || !multi ->
            selections.filterNot { it.slotId == slotId } + ComboSlotSelection(slotId, optionId)
        max > 0 && selections.count { it.slotId == slotId } >= max -> selections
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

fun allComboFormProductIds(form: ComboFormInput): List<Int> {
    val ids = linkedSetOf<Int>()
    form.fixedItems.forEach { item ->
        if (item.productId > 0) ids.add(item.productId)
    }
    form.slots.forEach { slot ->
        slot.options.forEach { option ->
            if (option.productId > 0) ids.add(option.productId)
        }
    }
    return ids.toList()
}

fun productNamesFromCatalog(productIds: Collection<Int>, catalog: List<PosProduct>): Map<Int, String> {
    if (productIds.isEmpty() || catalog.isEmpty()) return emptyMap()
    val byId = catalog.associateBy { it.id }
    return productIds.mapNotNull { id -> byId[id]?.let { id to it.name } }.toMap()
}

fun selectedComboProductIds(form: ComboFormInput, selections: List<ComboSlotSelection>): List<Int> {
    val ids = linkedSetOf<Int>()
    form.fixedItems.forEach { item ->
        if (item.productId > 0) ids.add(item.productId)
    }
    selections.forEach { sel ->
        val slot = form.slots.firstOrNull { (it.id ?: 0) == sel.slotId } ?: return@forEach
        slot.options.firstOrNull { (it.id ?: 0) == sel.optionId }?.productId?.let { ids.add(it) }
    }
    return ids.toList()
}

fun comboComponentModifiersList(map: Map<Int, List<CartModifierEntry>>): List<ComboComponentModifier> =
    map.map { (productId, modifiers) -> ComboComponentModifier(productId, modifiers) }

fun buildComboConfigureKey(comboId: Int, config: ComboCartConfig, notes: String, unitPrice: Double): String {
    val sel = config.selections.map { "${it.slotId}:${it.optionId}:q${selectionPickQuantity(it)}" }.sorted().joinToString("|")
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

fun updateCartLineNotes(
    cart: List<com.bendey.restaurant.core.domain.restaurant.PosCartLine>,
    cartKey: String,
    notes: String,
): List<com.bendey.restaurant.core.domain.restaurant.PosCartLine> =
    cart.map { line -> if (line.key == cartKey) line.copy(notes = notes.trim()) else line }

fun updateCartLineUnitPrice(
    cart: List<com.bendey.restaurant.core.domain.restaurant.PosCartLine>,
    cartKey: String,
    rawPrice: String,
): List<com.bendey.restaurant.core.domain.restaurant.PosCartLine> {
    val price = rawPrice.replace(',', '.').trim().toDoubleOrNull() ?: return cart
    return cart.map { line ->
        if (line.key == cartKey) line.copy(unitPrice = roundMoney(price.coerceAtLeast(0.0))) else line
    }
}

fun com.bendey.restaurant.core.domain.restaurant.PosCartLine.toOrderItemInput(): com.bendey.restaurant.core.domain.restaurant.OrderItemInput =
    when (itemKind) {
        "combo" -> com.bendey.restaurant.core.domain.restaurant.OrderItemInput(
            itemKind = "combo",
            productName = product.name,
            quantity = quantity.toDouble(),
            unitPrice = effectiveUnitPrice,
            notes = notes.takeIf { it.isNotBlank() },
            comboId = comboId,
            comboConfigJson = comboConfigJson,
        )
        "manual" -> com.bendey.restaurant.core.domain.restaurant.OrderItemInput(
            itemKind = "product",
            productId = null,
            productCode = product.code.ifBlank { "MANUAL" },
            productName = product.name,
            quantity = quantity.toDouble(),
            unitPrice = effectiveUnitPrice,
            notes = notes.takeIf { it.isNotBlank() },
            modifiersJson = "",
            igvAffectationType = product.igvAffectationType ?: "10",
            priceIncludesIgv = product.priceIncludesIgv ?: true,
        )
        else -> com.bendey.restaurant.core.domain.restaurant.OrderItemInput(
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

/** Crea línea de carrito para producto manual (paridad ManualProductModal). */
fun manualCartLine(input: ManualProductInput): com.bendey.restaurant.core.domain.restaurant.PosCartLine {
    val desc = input.description.trim()
    val qty = input.quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val price = input.unitPrice.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    val lineId = "manual-${System.currentTimeMillis()}"
    val igvLabel = when (input.igvAffectationType.trim()) {
        "10" -> "Gravado IGV"
        "20" -> "Exonerado"
        "30" -> "Inafecto"
        else -> "IGV ${input.igvAffectationType}"
    }
    return com.bendey.restaurant.core.domain.restaurant.PosCartLine(
        cartKey = lineId,
        product = com.bendey.restaurant.core.domain.restaurant.PosProduct(
            id = 0,
            code = input.code.trim().ifBlank { "MANUAL" },
            name = desc,
            salePrice = price,
            categoryId = null,
            imageUrl = null,
            igvAffectationType = input.igvAffectationType,
            priceIncludesIgv = input.priceIncludesIgv,
        ),
        quantity = qty,
        notes = input.notes.trim(),
        itemKind = "manual",
        unitPrice = price,
        subtitle = "Manual · $igvLabel",
    )
}

data class ComboComponentMetadata(
    val modifierGroups: Map<Int, List<ModifierGroup>> = emptyMap(),
    val productNames: Map<Int, String> = emptyMap(),
    val presentations: Map<Int, List<ProductPresentation>> = emptyMap(),
)

suspend fun resolveComboComponentProductNames(
    productsRepository: ProductsRepository,
    form: ComboFormInput,
    catalogProducts: List<PosProduct>,
): Map<Int, String> {
    val ids = allComboFormProductIds(form)
    if (ids.isEmpty()) return emptyMap()
    val hints = productNamesFromCatalog(ids, catalogProducts)
    return when (val result = productsRepository.resolveProductNames(ids, hints)) {
        is AppResult.Success -> result.data
        else -> hints
    }
}

suspend fun loadComboComponentMetadata(
    productsRepository: ProductsRepository,
    modifiersRepository: ModifiersRepository,
    productIds: List<Int>,
    existingNames: Map<Int, String> = emptyMap(),
): ComboComponentMetadata {
    if (productIds.isEmpty()) {
        return ComboComponentMetadata(productNames = existingNames)
    }
    val allGroups = when (val groupsResult = modifiersRepository.listModifierGroups()) {
        is AppResult.Success -> groupsResult.data
        else -> emptyList()
    }
    val details = when (val detailsResult = productsRepository.getProductDetails(productIds)) {
        is AppResult.Success -> detailsResult.data
        else -> emptyMap()
    }
    val groupsMap = mutableMapOf<Int, List<ModifierGroup>>()
    val namesMap = existingNames.toMutableMap()
    val presentationsMap = mutableMapOf<Int, List<ProductPresentation>>()
    for (productId in productIds) {
        val detail = details[productId] ?: continue
        val productForm = detail.toFormInput()
        namesMap.putIfAbsent(productId, productForm.name)
        val presentations = detail.presentations.filter { it.active && it.name.isNotBlank() }
        if (presentations.isNotEmpty()) {
            presentationsMap[productId] = presentations
        }
        val stub = PosProduct(
            id = productId,
            code = detail.product.code,
            name = productForm.name,
            salePrice = detail.product.salePrice,
            categoryId = detail.product.categoryId,
            imageUrl = detail.product.imageUrl,
            igvAffectationType = detail.product.igvAffectationType,
            priceIncludesIgv = detail.product.priceIncludesIgv,
            hasModifiers = productForm.hasModifiers,
            hasVariants = productForm.hasVariants,
        )
        if (stub.hasModifiers) {
            groupsMap[productId] = getProductExtraGroups(
                productForm.modifierGroupIds,
                allGroups,
                stub,
            )
        }
    }
    return ComboComponentMetadata(
        modifierGroups = groupsMap,
        productNames = namesMap,
        presentations = presentationsMap,
    )
}
