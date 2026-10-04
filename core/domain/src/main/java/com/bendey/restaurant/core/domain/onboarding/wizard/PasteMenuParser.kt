package com.bendey.restaurant.core.domain.onboarding.wizard

/**
 * Parser de "Pegar mi lista" (R4). Kotlin PURO, sin IA: solo reglas. Réplica EXACTA de
 * `pasteMenuParser.ts` (Tauri); la tabla de casos de `PasteMenuParserTest` es la misma. Si cambias
 * algo aquí, cámbialo allá.
 *
 * Por línea (trim; las vacías se ignoran):
 *  1. Termina en ":" y NO tiene ningún dígito -> categoría actual (texto sin los ":").
 *  2. Si no, es producto = nombre + precio. El precio es el ÚLTIMO número de la línea
 *     (`35`, `35.5`, `35,50`, `S/ 35`, `S/35`, `s/. 35`, `35 soles`). Separadores entre nombre y precio:
 *     espacios, `-`, `–`, `—`, `:`, tab, `.....`, `|`. Se quitan viñetas iniciales (`-`, `•`, `*`, `1.`, `1)`).
 *  3. Nombre: espacios normalizados; si TODO está en mayúsculas pasa a "Title Case" simple; máx. 120.
 *  4. Sin categoría actual -> "General".
 *  5. DUDOSA (doubtful=true; en la vista previa queda desmarcada): varios precios, precio 0 o > 9999,
 *     nombre de menos de 2 caracteres, o decimales de más de 2 dígitos (`1,500` se lee como 1500).
 *  6. INVÁLIDA (error): sin precio -> "Falta el precio"; sin nombre -> "Falta el nombre".
 *  7. Duplicados (misma categoría + mismo nombre, sin distinguir mayúsculas) -> warning, no error.
 *  8. Tope: 300 líneas de producto por pegado (las categorías no cuentan); el resto se ignora y
 *     `truncated = true`.
 */
object PasteMenu {
    const val MAX_PASTE_ROWS = 300
    const val MAX_NAME_LENGTH = 120
    const val MAX_REASONABLE_PRICE = 9999.0
    const val DEFAULT_CATEGORY = "General"

    const val MSG_MISSING_PRICE = "Falta el precio"
    const val MSG_MISSING_NAME = "Falta el nombre"
    const val MSG_SEVERAL_PRICES = "Tiene más de un precio: revisa cuál es"
    const val MSG_ODD_PRICE = "Precio fuera de lo normal: revísalo"
    const val MSG_SHORT_NAME = "Nombre muy corto: revísalo"
    const val MSG_DUPLICATE = "Está repetido en tu lista"
    const val MSG_LIMIT = "Pega hasta 300 platos a la vez"

    fun msgThousands(n: Long): String = "Revisa el precio: lo leímos como $n"
}

data class PasteRow(
    /** Número de línea en el texto pegado (1-based). */
    val line: Int,
    val category: String,
    val name: String,
    /** null cuando no se encontró precio (fila inválida). */
    val price: Double?,
    val doubtful: Boolean,
    val error: String? = null,
    val warning: String? = null,
)

data class PasteParseResult(
    val rows: List<PasteRow>,
    /** Categorías en orden de aparición (declaradas con `:` y "General" si alguna fila la usa). */
    val categories: List<String>,
    /** Había más de 300 líneas de producto: las sobrantes se ignoraron. */
    val truncated: Boolean,
)

data class RowClassification(val doubtful: Boolean, val error: String? = null, val warning: String? = null)

private const val NUM = "\\d+(?:[.,]\\d+)?"
private val SEP_TRAIL = Regex("[\\s\\-–—:|.\\t]+$")
private val CURRENCY_TRAIL = Regex("\\s*\\bs/\\.?$", RegexOption.IGNORE_CASE)
private val BULLET = Regex("^(?:[-•*·–—]+\\s*|\\d+[.)]\\s+)")
private val TRAILING_NUMBER = Regex("($NUM)\\s*$")
private val CONNECTOR_TAIL =
    Regex("($NUM)\\s*(?:/|-|–|—|\\bo\\b|\\bu\\b|\\by\\b)\\s*(?:s/\\.?\\s*)?$", RegexOption.IGNORE_CASE)
private val PLAIN_NUMBER_TAIL = Regex("(?:^|\\s)(?:s/\\.?\\s*)?$NUM$", RegexOption.IGNORE_CASE)
private val SOLES_TAIL = Regex("\\s+(?:soles|sol)\\s*$", RegexOption.IGNORE_CASE)
private val WHITESPACE = Regex("\\s+")
private val NUMBER_TOKEN = Regex("^(\\d+)(?:[.,](\\d+))?$")
private val BOUNDARY = Regex("[\\s\\-–—:|.\\t/]")
private val CATEGORY_COLONS = Regex(":+\\s*$")
private val LINE_BREAK = Regex("\r\n|\r|\n")

private fun collapseSpaces(s: String): String = s.replace(WHITESPACE, " ").trim()

/** Quita separadores y la moneda "S/" del final, hasta que no quede nada por quitar. */
private fun stripTrailingNoise(s: String): String {
    var out = s
    for (i in 0 until 4) {
        val next = out.replace(SEP_TRAIL, "").replace(CURRENCY_TRAIL, "")
        if (next == out) break
        out = next
    }
    return out.replace(SEP_TRAIL, "")
}

/** "CEVICHE MIXTO" -> "Ceviche Mixto" solo si TODO el texto está en mayúsculas. */
fun normalizeProductName(raw: String): String {
    var name = collapseSpaces(raw)
    if (name.isNotEmpty() && name == name.uppercase() && name != name.lowercase()) {
        val lower = name.lowercase()
        val sb = StringBuilder(lower.length)
        var prevIsSpace = true
        for (ch in lower) {
            sb.append(if (prevIsSpace && ch.isLetter()) ch.uppercaseChar() else ch)
            prevIsSpace = ch == ' '
        }
        name = sb.toString()
    }
    return if (name.length > PasteMenu.MAX_NAME_LENGTH) name.take(PasteMenu.MAX_NAME_LENGTH).trim() else name
}

/** Clasificación de una fila ya con nombre y precio (también se usa al EDITAR en la vista previa). */
fun classifyPasteRow(name: String, price: Double?): RowClassification {
    val n = name.trim()
    if (price == null || !price.isFinite()) return RowClassification(false, error = PasteMenu.MSG_MISSING_PRICE)
    if (n.isEmpty()) return RowClassification(false, error = PasteMenu.MSG_MISSING_NAME)
    if (price <= 0 || price > PasteMenu.MAX_REASONABLE_PRICE) {
        return RowClassification(true, warning = PasteMenu.MSG_ODD_PRICE)
    }
    if (n.length < 2) return RowClassification(true, warning = PasteMenu.MSG_SHORT_NAME)
    return RowClassification(false)
}

private fun round2(n: Double): Double = Math.round(n * 100.0) / 100.0

private data class ParsedNumber(val value: Double, val thousands: Boolean)

private fun parseNumberToken(token: String): ParsedNumber {
    val m = NUMBER_TOKEN.find(token) ?: return ParsedNumber(Double.NaN, false)
    val int = m.groupValues[1]
    val frac = m.groupValues[2]
    if (frac.isEmpty()) return ParsedNumber(int.toDouble(), false)
    // `1,500` / `28.500`: 3+ decimales = separador de miles, no precio con decimales.
    if (frac.length > 2) return ParsedNumber((int + frac).toDouble(), true)
    return ParsedNumber(round2("$int.$frac".toDouble()), false)
}

fun parsePasteMenu(text: String): PasteParseResult {
    val rows = mutableListOf<PasteRow>()
    val categories = mutableListOf<String>()
    fun addCategory(c: String) {
        if (c !in categories) categories += c
    }
    var currentCategory = PasteMenu.DEFAULT_CATEGORY
    var categoryDeclared = false
    var truncated = false
    var productLines = 0

    val lines = text.split(LINE_BREAK)
    for (i in lines.indices) {
        val trimmed = lines[i].trim()
        if (trimmed.isEmpty()) continue
        val lineNo = i + 1

        // 1. Categoría
        if (trimmed.endsWith(":") && trimmed.none { it.isDigit() }) {
            val cat = collapseSpaces(trimmed.replace(CATEGORY_COLONS, ""))
            if (cat.isNotEmpty()) {
                currentCategory = cat
                categoryDeclared = true
                addCategory(cat)
                continue
            }
        }

        // Viñeta inicial y moneda escrita en palabras al final.
        var s = trimmed.replace(BULLET, "").trim()
        if (s.isEmpty()) continue
        s = s.replace(SEP_TRAIL, "").replace(SOLES_TAIL, "")

        if (productLines >= PasteMenu.MAX_PASTE_ROWS) {
            truncated = true
            break
        }
        productLines++
        if (!categoryDeclared) addCategory(PasteMenu.DEFAULT_CATEGORY)

        // 2. Precio = último número, si va al inicio, tras un espacio o tras un separador.
        var price: Double? = null
        var namePart = s
        var doubtfulReason: String? = null
        val m = TRAILING_NUMBER.find(s)
        val idx = m?.range?.first ?: -1
        val before = if (idx > 0) s[idx - 1].toString() else ""
        val okBoundary = idx == 0 || (before.isNotEmpty() && BOUNDARY.containsMatchIn(before))
        if (m != null && okBoundary) {
            val parsed = parseNumberToken(m.groupValues[1])
            price = parsed.value
            if (parsed.thousands) doubtfulReason = PasteMenu.msgThousands(parsed.value.toLong())
            namePart = s.substring(0, idx)

            // 5. Varios precios: conector (`15 / `, `10-`, `15 o `) o número suelto justo antes.
            val conn = CONNECTOR_TAIL.find(namePart)
            if (conn != null) {
                namePart = namePart.substring(0, conn.range.first)
                doubtfulReason = PasteMenu.MSG_SEVERAL_PRICES
            } else {
                val rest = stripTrailingNoise(namePart)
                if (PLAIN_NUMBER_TAIL.containsMatchIn(rest)) doubtfulReason = PasteMenu.MSG_SEVERAL_PRICES
            }
        }

        val name = normalizeProductName(stripTrailingNoise(namePart))
        val cls = classifyPasteRow(name, price)
        rows += if (cls.error != null) {
            PasteRow(lineNo, currentCategory, name, price, doubtful = false, error = cls.error)
        } else {
            PasteRow(
                lineNo,
                currentCategory,
                name,
                price,
                doubtful = cls.doubtful || doubtfulReason != null,
                warning = doubtfulReason ?: cls.warning,
            )
        }
    }

    // 7. Duplicados
    val seen = HashSet<String>()
    for (k in rows.indices) {
        val r = rows[k]
        if (r.name.isEmpty()) continue
        val key = "${r.category.lowercase()}|${r.name.lowercase()}"
        if (!seen.add(key)) {
            rows[k] = r.copy(
                warning = r.warning?.let { "$it. ${PasteMenu.MSG_DUPLICATE}" } ?: PasteMenu.MSG_DUPLICATE,
            )
        }
    }

    return PasteParseResult(rows, categories, truncated)
}
