package com.bendey.restaurant.platform.printing.escpos

/** QR Code Model 2 — port de escposQr (printers.service.ts). */
object EscPosQr {

    fun encode(
        data: String,
        moduleSize: Int = 8,
        ecc: Char = 'M',
    ): ByteArray {
        val size = moduleSize.coerceIn(1, 16)
        val eccByte = when (ecc.uppercaseChar()) {
            'L' -> 0x30
            'M' -> 0x31
            'Q' -> 0x32
            'H' -> 0x33
            else -> 0x31
        }
        val bytes = EscPosTextUtils.textBytes(data)
        val storeLen = bytes.size + 3
        val pL = storeLen and 0xff
        val pH = (storeLen shr 8) and 0xff
        val out = mutableListOf<Byte>()
        out.addAll(listOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00).map { it.toByte() })
        out.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size).map { it.toByte() })
        out.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, eccByte).map { it.toByte() })
        out.addAll(listOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30).map { it.toByte() })
        out.addAll(bytes.toList())
        out.addAll(listOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30).map { it.toByte() })
        return out.toByteArray()
    }
}
