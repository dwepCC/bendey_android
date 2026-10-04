package com.bendey.restaurant.core.domain.catalog

/**
 * Resumen y archivo de errores de la importación de la carta (R4). Android exporta un CSV (no un
 * .xlsx como Tauri): no hay escritor de .xlsx liviano para esto y el CSV lo abre Excel sin problema
 * (UTF-8 con BOM).
 */
object BulkImportReport {

    /** Filas distintas del archivo que se omiten por tener algún error. */
    fun omittedRows(validation: BulkImportValidationResult): Int =
        validation.errors.filter { it.row > 1 || it.column != "encabezados" }.map { it.row }.toSet().size

    /** Texto bajo el botón: "Se importarán 8 platos. Se omiten 2 filas con errores." */
    fun summary(validation: BulkImportValidationResult): String {
        val ok = validation.rows.size
        val omitted = omittedRows(validation)
        return buildString {
            append(if (ok == 1) "Se importará 1 plato." else "Se importarán $ok platos.")
            if (omitted > 0) {
                append(' ')
                append(if (omitted == 1) "Se omite 1 fila con errores." else "Se omiten $omitted filas con errores.")
            }
        }
    }

    /** Etiqueta del botón: "Importar solo las filas válidas (N)" (o "Importar (N)" si no hay errores). */
    fun importButtonLabel(validation: BulkImportValidationResult): String =
        if (validation.errors.isEmpty()) "Importar (${validation.rows.size})"
        else "Importar solo las filas válidas (${validation.rows.size})"

    /** CSV con TODAS las filas malas (sin truncar) y una columna con el motivo, más la fila original. */
    fun errorsCsv(validation: BulkImportValidationResult): String {
        val sb = StringBuilder("﻿")
        val header = listOf("fila", "columna", "motivo") + validation.headers
        sb.append(header.joinToString(",") { csv(it) }).append("\r\n")
        for (e in validation.errors) {
            val cells = listOf(e.row.toString(), e.column, e.message) + e.rawValues
            sb.append(cells.joinToString(",") { csv(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** CSV de las filas que el servidor no pudo crear tras importar. */
    fun failuresCsv(failed: List<BulkImportRowError>): String {
        val sb = StringBuilder("﻿")
        sb.append("fila,motivo\r\n")
        for (e in failed) sb.append("${e.row},${csv(e.message)}\r\n")
        return sb.toString()
    }

    private fun csv(value: String): String {
        // Evita que Excel interprete el valor como fórmula (inyección de CSV).
        val safe = if (value.isNotEmpty() && value[0] in "=+-@") "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }
}
