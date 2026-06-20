package com.bendey.restaurant.platform.printing.escpos

data class ComandaItem(
    val productName: String,
    val quantity: Double,
    val notes: String? = null,
    val modifierLines: List<String> = emptyList(),
)

data class ComandaPrintInput(
    val tableName: String? = null,
    val orderNumber: Int? = null,
    val waiterName: String? = null,
    val items: List<ComandaItem>,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
    val textSize: ComandaTextSize = ComandaTextSize.DEFAULT,
)

/** Port de buildComandaEscPos — printers.service.ts */
object ComandaLayoutBuilder {

    fun build(input: ComandaPrintInput): ByteArray {
        val cols = columnsForPaper(input.paperWidth)
        val layout = comandaEscposLayout(input.textSize, cols)
        val wrapCols = comandaWrapCols(cols, layout)
        val b = EscPosBuilder()

        b.init()
        b.align(EscPosAlign.CENTER)
        b.bold(true)
        b.size(layout.titleMul.first, layout.titleMul.second)
        b.line("COMANDA")
        b.bold(false)
        b.size(1, 1)

        b.align(EscPosAlign.LEFT)
        b.divider(cols)

        b.bold(true)
        b.size(layout.headerMul.first, layout.headerMul.second)
        input.tableName?.let { name ->
            EscPosTextUtils.wrapText("MESA: $name", wrapCols).forEach { b.line(it) }
        }
        input.orderNumber?.let { b.line("PEDIDO: #$it") }
        input.waiterName?.let { name ->
            EscPosTextUtils.wrapText("MOZO: $name", wrapCols).forEach { b.line(it) }
        }
        b.bold(false)
        b.size(1, 1)
        b.divider(cols)

        for (item in input.items) {
            val qty = formatQty(item.quantity)
            val head = "${qty}x "
            val wrapped = EscPosTextUtils.wrapText(
                item.productName.trim(),
                maxOf(6, wrapCols - head.length),
            )

            b.bold(true)
            b.size(layout.itemMul.first, layout.itemMul.second)
            b.line("$head${wrapped.firstOrNull().orEmpty()}")
            wrapped.drop(1).forEach { w ->
                b.line("${" ".repeat(head.length)}$w")
            }
            b.bold(false)
            b.size(1, 1)

            item.modifierLines.forEach { mod ->
                val line = mod.trim()
                if (line.isNotEmpty()) {
                    EscPosTextUtils.wrapText(line, cols - 4).forEach { w ->
                        b.line("  * $w")
                    }
                }
            }
            item.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
                EscPosTextUtils.wrapText("Obs: $note", cols - 4).forEach { w ->
                    b.line("  > $w")
                }
            }
            b.line()
        }

        b.divider(cols)
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
