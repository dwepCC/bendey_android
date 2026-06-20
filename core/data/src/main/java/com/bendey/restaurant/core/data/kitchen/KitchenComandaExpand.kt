package com.bendey.restaurant.core.data.kitchen

import com.bendey.restaurant.core.domain.products.PreparationArea
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.normalizePreparationAreaKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class ComboSnapshotComponent(
    val productName: String,
    val presentationName: String?,
    val quantity: Double,
    val preparationArea: String?,
    val modifiersJson: String?,
)

private data class RoutingLine(
    val productName: String,
    val quantity: Double,
    val notes: String?,
    val modifierLines: List<String>,
    val preparationArea: String?,
    val isComboHeader: Boolean = false,
    val isComboComponent: Boolean = false,
)

fun expandKitchenItemsForKds(items: List<KitchenItem>): List<KitchenItem> =
    items.flatMap { expandSingleKitchenItem(it) }

private fun expandSingleKitchenItem(item: KitchenItem): List<KitchenItem> {
    val lines = comandaToRoutingLines(item)
    val componentLines = lines.filter { !it.isComboHeader }
    if (componentLines.isEmpty()) {
        return listOf(item.withDisplay(item.productName, item.quantity, item.preparationArea, emptyList(), false, null))
    }
    val hasComboComponents = lines.any { it.isComboComponent }
    if (!hasComboComponents) {
        val line = componentLines.first()
        return listOf(
            item.withDisplay(
                line.productName,
                line.quantity,
                line.preparationArea ?: item.preparationArea,
                line.modifierLines,
                false,
                null,
            ),
        )
    }
    return componentLines.mapIndexed { index, line ->
        item.withDisplay(
            displayName = line.productName.removePrefix("  · ").trim(),
            displayQuantity = line.quantity,
            preparationArea = line.preparationArea ?: item.preparationArea,
            modifierLines = line.modifierLines,
            isComboComponent = true,
            comboName = item.productName,
            displayKey = "${item.id}:${normalizePreparationAreaKey(line.preparationArea)}:$index",
        )
    }
}

private fun comandaToRoutingLines(item: KitchenItem): List<RoutingLine> {
    val comboJson = item.comboSnapshotJson
    if (!comboJson.isNullOrBlank()) {
        try {
            val obj = Json.parseToJsonElement(comboJson).jsonObject
            val header = (obj["combo_name"]?.jsonPrimitive?.contentOrNull ?: item.productName).uppercase()
            val components = parseComboComponents(obj)
            if (components.isNotEmpty()) {
                val lines = mutableListOf<RoutingLine>()
                val headerByArea = mutableSetOf<String>()
                for (comp in components) {
                    val label = comp.presentationName?.let { "${comp.productName} ($it)" } ?: comp.productName
                    if (label.isBlank()) continue
                    val area = componentPreparationArea(comp, item.preparationArea)
                    val areaKey = normalizePreparationAreaKey(area)
                    if (headerByArea.add(areaKey)) {
                        lines += RoutingLine(
                            productName = header,
                            quantity = item.quantity,
                            notes = item.notes,
                            modifierLines = listOf("—"),
                            preparationArea = area,
                            isComboHeader = true,
                        )
                    }
                    lines += RoutingLine(
                        productName = "  · ${label.trim()}",
                        quantity = comp.quantity * item.quantity,
                        notes = null,
                        modifierLines = parseModifierLines(comp.modifiersJson),
                        preparationArea = area,
                        isComboComponent = true,
                    )
                }
                return lines
            }
        } catch (_: Exception) {
            // fallback below
        }
    }
    return listOf(
        RoutingLine(
            productName = item.productName,
            quantity = item.quantity,
            notes = item.notes,
            modifierLines = parseModifierLines(item.modifiersJson),
            preparationArea = item.preparationArea,
        ),
    )
}

private fun parseComboComponents(obj: JsonObject): List<ComboSnapshotComponent> {
    val arr = obj["components"]?.jsonArray ?: return emptyList()
    return arr.mapNotNull { element ->
        if (element !is JsonObject) return@mapNotNull null
        ComboSnapshotComponent(
            productName = element["product_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            presentationName = element["presentation_name"]?.jsonPrimitive?.contentOrNull,
            quantity = element["quantity"]?.jsonPrimitive?.doubleOrNull
                ?: element["quantity"]?.jsonPrimitive?.intOrNull?.toDouble()
                ?: 1.0,
            preparationArea = element["preparation_area"]?.jsonPrimitive?.contentOrNull,
            modifiersJson = element["modifiers_json"]?.jsonPrimitive?.contentOrNull,
        )
    }
}

private fun componentPreparationArea(comp: ComboSnapshotComponent, comandaArea: String?): String {
    val fromComp = comp.preparationArea?.trim().orEmpty()
    if (fromComp.isNotBlank()) return fromComp
    return comandaArea?.trim().orEmpty().ifBlank { PreparationArea.COCINA.apiValue }
}

private fun parseModifierLines(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        when (val element = Json.parseToJsonElement(json)) {
            is JsonArray -> element.flatMap { parseModifierElement(it) }
            is JsonObject -> parseModifierElement(element)
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseModifierElement(element: JsonElement): List<String> {
    if (element !is JsonObject) return emptyList()
    val type = element["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val name = element["name"]?.jsonPrimitive?.contentOrNull
        ?: element["label"]?.jsonPrimitive?.contentOrNull
        ?: return emptyList()
    val price = element["price"]?.jsonPrimitive?.doubleOrNull
        ?: element["price_adjustment"]?.jsonPrimitive?.doubleOrNull
    val prefix = when (type) {
        "variant", "presentation" -> ""
        else -> "+ "
    }
    val priceSuffix = price?.takeIf { it != 0.0 }?.let { " (+S/ ${"%.2f".format(it)})" }.orEmpty()
    return listOf("$prefix$name$priceSuffix")
}

private fun KitchenItem.withDisplay(
    displayName: String,
    displayQuantity: Double,
    preparationArea: String?,
    modifierLines: List<String>,
    isComboComponent: Boolean,
    comboName: String?,
    displayKey: String = this.displayKey,
): KitchenItem = copy(
    displayKey = displayKey,
    displayName = displayName,
    displayQuantity = displayQuantity,
    preparationArea = preparationArea?.let(::normalizePreparationAreaKey),
    modifierLines = modifierLines,
    isComboComponent = isComboComponent,
    comboName = comboName,
)
