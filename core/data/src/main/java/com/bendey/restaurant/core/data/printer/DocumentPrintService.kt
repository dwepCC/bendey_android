package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.data.printer.printserver.PrintDeliveryMode
import com.bendey.restaurant.core.data.printer.printserver.PrintServerClient
import com.bendey.restaurant.core.data.printer.printserver.PrintServerConnectionManager
import com.bendey.restaurant.core.data.printer.printserver.RemotePrintResult
import com.bendey.restaurant.core.data.receipt.ReceiptLogoLoader
import com.bendey.restaurant.core.data.receipt.ReceiptModifierLines
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintInput
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintLine
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintPayment
import com.bendey.restaurant.platform.printing.escpos.EscPosAlign
import com.bendey.restaurant.platform.printing.escpos.EscPosBuilder
import com.bendey.restaurant.platform.printing.escpos.EscPosLogoRaster
import com.bendey.restaurant.platform.printing.escpos.EscPosTextUtils
import com.bendey.restaurant.platform.printing.escpos.LogoSize
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentPrintService @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val printerPreferencesStore: PrinterPreferencesStore,
    private val logoLoader: ReceiptLogoLoader,
    private val printServerClient: PrintServerClient,
    private val printServerConnectionManager: PrintServerConnectionManager,
) {
    /** null = sin impresora / auto-print off; true = OK; false = error. */
    suspend fun printSaleDocument(data: SalePrintData?, force: Boolean = false): Boolean? {
        if (data == null) return null
        val settings = printerPreferencesStore.settings.first()
        if (!force && !settings.autoPrintDocuments) return null
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            val server = printServerConnectionManager.resolveServer(settings) ?: return null
            return when (printServerClient.printDocument(server, data)) {
                RemotePrintResult.Success -> true
                is RemotePrintResult.Error -> false
            }
        }
        val target = settings.targetFor(PrinterSlot.DOCUMENTOS)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return null
        val logoRaster = loadLogoRaster(data.companyLogoUrl, target.paperWidth, settings.documentLogoSize)
        return when (printerRepository.printDocument(target, data.toInput(logoRaster, settings.openCashDrawerOnDocument))) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    suspend fun hasConfiguredPrinter(): Boolean {
        val settings = printerPreferencesStore.settings.first()
        return settings.isDocumentPrintReady()
    }

    /**
     * Imprime un reporte de texto libre (p. ej. el arqueo de caja) en la ticketera de documentos.
     * null = sin impresora directa (o modo servidor, que solo acepta documentos estructurados);
     * true = OK; false = error. El título va centrado en negrita; cada línea se ajusta al ancho.
     */
    suspend fun printReportTicket(title: String?, lines: List<String>, force: Boolean = false): Boolean? {
        val settings = printerPreferencesStore.settings.first()
        if (!force && !settings.autoPrintDocuments) return null
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) {
            // El servidor de impresión solo expone endpoints estructurados (documento/comanda);
            // el reporte de texto se imprime únicamente con impresora directa (BT/USB/red).
            return null
        }
        val target = settings.targetFor(PrinterSlot.DOCUMENTOS)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return null
        val cols = when (target.paperWidth) {
            PaperWidthMm.W58 -> 32
            PaperWidthMm.W80 -> 48
        }
        val builder = EscPosBuilder()
        builder.init()
        if (!title.isNullOrBlank()) {
            builder.align(EscPosAlign.CENTER)
            builder.bold(true)
            builder.size(2, 2)
            builder.line(title)
            builder.size(1, 1)
            builder.bold(false)
            builder.align(EscPosAlign.LEFT)
        }
        builder.divider(cols)
        lines.forEach { raw ->
            if (raw.isEmpty()) builder.line()
            else EscPosTextUtils.wrapText(raw, cols).forEach { builder.line(it) }
        }
        builder.line()
        builder.line()
        builder.cutPartial()
        return when (printerRepository.printRaw(builder.bytes(), target)) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    /**
     * Abre la gaveta de dinero sin imprimir nada — usa la impresora de documentos/comprobantes.
     * null = sin impresora directa configurada (el servidor de impresión solo expone endpoints
     * estructurados, no un pulso de gaveta suelto); true = OK; false = error.
     */
    suspend fun openCashDrawer(): Boolean? {
        val settings = printerPreferencesStore.settings.first()
        if (settings.deliveryMode == PrintDeliveryMode.SERVER) return null
        val target = settings.targetFor(PrinterSlot.DOCUMENTOS)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return null
        val builder = EscPosBuilder()
        builder.init()
        builder.openDrawer()
        return when (printerRepository.printRaw(builder.bytes(), target)) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    private fun loadLogoRaster(logoUrl: String?, paperWidth: PaperWidthMm, logoSize: LogoSize): ByteArray? {
        val baseMaxPx = when (paperWidth) {
            PaperWidthMm.W58 -> 360
            PaperWidthMm.W80 -> 520
        }
        // El tamaño elegido escala el ancho del logo (igual en 58 y 80 mm).
        val maxLogoPx = (baseMaxPx * logoSize.scale).toInt().coerceAtLeast(48)
        val bitmap = logoLoader.load(logoUrl, maxLogoPx) ?: return null
        return EscPosLogoRaster.encode(bitmap, paperWidth).also {
            if (bitmap.isRecycled.not()) bitmap.recycle()
        }
    }
}

private fun SalePrintData.toInput(logoRaster: ByteArray?, openCashDrawer: Boolean = false) = DocumentPrintInput(
    docType = docType,
    sunatCode = sunatCode,
    number = number,
    issueDate = issueDate,
    companyName = companyName,
    companyLegalName = companyLegalName,
    companyRuc = companyRuc,
    companyAddress = companyAddress,
    branchName = branchName,
    clientName = clientName,
    clientDocNumber = clientDocNumber,
    items = items.map {
        DocumentPrintLine(
            description = it.description,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            total = it.total,
            detailLines = ReceiptModifierLines.of(it.modifiersJson) { monto ->
                "${if (currency.uppercase() == "USD") "USD" else "S/"} ${"%.2f".format(monto)}"
            },
            discount = it.discount,
        )
    },
    subtotal = subtotal,
    taxAmount = taxAmount,
    total = total,
    currency = currency,
    payments = payments.map {
        DocumentPrintPayment(method = it.method, amount = it.amount)
    },
    amountPaid = amountPaid,
    change = change,
    legendText = legendText,
    qrData = qrData,
    sunatHash = sunatHash,
    logoRaster = logoRaster,
    openCashDrawer = openCashDrawer,
)
