package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintInput
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintLine
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintPayment
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentPrintService @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val printerPreferencesStore: PrinterPreferencesStore,
) {
    /** null = sin impresora / auto-print off; true = OK; false = error. */
    suspend fun printSaleDocument(data: SalePrintData?, force: Boolean = false): Boolean? {
        if (data == null) return null
        val settings = printerPreferencesStore.settings.first()
        if (!force && !settings.autoPrintDocuments) return null
        val target = settings.targetFor(PrinterSlot.DOCUMENTOS)
            ?: settings.targetFor(PrinterSlot.COMANDAS)
            ?: return null
        return when (printerRepository.printDocument(target, data.toInput())) {
            is PrintResult.Success -> true
            is PrintResult.Error -> false
        }
    }

    suspend fun hasConfiguredPrinter(): Boolean {
        val settings = printerPreferencesStore.settings.first()
        return settings.targetFor(PrinterSlot.DOCUMENTOS) != null ||
            settings.targetFor(PrinterSlot.COMANDAS) != null
    }
}

private fun SalePrintData.toInput() = DocumentPrintInput(
    docType = docType,
    sunatCode = sunatCode,
    number = number,
    issueDate = issueDate,
    companyName = companyName,
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
)
