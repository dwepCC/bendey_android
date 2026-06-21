package com.bendey.restaurant.feature.ventas

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.billingStatusLabel
import java.io.File
import java.util.Locale
import kotlin.math.ceil

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun exportTitle(tab: VentasTab): String = when (tab) {
    VentasTab.NOTAS -> "Notas de venta"
    VentasTab.CREDITOS -> "Notas de crédito"
    VentasTab.FACTURACION -> "Facturas y boletas"
}

private fun exportFileBase(tab: VentasTab): String =
    exportTitle(tab).replace("\\s+".toRegex(), "-").lowercase(Locale.ROOT)

private fun saleStatusExportLabel(status: String): String =
    if (status.trim().equals("cancelled", ignoreCase = true)) "Anulada" else "Activa"

private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun writeCsv(file: File, rows: List<List<String>>) {
    file.parentFile?.mkdirs()
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
}

private fun writeSimplePdf(file: File, title: String, lines: List<String>) {
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
    val pageWidth = 842
    val pageHeight = 595
    val margin = 36f
    val maxLinesPerPage = ((pageHeight - margin * 2) / lineHeight).toInt().coerceAtLeast(1)
    val pages = ceil(lines.size.toDouble() / maxLinesPerPage).toInt().coerceAtLeast(1)
    val document = PdfDocument()
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
    document.close()
}

fun exportSalesListCsv(
    context: Context,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
) {
    val includeBilling = tab != VentasTab.NOTAS
    val rows = mutableListOf<List<String>>()
    val headers = mutableListOf("Fecha", "Comprobante", "Cliente", "Total (S/)", "Estado")
    if (includeBilling) headers += "Estado SUNAT"
    rows += headers
    sales.forEach { sale ->
        val row = mutableListOf(
            sale.issueDate,
            "${sale.docType} ${sale.displayNumber}".trim(),
            sale.contactName ?: "—",
            fmt2(sale.total),
            saleStatusExportLabel(sale.status),
        )
        if (includeBilling) {
            row += billingStatusLabel(sale.billingStatus)
        }
        rows += row
    }
    val file = File(context.cacheDir, "exports/${exportFileBase(tab)}-$from-$to.csv")
    writeCsv(file, rows)
    shareFile(context, file, "text/csv", "Exportar listado Excel")
}

fun exportSalesListPdf(
    context: Context,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
) {
    val includeBilling = tab != VentasTab.NOTAS
    val title = "${exportTitle(tab)} · $from — $to"
    val header = buildString {
        append(String.format("%-12s", "Fecha"))
        append(String.format("%-22s", "Comprobante"))
        append(String.format("%-24s", "Cliente"))
        append(String.format("%10s", "Total"))
        append(String.format("%-10s", "Estado"))
        if (includeBilling) append(String.format("%-18s", "Estado SUNAT"))
    }
    val lines = mutableListOf(header, "-".repeat(header.length.coerceAtMost(120)))
    sales.forEach { sale ->
        val line = buildString {
            append(String.format("%-12s", sale.issueDate.take(12)))
            append(String.format("%-22s", "${sale.docType} ${sale.displayNumber}".trim().take(22)))
            append(String.format("%-24s", (sale.contactName ?: "—").take(24)))
            append(String.format("%10s", fmt2(sale.total)))
            append(String.format("%-10s", saleStatusExportLabel(sale.status).take(10)))
            if (includeBilling) {
                append(String.format("%-18s", billingStatusLabel(sale.billingStatus).take(18)))
            }
        }
        lines += line
    }
    val file = File(context.cacheDir, "exports/${exportFileBase(tab)}-$from-$to.pdf")
    writeSimplePdf(file, title, lines)
    shareFile(context, file, "application/pdf", "Exportar listado PDF")
}

fun shareBillingDocumentFile(context: Context, file: File, mimeType: String, title: String) {
    shareFile(context, file, mimeType, title)
}
