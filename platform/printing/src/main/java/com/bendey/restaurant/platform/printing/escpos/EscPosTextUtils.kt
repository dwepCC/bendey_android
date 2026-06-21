package com.bendey.restaurant.platform.printing.escpos

import java.text.Normalizer

object EscPosTextUtils {

    private val symbolReplacements = listOf(
        Regex("ß") to "ss",
        Regex("¿") to "?",
        Regex("¡") to "!",
        Regex("€") to "EUR",
        Regex("°") to "o",
        Regex("ª") to "a",
        Regex("º") to "o",
        Regex("[—–]") to "-",
        Regex("…") to "...",
        Regex("[·•]") to "-",
        Regex("[\u201c\u201d]") to "\"",
        Regex("[\u2018\u2019]") to "'",
        Regex("[\u200b-\u200d\ufeff]") to "",
    )

    /** Paridad con normalizeTextForTicketPrint.ts */
    fun normalizeForPrint(text: String): String {
        if (text.isEmpty()) return ""
        var s = text
        s = s.replace(Regex("ñ([oO])"), "ni$1")
        s = s.replace(Regex("Ñ([oO])"), "Ni$1")
        s = s.replace("ñ", "n").replace("Ñ", "N")
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        for ((pattern, replacement) in symbolReplacements) {
            s = s.replace(pattern, replacement)
        }
        return s
    }

    fun textBytes(s: String): ByteArray =
        normalizeForPrint(s).toByteArray(Charsets.UTF_8)

    fun wrapText(s: String, width: Int): List<String> {
        val clean = normalizeForPrint(s).replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return listOf("")
        if (width <= 0) return listOf(clean)
        val words = clean.split(' ')
        val out = mutableListOf<String>()
        var line = ""
        for (w in words) {
            val next = if (line.isEmpty()) w else "$line $w"
            if (next.length <= width) {
                line = next
                continue
            }
            if (line.isNotEmpty()) out.add(line)
            if (w.length > width) {
                var i = 0
                while (i < w.length) {
                    out.add(w.substring(i, minOf(i + width, w.length)))
                    i += width
                }
                line = ""
            } else {
                line = w
            }
        }
        if (line.isNotEmpty()) out.add(line)
        return out
    }
}
