package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.domain.waiter.comandaSentMessage
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

/** Qué pasó al imprimir la comanda de una ronda recién enviada. Distingue "falló" de "no hay con qué". */
enum class ComandaPrintOutcome {
    /** Se imprimió completa. */
    Printed,
    /** Hay impresora/servidor configurado y la impresión falló (total o parcialmente). */
    Failed,
    /** No hay impresora de comandas (ni servidor) configurado. */
    NotConfigured,
    /** El usuario desactivó la impresión automática: no es un fallo. */
    AutoPrintOff,
    /** La ronda no tiene comandas que imprimir. */
    NothingToPrint,
}

/**
 * Qué mostrar y qué registrar tras enviar una ronda a cocina. Pura, para poder probarla sin Android.
 *
 * @property snack aviso corto (sin acción) o null.
 * @property alert aviso accionable (el pedido ya salió a cocina, pero la comanda en papel no) o null.
 * @property canReprint si el aviso puede ofrecer "Reimprimir" (solo cuando hay impresora a la que reintentar).
 * @property markPrinted si se debe confirmar la ronda como impresa en el backend (solo si salió en papel).
 */
data class ComandaPrintFeedback(
    val snack: String?,
    val alert: String?,
    val canReprint: Boolean,
    val markPrinted: Boolean,
)

fun comandaPrintFeedback(
    outcome: ComandaPrintOutcome,
    orderNumber: Int,
    /** Suma de cantidades de la ronda; con valor > 0 el aviso dice "Comanda #3 enviada · 4 ítems" (igual que Tauri). */
    itemCount: Int = 0,
): ComandaPrintFeedback = when (outcome) {
    ComandaPrintOutcome.Printed -> ComandaPrintFeedback(comandaSentMessage(orderNumber, itemCount), null, false, true)
    ComandaPrintOutcome.AutoPrintOff -> ComandaPrintFeedback(
        if (itemCount > 0) comandaSentMessage(orderNumber, itemCount) else "Comanda #$orderNumber enviada a cocina",
        null, false, false,
    )
    ComandaPrintOutcome.NothingToPrint -> ComandaPrintFeedback("Pedido #$orderNumber enviado", null, false, false)
    ComandaPrintOutcome.Failed -> ComandaPrintFeedback(
        snack = null,
        alert = "El pedido se envió a cocina, pero la comanda no se imprimió. Revisa la impresora.",
        canReprint = true,
        markPrinted = false,
    )
    ComandaPrintOutcome.NotConfigured -> ComandaPrintFeedback(
        snack = null,
        alert = "El pedido se envió a cocina, pero no hay una impresora de comandas configurada. Configúrala en Ajustes.",
        canReprint = false,
        markPrinted = false,
    )
}

/** Aviso pendiente de una comanda que no salió en papel, con lo necesario para reimprimirla desde el propio aviso. */
data class ComandaPrintAlert(
    val message: String,
    val canReprint: Boolean,
    val tableName: String?,
    val orderNumber: Int,
    val waiterName: String?,
    val comandas: List<ComandaLine>,
    /** Id de la ronda en el backend, para confirmarla como impresa al reimprimir (solo POS lo usa). */
    val orderId: Int? = null,
)

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

    /**
     * Igual que [printComandaRound] pero sin colapsar los motivos en un `Boolean?`: la UI necesita saber si
     * la comanda salió, falló o nunca hubo impresora para no marcar como impresa una ronda que no lo está.
     */
    suspend fun printComandaRoundOutcome(
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): ComandaPrintOutcome {
        if (comandas.isEmpty()) return ComandaPrintOutcome.NothingToPrint
        val settings = printerPreferencesStore.settings.first()
        if (!settings.autoPrintComandas) return ComandaPrintOutcome.AutoPrintOff
        if (!settings.isComandaPrintReady()) return ComandaPrintOutcome.NotConfigured
        return if (printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)) {
            ComandaPrintOutcome.Printed
        } else {
            ComandaPrintOutcome.Failed
        }
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
