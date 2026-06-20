package com.bendey.restaurant.platform.printing.escpos

enum class EscPosAlign { LEFT, CENTER, RIGHT }

class EscPosBuilder {
    private val buffer = mutableListOf<Byte>()

    fun bytes(): ByteArray = buffer.toByteArray()

    fun init() = command(0x1B, 0x40)

    fun align(align: EscPosAlign) {
        val n = when (align) {
            EscPosAlign.LEFT -> 0
            EscPosAlign.CENTER -> 1
            EscPosAlign.RIGHT -> 2
        }
        command(0x1B, 0x61, n)
    }

    fun bold(on: Boolean) = command(0x1B, 0x45, if (on) 1 else 0)

    fun size(widthMul: Int, heightMul: Int) {
        val w = widthMul.coerceIn(1, 8)
        val h = heightMul.coerceIn(1, 8)
        val n = ((w - 1) shl 4) or (h - 1)
        command(0x1D, 0x21, n)
    }

    fun text(value: String) {
        buffer.addAll(EscPosTextUtils.textBytes(value).toList())
    }

    fun line(value: String = "") {
        text(if (value.isEmpty()) "\n" else "$value\n")
    }

    fun cutPartial() = command(0x1D, 0x56, 0x41, 0x10)

    fun raw(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }

    fun divider(cols: Int) = line("-".repeat(cols))

    private fun command(vararg bytes: Int) {
        bytes.forEach { buffer.add(it.toByte()) }
    }
}
