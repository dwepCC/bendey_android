package com.bendey.restaurant.core.data.kitchen

import com.bendey.restaurant.core.domain.catalog.preparationAreaDisplayLabel
import com.bendey.restaurant.core.domain.pos.formatKitchenComponentDisplay
import com.bendey.restaurant.core.domain.pos.groupSelections
import com.bendey.restaurant.core.domain.pos.snapshotComponentsToSummaryItems
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
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

const val PRINT_DEFAULT_AREA_KEY = "__default__"

data class KitchenRoutingLine(
    val productName: String,
    val quantity: Double,
    val notes: String?,
    val modifierLines: List<String>,
    val preparationArea: String?,
    val isComboHeader: Boolean = false,
    val isComboComponent: Boolean = false,
)

fun normalizePreparationAreaKeyForPrint(area: String?): String? =
    area?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

fun comandasToRoutingLines(comandas: List<ComandaLine>): List<KitchenRoutingLine> =
    comandas.flatMap { comandaToRoutingLines(it) }

fun groupLinesByPreparationArea(lines: List<KitchenRoutingLine>): Map<String, List<KitchenRoutingLine>> =
    lines.groupBy { line ->
        normalizePreparationAreaKeyForPrint(line.preparationArea) ?: PRINT_DEFAULT_AREA_KEY
    }

fun areaTicketLabel(baseTableName: String, areaKey: String): String {
    if (areaKey == PRINT_DEFAULT_AREA_KEY) return baseTableName
    val label = preparationAreaDisplayLabel(areaKey)
    return if (label != "Sin área") {
        "$baseTableName - ${label.uppercase()}"
    } else {
        "$baseTableName - ${areaKey.uppercase()}"
    }
}

private fun comandaToRoutingLines(comanda: ComandaLine): List<KitchenRoutingLine> {
    val comboJson = comanda.comboSnapshotJson
    if (!comboJson.isNullOrBlank()) {
        try {
            val obj = Json.parseToJsonElement(comboJson).jsonObject
            val header = (obj["combo_name"]?.jsonPrimitive?.contentOrNull ?: comanda.productName).uppercase()
            val components = parseComboComponents(obj)
            if (components.isNotEmpty()) {
                val lines = mutableListOf<KitchenRoutingLine>()
                val headerByArea = mutableSetOf<String>()
                val lineQty = if (comanda.quantity > 0) comanda.quantity else 1.0
                val grouped = groupSelections(
                    snapshotComponentsToSummaryItems(
                        components.map {
                            com.bendey.restaurant.core.domain.pos.ComboSnapshotComponentInput(
                                productName = it.productName,
                                presentationName = it.presentationName,
                                quantity = it.quantity,
                                modifiersJson = it.modifiersJson,
                            )
                        },
                    ),
                )
                for (group in grouped) {
                    val matching = components.firstOrNull { comp ->
                        val label = comp.presentationName?.let { "${comp.productName} ($it)" } ?: comp.productName
                        label.trim() == group.optionName
                    } ?: components.first()
                    val area = componentPreparationArea(matching, comanda.preparationArea)
                    val areaKey = normalizePreparationAreaKeyForPrint(area) ?: PRINT_DEFAULT_AREA_KEY
                    if (headerByArea.add(areaKey)) {
                        lines += KitchenRoutingLine(
                            productName = header,
                            quantity = lineQty,
                            notes = comanda.notes,
                            modifierLines = emptyList(),
                            preparationArea = area,
                            isComboHeader = true,
                        )
                    }
                    val display = formatKitchenComponentDisplay(group.optionName, group.quantity, lineQty)
                    lines += KitchenRoutingLine(
                        productName = "  - ${display.displayName}",
                        quantity = display.displayQuantity,
                        notes = null,
                        modifierLines = components
                            .filter { comp ->
                                val label = comp.presentationName?.let { "${comp.productName} ($it)" } ?: comp.productName
                                label.trim() == group.optionName
                            }
                            .flatMap { parseModifierLines(it.modifiersJson) }
                            .distinct(),
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
        KitchenRoutingLine(
            productName = comanda.productName,
            quantity = comanda.quantity,
            notes = comanda.notes,
            modifierLines = parseModifierLines(comanda.modifiersJson),
            preparationArea = comanda.preparationArea,
        ),
    )
}

private data class PrintComboComponent(
    val productName: String,
    val presentationName: String?,
    val quantity: Double,
    val preparationArea: String?,
    val modifiersJson: String?,
)

private fun parseComboComponents(obj: JsonObject): List<PrintComboComponent> {
    val arr = obj["components"]?.jsonArray ?: return emptyList()
    return arr.mapNotNull { element ->
        if (element !is JsonObject) return@mapNotNull null
        PrintComboComponent(
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

private fun componentPreparationArea(comp: PrintComboComponent, comandaArea: String?): String {
    val fromComp = comp.preparationArea?.trim().orEmpty()
    if (fromComp.isNotBlank()) return fromComp.lowercase()
    return comandaArea?.trim()?.lowercase().orEmpty()
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
        ?: element["option_name"]?.jsonPrimitive?.contentOrNull
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

fun KitchenRoutingLine.toPrintItem() = com.bendey.restaurant.platform.printing.escpos.ComandaItem(
    productName = productName,
    quantity = quantity,
    notes = notes?.takeIf { it.isNotBlank() },
    modifierLines = modifierLines,
)
