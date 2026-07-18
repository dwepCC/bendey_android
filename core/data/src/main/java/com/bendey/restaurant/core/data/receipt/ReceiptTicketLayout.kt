package com.bendey.restaurant.core.data.receipt

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.platform.printing.escpos.EscPosTextUtils
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.escpos.SunatPrintUtils
import com.bendey.restaurant.platform.printing.escpos.columnsForPaper
import java.text.NumberFormat

internal enum class ReceiptTextAlign { LEFT, CENTER, RIGHT }

internal data class ReceiptTextLine(
    val text: String,
    val align: ReceiptTextAlign = ReceiptTextAlign.LEFT,
    val bold: Boolean = false,
)

/** Líneas de ticket alineadas con [com.bendey.restaurant.platform.printing.escpos.DocumentLayoutBuilder]. */
internal object ReceiptTicketLayout {
    private val cols = columnsForPaper(PaperWidthMm.W80)

    fun build(data: SalePrintData, money: NumberFormat): List<ReceiptTextLine> {
        val lines = mutableListOf<ReceiptTextLine>()
        fun center(text: String, bold: Boolean = false) {
            lines += ReceiptTextLine(text, ReceiptTextAlign.CENTER, bold)
        }
        fun left(text: String, bold: Boolean = false) {
            lines += ReceiptTextLine(text, ReceiptTextAlign.LEFT, bold)
        }
        fun right(text: String, bold: Boolean = false) {
            lines += ReceiptTextLine(text, ReceiptTextAlign.RIGHT, bold)
        }
        fun divider() {
            left("-".repeat(cols.coerceAtMost(48)))
        }
        fun wrapCenter(text: String, bold: Boolean = false) {
            EscPosTextUtils.wrapText(text, cols).forEach { center(it, bold) }
        }
        fun wrapLeft(text: String, bold: Boolean = false) {
            EscPosTextUtils.wrapText(text, cols).forEach { left(it, bold) }
        }

        wrapCenter(data.companyName.ifBlank { "EMPRESA" }, bold = true)
        data.companyLegalName?.takeIf { it.isNotBlank() }?.let { wrapCenter(it) }
        if (data.companyRuc.isNotBlank()) center("RUC: ${data.companyRuc}")
        data.companyAddress?.takeIf { it.isNotBlank() }?.let { wrapCenter(it) }
        data.branchName?.takeIf { it.isNotBlank() }?.let { center(it) }
        data.companyPhone?.takeIf { it.isNotBlank() }?.let { center("Telf: $it") }
        data.companyEmail?.takeIf { it.isNotBlank() }?.let { center("Email: $it") }
        data.companyWebsite?.takeIf { it.isNotBlank() }?.let { center("Web: $it") }

        divider()
        wrapCenter(SunatPrintUtils.tipoComprobanteLabel(data.sunatCode, data.docType), bold = true)
        center(data.number, bold = true)
        divider()

        left("Fecha Emision: ${data.issueDate}")
        data.issueTime?.takeIf { it.isNotBlank() }?.let { left("Hora Emision: $it") }
        data.clientName?.takeIf { it.isNotBlank() }?.let { wrapLeft("Cliente: $it") }
        data.clientDocNumber?.takeIf { it.isNotBlank() }?.let { left("Doc: $it") }

        divider()
        left(itemHeader())
        divider()

        data.items.forEach { item ->
            appendItemLines(item.quantity, item.description, item.total, money).forEach { left(it) }
        }

        divider()
        if (data.taxAmount > 0) right("IGV: ${money.format(data.taxAmount)}")
        right("TOTAL A PAGAR: ${money.format(data.total)}", bold = true)

        data.legendText?.takeIf { it.isNotBlank() }?.let { legend ->
            divider()
            wrapLeft("Son: $legend")
        }

        if (data.payments.isNotEmpty()) {
            divider()
            left("Pagos:")
            data.payments.forEach { payment ->
                left("${payment.method}: ${money.format(payment.amount)}")
            }
        }

        val showQr = SunatPrintUtils.isElectronicSunatCode(data.sunatCode) &&
            !data.qrData.isNullOrBlank()
        if (showQr) {
            lines += ReceiptTextLine("__QR__")
            data.sunatHash?.takeIf { it.isNotBlank() }?.let { hash ->
                EscPosTextUtils.wrapText("Hash: $hash", cols).forEach { center(it) }
            }
            center("Representacion impresa CPE")
            center("Consulte en sunat.gob.pe")
        }

        left("")
        center("Gracias por su preferencia")
        if (data.showsBendeyBranding) center("Hecho con Bendey")
        left("")
        return lines
    }

    private fun itemHeader(): String {
        val right = "TOTAL"
        val left = "CANT  DESCRIPCION".padEnd(cols - right.length)
        return left + right
    }

    private fun appendItemLines(
        quantity: Double,
        description: String,
        total: Double,
        money: NumberFormat,
    ): List<String> {
        val qty = formatQty(quantity)
        val totalStr = money.format(total)
        val leftCols = cols - totalStr.length - 1
        val descWrapped = EscPosTextUtils.wrapText(
            description.trim(),
            maxOf(8, leftCols - (qty.length + 2)),
        )
        val result = mutableListOf<String>()
        val firstLeft = "${qty}x ${descWrapped.firstOrNull().orEmpty()}".padEnd(leftCols)
        result += "$firstLeft $totalStr"
        descWrapped.drop(1).forEach { w -> result += "   $w" }
        return result
    }

    private fun formatQty(qty: Double): String = qty.toString().replace(Regex("\\.0+$"), "")
}
