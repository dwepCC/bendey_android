package com.bendey.restaurant.core.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.IOException
import kotlin.math.ceil

object ReportPdfWriter {
    fun write(
        file: File,
        title: String,
        lines: List<String>,
        pageWidth: Int = 842,
        pageHeight: Int = 595,
    ) {
        file.parentFile?.mkdirs()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val lineHeight = 14f
        val margin = 36f
        val maxLinesPerPage = ((pageHeight - margin * 2) / lineHeight).toInt().coerceAtLeast(1)
        val pages = ceil(lines.size.toDouble() / maxLinesPerPage).toInt().coerceAtLeast(1)
        val document = PdfDocument()
        try {
            var lineIndex = 0
            for (pageIndex in 0 until pages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                var y = margin
                if (pageIndex == 0) {
                    canvas.drawText(title, margin, y, titlePaint)
                    y += lineHeight * 1.5f
                }
                repeat(maxLinesPerPage) {
                    if (lineIndex >= lines.size) return@repeat
                    canvas.drawText(lines[lineIndex], margin, y, paint)
                    y += lineHeight
                    lineIndex++
                }
                document.finishPage(page)
            }
            file.outputStream().use { document.writeTo(it) }
        } catch (e: IOException) {
            throw IOException("No fue posible generar el archivo PDF.", e)
        } finally {
            document.close()
        }
    }

    fun writePortraitA4(file: File, title: String, lines: List<String>) {
        write(file, title, lines, pageWidth = 595, pageHeight = 842)
    }
}
