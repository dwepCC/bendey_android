package com.bendey.restaurant.platform.printing.escpos

data class PrecuentaItem(
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
)

data class PrecuentaPrintInput(
    val tableName: String? = null,
    val items: List<PrecuentaItem>,
    val total: Double,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
)

/** Port de buildPrecuentaEscPos — printers.service.ts */
object PrecuentaLayoutBuilder {

    fun build(input: PrecuentaPrintInput): ByteArray {
        val cols = columnsForPaper(input.paperWidth)
        val b = EscPosBuilder()

        b.init()
        b.align(EscPosAlign.CENTER)
        b.bold(true)
        b.size(2, 2)
        b.line("PRECUENTA")
        b.bold(false)
        b.size(1, 1)

        b.align(EscPosAlign.LEFT)
        input.tableName?.let { name ->
            EscPosTextUtils.wrapText("Mesa: $name", cols).forEach { b.line(it) }
        }
        b.divider(cols)

        for (item in input.items) {
            val qty = formatQty(item.quantity)
            val subtotal = "%.2f".format(item.quantity * item.unitPrice)
            val right = "S/ $subtotal"
            val leftCols = cols - right.length - 1
            val descWrapped = EscPosTextUtils.wrapText(
                item.productName.trim(),
                maxOf(8, leftCols - (qty.length + 2)),
            )
            val firstLeft = "${qty}x ${descWrapped.firstOrNull().orEmpty()}".padEnd(leftCols)
            b.line("$firstLeft $right")
            descWrapped.drop(1).forEach { w -> b.line("   $w") }
        }

        b.divider(cols)
        b.bold(true)
        b.size(2, 2)
        val totalStr = "S/ ${"%.2f".format(input.total)}"
        val bigCols = maxOf(12, cols / 2)
        EscPosTextUtils.wrapText("TOTAL $totalStr", bigCols).forEach { b.line(it) }
        b.bold(false)
        b.size(1, 1)
        b.line()
        b.line()
        b.cutPartial()
        return b.bytes()
    }

    private fun formatQty(qty: Double): String {
        val s = qty.toString()
        return s.replace(Regex("\\.0+$"), "")
    }
}
