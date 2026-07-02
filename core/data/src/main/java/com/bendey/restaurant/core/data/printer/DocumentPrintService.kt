package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.data.printer.printserver.PrintDeliveryMode
import com.bendey.restaurant.core.data.printer.printserver.PrintServerClient
import com.bendey.restaurant.core.data.printer.printserver.PrintServerConnectionManager
import com.bendey.restaurant.core.data.printer.printserver.RemotePrintResult
import com.bendey.restaurant.core.data.receipt.ReceiptLogoLoader
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintInput
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintLine
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintPayment
import com.bendey.restaurant.platform.printing.escpos.EscPosLogoRaster
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
        val logoRaster = loadLogoRaster(data.companyLogoUrl, target.paperWidth)
        return when (printerRepository.printDocument(target, data.toInput(logoRaster))) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    suspend fun hasConfiguredPrinter(): Boolean {
        val settings = printerPreferencesStore.settings.first()
        return settings.isDocumentPrintReady()
    }

    private fun loadLogoRaster(logoUrl: String?, paperWidth: PaperWidthMm): ByteArray? {
        val maxLogoPx = when (paperWidth) {
            PaperWidthMm.W58 -> 360
            PaperWidthMm.W80 -> 520
        }
        val bitmap = logoLoader.load(logoUrl, maxLogoPx) ?: return null
        return EscPosLogoRaster.encode(bitmap, paperWidth).also {
            if (bitmap.isRecycled.not()) bitmap.recycle()
        }
    }
}

private fun SalePrintData.toInput(logoRaster: ByteArray?) = DocumentPrintInput(
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
        )
    },
    subtotal = subtotal,
    taxAmount = taxAmount,
    total = total,
    currency = currency,
    payments = payments.map {
        DocumentPrintPayment(method = it.method, amount = it.amount)
    },
    legendText = legendText,
    qrData = qrData,
    sunatHash = sunatHash,
    logoRaster = logoRaster,
)
