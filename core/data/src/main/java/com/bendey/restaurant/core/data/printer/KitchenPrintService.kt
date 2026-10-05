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
import com.bendey.restaurant.core.data.printer.printserver.toPrintOutcome
import com.bendey.restaurant.core.domain.print.PrintOutcome
import com.bendey.restaurant.core.domain.print.PrintStatus
import com.bendey.restaurant.core.domain.print.combinePrintOutcomes
import com.bendey.restaurant.core.domain.print.comandaSentNotPrinted
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

/** Qué pasó al imprimir la comanda de una ronda recién enviada. Distingue "falló" de "no hay con qué". */
sealed interface ComandaPrintOutcome {
    /** Se imprimió completa. */
    data object Printed : ComandaPrintOutcome
    /** Hay impresora/servidor configurado y la impresión falló (total o parcialmente). */
    data class Failed(val reason: String?) : ComandaPrintOutcome
    /** No hay impresora de comandas (ni servidor) configurado. */
    data object NotConfigured : ComandaPrintOutcome
    /** Hay servidor de impresión elegido pero no se lo encontró en la red. */
    data object ServerUnreachable : ComandaPrintOutcome
    /** Se encontró el servidor de impresión pero no atendió el trabajo. */
    data object ServerDown : ComandaPrintOutcome
    /** El usuario desactivó la impresión automática: no es un fallo. */
    data object AutoPrintOff : ComandaPrintOutcome
    /** La ronda no tiene comandas que imprimir. */
    data object NothingToPrint : ComandaPrintOutcome
}

/** Estado de impresión unificado -> resultado de una comanda recién enviada. */
fun PrintOutcome.toComandaOutcome(): ComandaPrintOutcome = when (status) {
    PrintStatus.OK -> ComandaPrintOutcome.Printed
    PrintStatus.NOT_CONFIGURED -> ComandaPrintOutcome.NotConfigured
    PrintStatus.SERVER_UNREACHABLE -> ComandaPrintOutcome.ServerUnreachable
    PrintStatus.SERVER_DOWN -> ComandaPrintOutcome.ServerDown
    PrintStatus.FAILED -> ComandaPrintOutcome.Failed(reason)
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
): ComandaPrintFeedback {
    // "Comanda #n enviada, pero no se imprimió." + el motivo (mismos textos print.* que Tauri).
    fun notPrinted(print: PrintOutcome, canReprint: Boolean) = ComandaPrintFeedback(
        snack = null,
        alert = comandaSentNotPrinted(orderNumber) + " " + print.message,
        canReprint = canReprint,
        markPrinted = false,
    )
    return when (outcome) {
        ComandaPrintOutcome.Printed -> ComandaPrintFeedback(comandaSentMessage(orderNumber, itemCount), null, false, true)
        ComandaPrintOutcome.AutoPrintOff -> ComandaPrintFeedback(
            if (itemCount > 0) comandaSentMessage(orderNumber, itemCount) else "Comanda #$orderNumber enviada a cocina",
            null, false, false,
        )
        ComandaPrintOutcome.NothingToPrint -> ComandaPrintFeedback("Pedido #$orderNumber enviado", null, false, false)
        is ComandaPrintOutcome.Failed -> notPrinted(PrintOutcome.failed(outcome.reason), canReprint = true)
        ComandaPrintOutcome.NotConfigured -> notPrinted(PrintOutcome.NotConfigured, canReprint = false)
        ComandaPrintOutcome.ServerUnreachable -> notPrinted(PrintOutcome.ServerUnreachable, canReprint = true)
        ComandaPrintOutcome.ServerDown -> notPrinted(PrintOutcome.ServerDown, canReprint = true)
    }
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
    /**
     * Imprime la comanda de una ronda recién enviada, sin colapsar los motivos: la UI necesita saber si
     * salió, falló (y por qué), o nunca hubo impresora, para no marcar como impresa una ronda que no lo está.
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
        return printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas).toComandaOutcome()
    }

    /**
     * Reimpresión manual: ignora auto-print. null = no hay comandas que imprimir; si no, el resultado
     * unificado (sin impresora configurada = NOT_CONFIGURED, no un silencio).
     */
    suspend fun reprintComandaRound(
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): PrintOutcome? {
        if (comandas.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (!settings.isComandaPrintReady()) return PrintOutcome.NotConfigured
        return printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)
    }

    suspend fun reprintAllComandaRounds(
        tableName: String?,
        waiterName: String?,
        orders: List<Pair<Int, List<ComandaLine>>>,
    ): PrintOutcome? {
        if (orders.isEmpty()) return null
        val settings = printerPreferencesStore.settings.first()
        if (!settings.isComandaPrintReady()) return PrintOutcome.NotConfigured
        // Solo cuenta como reimpresas si TODAS las rondas salieron.
        return combinePrintOutcomes(
            orders.map { (orderNumber, comandas) ->
                printComandaRoundInternal(settings, tableName, orderNumber, waiterName, comandas)
            },
        )
    }

    private suspend fun printComandaRoundInternal(
        settings: PrinterSettings,
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): PrintOutcome {
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            val server = printServerConnectionManager.resolveServer(settings) ?: return PrintOutcome.NotConfigured
            return printServerClient.printComandaRound(
                server = server,
                tableName = tableName,
                orderNumber = orderNumber,
                waiterName = waiterName,
                comandas = comandas,
            ).toPrintOutcome()
        }

        val baseName = tableName ?: "Mostrador"
        val groups = groupLinesByPreparationArea(comandasToRoutingLines(comandas))
        val outcomes = mutableListOf<PrintOutcome>()
        for ((areaKey, areaLines) in groups) {
            // Ajuste local: cómo se presentan los combos en el ticket de esta área.
            val printableLines = comboLinesForPrint(areaLines, settings.comandaComboDisplay)
            if (printableLines.isEmpty()) continue
            val prepArea = if (areaKey == PRINT_DEFAULT_AREA_KEY) null else areaKey
            val target = settings.targetForComandaArea(prepArea)
            if (target == null) {
                // Un área sin impresora a la que mandar: esa parte de la comanda NO salió.
                outcomes += PrintOutcome.NotConfigured
                continue
            }
            val ticketLabel = areaTicketLabel(baseName, areaKey)
            outcomes += printerRepository.printComanda(
                target,
                ComandaPrintInput(
                    tableName = ticketLabel,
                    orderNumber = orderNumber,
                    waiterName = waiterName,
                    items = printableLines.map { it.toPrintItem() },
                    paperWidth = target.paperWidth,
                    textSize = settings.comandaTextSize,
                ),
            ).toPrintOutcome()
        }
        return combinePrintOutcomes(outcomes)
    }

    /**
     * Imprime la precuenta. La lista vacía no es "sin impresora": es un FAILED con motivo propio.
     * El motivo del servidor ("Impresora de precuenta no configurada", etc.) se traduce a los
     * estados unificados (incidente El Braserito, 2026-09-07: el mozo debe saber qué pasó).
     */
    suspend fun printPrecuenta(precuenta: PrecuentaData): PrintOutcome {
        if (precuenta.lines.isEmpty()) return PrintOutcome.failed("la cuenta todavía no tiene productos")
        val settings = printerPreferencesStore.settings.first()
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            val server = printServerConnectionManager.resolveServer(settings) ?: return PrintOutcome.NotConfigured
            return printServerClient.printPrecuenta(server, precuenta).toPrintOutcome()
        }
        val target = settings.targetFor(PrinterSlot.PRECUENTA)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return PrintOutcome.NotConfigured
        return printerRepository.printPrecuenta(
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
        ).toPrintOutcome()
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
