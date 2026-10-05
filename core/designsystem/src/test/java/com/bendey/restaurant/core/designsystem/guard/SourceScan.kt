package com.bendey.restaurant.core.designsystem.guard

import java.io.File

/**
 * Utilidades de los guards de deriva de UI (R11, DESIGN-SYSTEM §12). Escanean las fuentes `.kt`
 * de producción del repo — no compilan nada — para que lo que se limpió en R11 no reaparezca.
 */
internal object SourceScan {

    /** Raíz del repo (carpeta con settings.gradle.kts), subiendo desde el directorio de trabajo del test. */
    fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("No se encontró settings.gradle.kts subiendo desde ${System.getProperty("user.dir")}")
    }

    class Source(val relPath: String, val raw: String) {
        /** Texto sin comentarios (mismas posiciones: los comentarios se vuelven espacios). */
        val code: String = stripComments(raw)
        fun lineOf(index: Int): Int = raw.substring(0, index.coerceIn(0, raw.length)).count { it == '\n' } + 1
        val fileName: String get() = relPath.substringAfterLast('/')
    }

    /** Todas las fuentes de producción (src/main) de todos los módulos, sin carpetas build. */
    fun productionSources(): List<Source> {
        val root = repoRoot()
        return root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { f ->
                val rel = f.relativeTo(root).invariantSeparatorsPath
                "/src/main/" in rel
            }
            .map { Source(it.relativeTo(root).invariantSeparatorsPath, it.readText()) }
            .toList()
    }

    /** Quita `// ...` y `/* ... */` respetando cadenas, conservando longitud y saltos de línea. */
    fun stripComments(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        var inString = false
        var inRaw = false
        while (i < s.length) {
            val c = s[i]
            if (inRaw) {
                out.append(c)
                if (s.startsWith("\"\"\"", i)) { out.append("\"\"").also { i += 2 }; inRaw = false }
                i++; continue
            }
            if (inString) {
                out.append(c)
                if (c == '\\' && i + 1 < s.length) { out.append(s[i + 1]); i += 2; continue }
                if (c == '"') inString = false
                i++; continue
            }
            when {
                s.startsWith("\"\"\"", i) -> { inRaw = true; out.append("\"\"\""); i += 3 }
                c == '"' -> { inString = true; out.append(c); i++ }
                c == '\'' && i + 2 < s.length && (s[i + 2] == '\'' || (s[i + 1] == '\\' && i + 3 < s.length && s[i + 3] == '\'')) -> {
                    val len = if (s[i + 1] == '\\') 4 else 3
                    out.append(s, i, i + len); i += len
                }
                s.startsWith("//", i) -> {
                    while (i < s.length && s[i] != '\n') { out.append(' '); i++ }
                }
                s.startsWith("/*", i) -> {
                    var depth = 0
                    while (i < s.length) {
                        if (s.startsWith("/*", i)) { depth++; out.append("  "); i += 2 }
                        else if (s.startsWith("*/", i)) { depth--; out.append("  "); i += 2; if (depth == 0) break }
                        else { out.append(if (s[i] == '\n' || s[i] == '\r') s[i] else ' '); i++ }
                    }
                }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    /** Índice del cierre que corresponde al abre en [open] ('(' o '{'), respetando cadenas. -1 si no cierra. */
    fun matching(code: String, open: Int): Int {
        val o = code[open]
        val c = if (o == '(') ')' else '}'
        var depth = 0
        var i = open
        var inString = false
        while (i < code.length) {
            val ch = code[i]
            if (inString) {
                if (ch == '\\') i++ else if (ch == '"') inString = false
            } else when (ch) {
                '"' -> inString = true
                o -> depth++
                c -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return -1
    }

    class Call(val name: String, val start: Int, val text: String)

    /**
     * Llamadas `Name(...)` (más su lambda final `{...}` si la hay) para los nombres dados. Ignora
     * declaraciones `fun Name(`.
     */
    fun calls(code: String, names: Set<String>): List<Call> {
        val result = mutableListOf<Call>()
        val regex = Regex("""(?<![A-Za-z0-9_.])(${names.joinToString("|") { Regex.escape(it) }})\s*\(""")
        for (m in regex.findAll(code)) {
            val before = code.substring(maxOf(0, m.range.first - 6), m.range.first)
            if (before.endsWith("fun ")) continue
            val open = m.range.last
            var end = matching(code, open)
            if (end < 0) continue
            var j = end + 1
            while (j < code.length && code[j].isWhitespace()) j++
            if (j < code.length && code[j] == '{') {
                val e2 = matching(code, j)
                if (e2 > 0) end = e2
            }
            result += Call(m.groupValues[1], m.range.first, code.substring(m.range.first, end + 1))
        }
        return result
    }

    /** Líneas contiguas de una cadena de Modifier alrededor de [index] (las que empiezan por `.` o nombran Modifier). */
    fun modifierChain(code: String, index: Int): String {
        val lines = code.split('\n')
        var pos = 0
        var li = 0
        while (li < lines.size && pos + lines[li].length + 1 <= index) { pos += lines[li].length + 1; li++ }
        if (li >= lines.size) return ""
        var a = li
        while (a > 0 && !lines[a].contains("Modifier") && !lines[a].contains("modifier")) {
            if (!lines[a].trim().startsWith(".")) break
            a--
        }
        var b = li
        while (b + 1 < lines.size && lines[b + 1].trim().startsWith(".")) b++
        return lines.subList(a, b + 1).joinToString("\n")
    }
}
