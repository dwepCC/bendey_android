package com.bendey.restaurant.platform.printing.escpos

/** Ancho rollo térmico — paridad web. */
enum class PaperWidthMm(val mm: Int) {
    W58(58),
    W80(80),
    ;

    companion object {
        fun fromMm(mm: Int): PaperWidthMm = if (mm == 58) W58 else W80
    }
}

fun columnsForPaper(width: PaperWidthMm): Int = when (width) {
    PaperWidthMm.W58 -> 32
    PaperWidthMm.W80 -> 48
}

enum class ComandaTextSize {
    DEFAULT,
    MEDIANO,
}

/** Tamaño del logo en comprobantes (ticket ESC/POS y PDF). El factor `scale` se aplica
 *  igual en 58 y 80 mm. LARGE = ancho casi completo (comportamiento anterior). */
enum class LogoSize(val scale: Float) {
    SMALL(0.5f),
    MEDIUM(0.75f),
    LARGE(1f),
}

data class ComandaEscposLayout(
    val titleMul: Pair<Int, Int>,
    val headerMul: Pair<Int, Int>,
    val itemMul: Pair<Int, Int>,
    val widthColsDiv: Int,
)

fun comandaEscposLayout(textSize: ComandaTextSize, @Suppress("UNUSED_PARAMETER") cols: Int): ComandaEscposLayout {
    return if (textSize == ComandaTextSize.MEDIANO) {
        ComandaEscposLayout(
            titleMul = 1 to 2,
            headerMul = 1 to 2,
            itemMul = 1 to 2,
            widthColsDiv = 1,
        )
    } else {
        ComandaEscposLayout(
            titleMul = 2 to 2,
            headerMul = 2 to 2,
            itemMul = 2 to 2,
            widthColsDiv = 2,
        )
    }
}

fun comandaWrapCols(cols: Int, layout: ComandaEscposLayout): Int {
    val div = maxOf(1, layout.widthColsDiv)
    return maxOf(12, cols / div)
}
