package com.bendey.restaurant.core.domain.help

/**
 * Markdown simple de la ayuda (el que genera `scripts/export-help.mjs` de Tauri): párrafos, `### ` como
 * subtítulo, pasos `1.`, listas `- `, citas `> Dato: / Ojo: / Truco: / Lo que NO hace:` y tablas `|`.
 * Inline: `**negrita**` y `` `código` ``. Pura y sin Android, para poder probarla.
 */
data class HelpSpan(val text: String, val bold: Boolean = false, val code: Boolean = false)

enum class HelpCalloutKind(val prefix: String, val label: String) {
    NOTE("Dato: ", "Dato"),
    WARN("Ojo: ", "Ojo"),
    TIP("Truco: ", "Truco"),
    NOPE("Lo que NO hace: ", "Lo que NO hace"),
}

sealed interface HelpBlock {
    data class Paragraph(val spans: List<HelpSpan>) : HelpBlock
    data class Heading(val text: String) : HelpBlock
    data class Steps(val items: List<List<HelpSpan>>) : HelpBlock
    data class Bullets(val items: List<List<HelpSpan>>) : HelpBlock
    data class Callout(val kind: HelpCalloutKind, val spans: List<HelpSpan>) : HelpBlock
    data class Table(val head: List<String>, val rows: List<List<String>>) : HelpBlock
}

private val STEP = Regex("""^\d+\.\s+(.*)$""")
private val BULLET = Regex("""^[-*]\s+(.*)$""")
private val TABLE_SEPARATOR = Regex("""^\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?$""")

fun parseHelpMarkdown(markdown: String): List<HelpBlock> {
    val blocks = ArrayList<HelpBlock>()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        when {
            line.isEmpty() -> i++
            line.startsWith("#") -> {
                blocks += HelpBlock.Heading(line.trimStart('#').trim().let(::stripBold))
                i++
            }
            line.startsWith(">") -> {
                val body = line.removePrefix(">").trim()
                val kind = HelpCalloutKind.entries.firstOrNull { body.startsWith(it.prefix) }
                blocks += HelpBlock.Callout(
                    kind = kind ?: HelpCalloutKind.NOTE,
                    spans = parseHelpSpans(if (kind != null) body.removePrefix(kind.prefix) else body),
                )
                i++
            }
            line.startsWith("|") -> {
                val rows = ArrayList<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val l = lines[i].trim()
                    if (!TABLE_SEPARATOR.matches(l)) rows += splitTableRow(l)
                    i++
                }
                if (rows.isNotEmpty()) blocks += HelpBlock.Table(head = rows.first(), rows = rows.drop(1))
            }
            STEP.matches(line) -> {
                val items = ArrayList<List<HelpSpan>>()
                while (i < lines.size) {
                    val m = STEP.matchEntire(lines[i].trim()) ?: break
                    items += parseHelpSpans(m.groupValues[1])
                    i++
                }
                blocks += HelpBlock.Steps(items)
            }
            BULLET.matches(line) -> {
                val items = ArrayList<List<HelpSpan>>()
                while (i < lines.size) {
                    val m = BULLET.matchEntire(lines[i].trim()) ?: break
                    items += parseHelpSpans(m.groupValues[1])
                    i++
                }
                blocks += HelpBlock.Bullets(items)
            }
            else -> {
                val parts = ArrayList<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.isEmpty() || l.startsWith("#") || l.startsWith(">") || l.startsWith("|") ||
                        STEP.matches(l) || BULLET.matches(l)
                    ) break
                    parts += l
                    i++
                }
                blocks += HelpBlock.Paragraph(parseHelpSpans(parts.joinToString(" ")))
            }
        }
    }
    return blocks
}

/** `a | b \| c | d` -> ["a", "b | c", "d"]. */
internal fun splitTableRow(line: String): List<String> {
    val cells = ArrayList<String>()
    val cur = StringBuilder()
    var k = 0
    val inner = line.trim().removePrefix("|").let { if (it.endsWith("|") && !it.endsWith("\\|")) it.dropLast(1) else it }
    while (k < inner.length) {
        val c = inner[k]
        if (c == '\\' && k + 1 < inner.length && inner[k + 1] == '|') {
            cur.append('|')
            k += 2
        } else if (c == '|') {
            cells += cur.toString().trim()
            cur.clear()
            k++
        } else {
            cur.append(c)
            k++
        }
    }
    cells += cur.toString().trim()
    return cells
}

private fun stripBold(s: String) = s.replace("**", "")

/** Trozos con `**negrita**` y `` `código` ``; lo que no cierra se deja como texto. */
fun parseHelpSpans(text: String): List<HelpSpan> {
    val spans = ArrayList<HelpSpan>()
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            spans += HelpSpan(plain.toString())
            plain.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                flush()
                spans += HelpSpan(text.substring(i + 2, end), bold = true)
                i = end + 2
                continue
            }
        }
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                flush()
                spans += HelpSpan(text.substring(i + 1, end), code = true)
                i = end + 1
                continue
            }
        }
        plain.append(text[i])
        i++
    }
    flush()
    return spans
}
