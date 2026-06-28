package com.bendey.restaurant.core.data.export

import java.io.File
import java.io.IOException

object ReportCsvWriter {
    fun write(file: File, rows: List<List<String>>) {
        file.parentFile?.mkdirs()
        try {
            file.bufferedWriter().use { writer ->
                rows.forEach { row ->
                    writer.appendLine(row.joinToString(",") { cell ->
                        val escaped = cell.replace("\"", "\"\"")
                        if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
                            "\"$escaped\""
                        } else {
                            escaped
                        }
                    })
                }
            }
        } catch (e: IOException) {
            throw IOException("No fue posible generar el archivo CSV.", e)
        }
    }
}
