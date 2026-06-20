package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import com.bendey.restaurant.platform.printing.escpos.ComandaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaPrintInput
import com.bendey.restaurant.platform.printing.escpos.ComandaPrintInput
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import com.bendey.restaurant.platform.printing.transport.PrinterTarget
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KitchenPrintService @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val printerPreferencesStore: PrinterPreferencesStore,
) {
    /** null = sin impresora / auto-print off; true = OK; false = error de impresión. */
    suspend fun printComandaRound(
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): Boolean? {
        if (comandas.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (!settings.autoPrintComandas) return null
        val target = settings.targetFor(PrinterSlot.COMANDAS) ?: return null
        return when (
            printWithTarget(target, tableName, orderNumber, waiterName, comandas)
        ) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    /** null = sin impresora; true = OK; false = error. */
    suspend fun printPrecuenta(precuenta: PrecuentaData): Boolean? {
        if (precuenta.lines.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        val target = settings.targetFor(PrinterSlot.PRECUENTA)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return null
        return when (
            printerRepository.printPrecuenta(
                target,
                PrecuentaPrintInput(
                    tableName = precuenta.tableName,
                    items = precuenta.lines.map {
                        PrecuentaItem(
                            productName = it.productName,
                            quantity = it.quantity,
                            unitPrice = it.unitPrice,
                        )
                    },
                    total = precuenta.total,
                ),
            )
        ) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    suspend fun printWithTarget(
        target: PrinterTarget,
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): PrintResult = printerRepository.printComanda(
        target,
        ComandaPrintInput(
            tableName = tableName ?: "Mostrador",
            orderNumber = orderNumber,
            waiterName = waiterName,
            items = comandas.map { it.toPrintItem() },
        ),
    )

    private fun ComandaLine.toPrintItem() = ComandaItem(
        productName = productName,
        quantity = quantity,
        notes = notes?.takeIf { it.isNotBlank() },
        modifierLines = parseModifierLines(modifiersJson),
    )

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

    private fun parseModifierElement(element: JsonElement): List<String> = when (element) {
        is JsonObject -> {
            val name = element["name"]?.jsonPrimitive?.contentOrNull
                ?: element["option_name"]?.jsonPrimitive?.contentOrNull
            if (!name.isNullOrBlank()) listOf(name) else emptyList()
        }
        is JsonArray -> element.flatMap { parseModifierElement(it) }
        else -> emptyList()
    }
}
