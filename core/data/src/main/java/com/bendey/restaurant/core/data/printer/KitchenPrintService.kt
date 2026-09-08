package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.data.kitchen.KitchenRoutingLine
import com.bendey.restaurant.core.data.kitchen.PRINT_DEFAULT_AREA_KEY
import com.bendey.restaurant.core.data.kitchen.areaTicketLabel
import com.bendey.restaurant.core.data.kitchen.comandasToRoutingLines
import com.bendey.restaurant.core.data.kitchen.consolidateComboLinesForPrint
import com.bendey.restaurant.core.data.kitchen.flattenComboLinesToProducts
import com.bendey.restaurant.core.data.kitchen.groupLinesByPreparationArea
import com.bendey.restaurant.core.data.kitchen.toPrintItem
import com.bendey.restaurant.core.data.printer.printserver.PrintDeliveryMode
import com.bendey.restaurant.core.data.printer.printserver.PrintServerClient
import com.bendey.restaurant.core.data.printer.printserver.PrintServerConnectionManager
import com.bendey.restaurant.core.data.printer.printserver.RemotePrintResult
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import com.bendey.restaurant.platform.printing.escpos.ComandaComboDisplay
import com.bendey.restaurant.platform.printing.escpos.ComandaPrintInput
import com.bendey.restaurant.platform.printing.escpos.ComandaTextSize
import com.bendey.restaurant.platform.printing.escpos.PrecuentaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaPrintInput
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import com.bendey.restaurant.platform.printing.transport.PrinterTarget
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de imprimir precuenta. A diferencia de comandas (Boolean?/tri-estado simple), acá
 *  interesa propagar el motivo real del error hasta la UI — antes se perdía en un `false` plano
 *  y el mozo veía siempre el mismo mensaje genérico sin importar la causa (impresora sin
 *  configurar, servidor caído, etc). Ver incidente El Braserito, 2026-09-07. */
sealed class PrecuentaPrintOutcome {
    data object Success : PrecuentaPrintOutcome()
    /** Nada que hacer: precuenta sin líneas, o sin impresora/servidor configurado — mismos casos
     *  que antes colapsaban en `null`. */
    data object Skipped : PrecuentaPrintOutcome()
    data class Failed(val message: String) : PrecuentaPrintOutcome()
}

@Singleton
class KitchenPrintService @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val printerPreferencesStore: PrinterPreferencesStore,
    private val printServerClient: PrintServerClient,
    private val printServerConnectionManager: PrintServerConnectionManager,
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
        if (!settings.isComandaPrintReady()) return null
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
        if (!settings.isComandaPrintReady()) return null
        return printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)
    }

    suspend fun reprintAllComandaRounds(
        tableName: String?,
        waiterName: String?,
        orders: List<Pair<Int, List<ComandaLine>>>,
    ): Boolean? {
        if (orders.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (!settings.isComandaPrintReady()) return null
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
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            val server = printServerConnectionManager.resolveServer(settings) ?: return false
            return when (
                printServerClient.printComandaRound(
                    server = server,
                    tableName = tableName,
                    orderNumber = orderNumber,
                    waiterName = waiterName,
                    comandas = comandas,
                )
            ) {
                RemotePrintResult.Success -> true
                is RemotePrintResult.Error -> false
            }
        }

        val baseName = tableName ?: "Mostrador"
        val groups = groupLinesByPreparationArea(comandasToRoutingLines(comandas))
        var printed = 0
        var hadError = false
        for ((areaKey, areaLines) in groups) {
            // Ajuste local: cómo se presentan los combos en el ticket de esta área.
            val printableLines = comboLinesForPrint(areaLines, settings.comandaComboDisplay)
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
                        paperWidth = target.paperWidth,
                        textSize = settings.comandaTextSize,
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

    suspend fun printPrecuenta(precuenta: PrecuentaData): PrecuentaPrintOutcome {
        if (precuenta.lines.isEmpty()) return PrecuentaPrintOutcome.Skipped
        val settings = printerPreferencesStore.settings.first()
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            val server = printServerConnectionManager.resolveServer(settings)
                ?: return PrecuentaPrintOutcome.Skipped
            return when (val result = printServerClient.printPrecuenta(server, precuenta)) {
                RemotePrintResult.Success -> PrecuentaPrintOutcome.Success
                // El servidor (PC con Tauri) ya manda el motivo real ("Impresora de precuenta no
                // configurada", etc.) — antes se descartaba acá y el mozo siempre veía el mismo
                // genérico sin importar la causa. Ver incidente El Braserito, 2026-09-07.
                is RemotePrintResult.Error -> PrecuentaPrintOutcome.Failed(result.message)
            }
        }
        val target = settings.targetFor(PrinterSlot.PRECUENTA)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return PrecuentaPrintOutcome.Skipped
        return when (
            val result = printerRepository.printPrecuenta(
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
            is PrintResult.Success -> PrecuentaPrintOutcome.Success
            is PrintResult.Error -> PrecuentaPrintOutcome.Failed(result.message)
        }
    }

    suspend fun printWithTarget(
        target: PrinterTarget,
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): PrintResult {
        val settings = printerPreferencesStore.settings.first()
        val allLines = comandasToRoutingLines(comandas)
        val printableLines = comboLinesForPrint(allLines, settings.comandaComboDisplay)
        return printerRepository.printComanda(
            target,
            ComandaPrintInput(
                tableName = tableName ?: "Mostrador",
                orderNumber = orderNumber,
                waiterName = waiterName,
                items = printableLines.map { it.toPrintItem() },
                paperWidth = target.paperWidth,
                textSize = settings.comandaTextSize,
            ),
        )
    }
}

/**
 * Aplica el modo de combos elegido a las lineas de UN ticket (una area, o todas si no hay ruteo).
 *
 * Vive fuera de la clase porque los dos caminos de impresion —el ruteo por area y la impresion
 * dirigida a una impresora concreta— tienen que resolverlo IGUAL. Antes cada uno repetia el mismo
 * `if`, y con tres modos esa duplicacion es justo donde uno de los dos se queda atras.
 */
private fun comboLinesForPrint(
    lines: List<KitchenRoutingLine>,
    display: ComandaComboDisplay,
): List<KitchenRoutingLine> = when (display) {
    ComandaComboDisplay.GROUPED -> consolidateComboLinesForPrint(lines)
    ComandaComboDisplay.PRODUCTS -> flattenComboLinesToProducts(lines)
    // La cabecera del combo no se imprime en este modo: cocina ya ve los componentes, y el nombre
    // del combo solo agrega una linea que no se prepara.
    ComandaComboDisplay.DETAILED -> lines.filter { !it.isComboHeader }
}
