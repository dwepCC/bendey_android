package com.bendey.restaurant.platform.printing.escpos

data class DocumentPrintLine(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
)

data class DocumentPrintPayment(
    val method: String,
    val amount: Double,
)

data class DocumentPrintInput(
    val docType: String,
    val sunatCode: String = "",
    val number: String,
    val issueDate: String,
    val companyName: String,
    val companyRuc: String,
    val companyAddress: String?,
    val branchName: String?,
    val clientName: String?,
    val clientDocNumber: String?,
    val items: List<DocumentPrintLine>,
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
    val currency: String,
    val payments: List<DocumentPrintPayment>,
    val legendText: String?,
    val qrData: String? = null,
    val sunatHash: String? = null,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
)

/** Ticket de venta — port de buildSaleDocumentEscPos (QR SUNAT en CPE electrónicos). */
object DocumentLayoutBuilder {

    fun build(input: DocumentPrintInput): ByteArray {
        val cols = columnsForPaper(input.paperWidth)
        val b = EscPosBuilder()
        val money = { n: Double -> "${input.currencySymbol()} ${"%.2f".format(n)}" }

        b.init()
        b.align(EscPosAlign.CENTER)
        b.bold(true)
        EscPosTextUtils.wrapText(input.companyName.ifBlank { "EMPRESA" }, cols).forEach { b.line(it) }
        b.bold(false)
        if (input.companyRuc.isNotBlank()) b.line("RUC: ${input.companyRuc}")
        input.companyAddress?.takeIf { it.isNotBlank() }?.let { addr ->
            EscPosTextUtils.wrapText(addr, cols).forEach { b.line(it) }
        }
        input.branchName?.takeIf { it.isNotBlank() }?.let { b.line(it) }

        b.divider(cols)
        b.bold(true)
        val docLabel = SunatPrintUtils.tipoComprobanteLabel(input.sunatCode, input.docType)
        EscPosTextUtils.wrapText(docLabel, cols).forEach { b.line(it) }
        b.line(input.number)
        b.bold(false)
        b.divider(cols)
        b.align(EscPosAlign.LEFT)

        b.line("Fecha: ${input.issueDate}")
        input.clientName?.takeIf { it.isNotBlank() }?.let { name ->
            EscPosTextUtils.wrapText("Cliente: $name", cols).forEach { b.line(it) }
        }
        input.clientDocNumber?.takeIf { it.isNotBlank() }?.let { b.line("Doc: $it") }

        b.divider(cols)
        b.line(itemHeader(cols))
        b.divider(cols)

        for (item in input.items) {
            appendItemLines(b, cols, item, money)
        }

        b.divider(cols)
        b.align(EscPosAlign.RIGHT)
        if (input.taxAmount > 0) b.line("IGV: ${money(input.taxAmount)}")
        b.bold(true)
        b.line("TOTAL: ${money(input.total)}")
        b.bold(false)

        input.legendText?.takeIf { it.isNotBlank() }?.let { legend ->
            b.align(EscPosAlign.LEFT)
            b.divider(cols)
            EscPosTextUtils.wrapText("Son: $legend", cols).forEach { b.line(it) }
        }

        if (input.payments.isNotEmpty()) {
            b.divider(cols)
            b.align(EscPosAlign.LEFT)
            b.line("Pagos:")
            input.payments.forEach { payment ->
                b.line("${payment.method}: ${money(payment.amount)}")
            }
        }

        val showQr = SunatPrintUtils.isElectronicSunatCode(input.sunatCode) &&
            !input.qrData.isNullOrBlank()
        if (showQr) {
            b.line()
            b.align(EscPosAlign.CENTER)
            val moduleSize = if (input.paperWidth == PaperWidthMm.W58) 6 else 8
            b.raw(EscPosQr.encode(input.qrData!!, moduleSize = moduleSize))
            b.line()
            input.sunatHash?.takeIf { it.isNotBlank() }?.let { hash ->
                EscPosTextUtils.wrapText("Hash: $hash", cols).forEach { b.line(it) }
            }
            EscPosTextUtils.wrapText("Representacion impresa CPE", cols).forEach { b.line(it) }
            EscPosTextUtils.wrapText("Consulte en sunat.gob.pe", cols).forEach { b.line(it) }
        }

        b.line()
        b.line("Gracias por su preferencia")
        b.line()
        b.cutPartial()
        return b.bytes()
    }

    private fun itemHeader(cols: Int): String {
        val right = "TOTAL"
        val left = "CANT  DESCRIPCION".padEnd(cols - right.length)
        return left + right
    }

    private fun appendItemLines(
        b: EscPosBuilder,
        cols: Int,
        item: DocumentPrintLine,
        money: (Double) -> String,
    ) {
        val qty = formatQty(item.quantity)
        val totalStr = money(item.total)
        val leftCols = cols - totalStr.length - 1
        val descWrapped = EscPosTextUtils.wrapText(
            item.description.trim(),
            maxOf(8, leftCols - (qty.length + 2)),
        )
        val firstLeft = "${qty}x ${descWrapped.firstOrNull().orEmpty()}".padEnd(leftCols)
        b.line("$firstLeft $totalStr")
        descWrapped.drop(1).forEach { w -> b.line("   $w") }
    }

    private fun formatQty(qty: Double): String = qty.toString().replace(Regex("\\.0+$"), "")

    private fun DocumentPrintInput.currencySymbol(): String = when (currency.uppercase()) {
        "USD" -> "USD"
        else -> "S/"
    }
}
