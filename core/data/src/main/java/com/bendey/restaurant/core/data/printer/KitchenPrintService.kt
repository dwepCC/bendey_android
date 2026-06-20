package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.data.kitchen.PRINT_DEFAULT_AREA_KEY
import com.bendey.restaurant.core.data.kitchen.areaTicketLabel
import com.bendey.restaurant.core.data.kitchen.comandasToRoutingLines
import com.bendey.restaurant.core.data.kitchen.groupLinesByPreparationArea
import com.bendey.restaurant.core.data.kitchen.toPrintItem
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import com.bendey.restaurant.platform.printing.escpos.ComandaPrintInput
import com.bendey.restaurant.platform.printing.escpos.PrecuentaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaPrintInput
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import com.bendey.restaurant.platform.printing.transport.PrinterTarget
import kotlinx.coroutines.flow.first
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
        if (settings.comandas.toTarget() == null) return null
        return printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)
    }

    /** Reimpresión manual: ignora auto-print pero requiere impresora de comandas configurada. */
    suspend fun reprintComandaRound(
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): Boolean? {
        if (comandas.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (settings.comandas.toTarget() == null) return null
        return printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)
    }

    suspend fun reprintAllComandaRounds(
        tableName: String?,
        waiterName: String?,
        orders: List<Pair<Int, List<ComandaLine>>>,
    ): Boolean? {
        if (orders.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (settings.comandas.toTarget() == null) return null
        var anySuccess = false
        var anyError = false
        for ((orderNumber, comandas) in orders) {
            when (printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)) {
                true -> anySuccess = true
                false -> anyError = true
            }
        }
        return when {
            anyError && !anySuccess -> false
            anySuccess -> true
            else -> false
        }
    }

    private suspend fun printComandaRoundInternal(
        settings: PrinterSettings,
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): Boolean {
        val baseName = tableName ?: "Mostrador"
        val groups = groupLinesByPreparationArea(comandasToRoutingLines(comandas))
        var printed = 0
        var hadError = false
        for ((areaKey, areaLines) in groups) {
            val printableLines = areaLines.filter { !it.isComboHeader }
            if (printableLines.isEmpty()) continue
            val prepArea = if (areaKey == PRINT_DEFAULT_AREA_KEY) null else areaKey
            val target = settings.targetForComandaArea(prepArea) ?: continue
            val ticketLabel = areaTicketLabel(baseName, areaKey)
            when (
                printerRepository.printComanda(
                    target,
                    ComandaPrintInput(
                        tableName = ticketLabel,
                        orderNumber = orderNumber,
                        waiterName = waiterName,
                        items = printableLines.map { it.toPrintItem() },
                    ),
                )
            ) {
                is PrintResult.Success -> printed++
                is PrintResult.Error -> hadError = true
            }
        }
        return when {
            printed > 0 && !hadError -> true
            printed > 0 -> false
            else -> false
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
    ): PrintResult {
        val printableLines = comandasToRoutingLines(comandas).filter { !it.isComboHeader }
        return printerRepository.printComanda(
            target,
            ComandaPrintInput(
                tableName = tableName ?: "Mostrador",
                orderNumber = orderNumber,
                waiterName = waiterName,
                items = printableLines.map { it.toPrintItem() },
            ),
        )
    }
}
