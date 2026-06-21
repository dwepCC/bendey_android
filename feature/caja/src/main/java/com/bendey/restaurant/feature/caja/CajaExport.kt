package com.bendey.restaurant.feature.caja

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bendey.restaurant.core.domain.cash.CashMovementReportRow
import com.bendey.restaurant.core.domain.cash.CashPaymentDetailRow
import java.io.File
import java.text.NumberFormat
import java.util.Locale

private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

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

fun exportCashMovementsCsv(
    context: Context,
    cashRows: List<CashMovementReportRow>,
    electronicRows: List<CashPaymentDetailRow>,
    from: String,
    to: String,
) {
    val rows = mutableListOf<List<String>>()
    rows += listOf("Movimientos efectivo")
    rows += listOf("Fecha", "Tipo", "Comprobante", "Usuario", "Método", "Monto (S/)")
    cashRows.forEach { row ->
        rows += listOf(
            row.date,
            row.type,
            row.docNumber.ifBlank { row.category.orEmpty() },
            row.userName,
            row.paymentMethod,
            fmt2(row.amount),
        )
    }
    if (electronicRows.isNotEmpty()) {
        rows.add(emptyList())
        rows += listOf("Cobros electrónicos")
        rows += listOf("Fecha", "Venta", "Pedido", "Usuario", "Método", "Monto (S/)")
        electronicRows.forEach { row ->
            rows += listOf(
                row.date,
                row.saleNumber,
                row.orderCode,
                row.userName.orEmpty(),
                row.method,
                fmt2(row.amount),
            )
        }
    }
    val file = File(context.cacheDir, "exports/movimientos-caja-$from-$to.csv")
    writeCsv(file, rows)
    shareFile(context, file, "text/csv", "Exportar movimientos Excel")
}

fun shareSessionReportPdf(context: Context, title: String, lines: List<String>) {
    file@ run {
        val file = File(context.cacheDir, "exports/reporte-caja.pdf")
        file.parentFile?.mkdirs()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val lineHeight = 14f
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val maxLines = ((pageHeight - margin * 2) / lineHeight).toInt().coerceAtLeast(1)
        val document = android.graphics.pdf.PdfDocument()
        var lineIndex = 0
        val pages = kotlin.math.ceil(lines.size.toDouble() / maxLines).toInt().coerceAtLeast(1)
        for (pageIndex in 0 until pages) {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = margin
            if (pageIndex == 0) {
                canvas.drawText(title, margin, y, titlePaint)
                y += lineHeight * 1.5f
            }
            repeat(maxLines) {
                if (lineIndex >= lines.size) return@repeat
                canvas.drawText(lines[lineIndex], margin, y, paint)
                y += lineHeight
                lineIndex++
            }
            document.finishPage(page)
        }
        file.outputStream().use { document.writeTo(it) }
        document.close()
        shareFile(context, file, "application/pdf", "Exportar reporte PDF")
    }
}

fun formatSessionReportLines(report: com.bendey.restaurant.core.domain.cash.CashSessionReport, products: List<com.bendey.restaurant.core.domain.cash.CashSessionProductSold>): List<String> {
    val session = report.session
    val lines = mutableListOf<String>()
    lines += "Reporte sesión #${session.id}"
    session.branchName?.let { lines += it }
    session.openedAt?.let { lines += "Apertura: $it" }
    session.closedAt?.let { lines += "Cierre: $it" }
    lines += "Apertura: ${currency.format(session.openingBalance)}"
    lines += "Ingresos: ${currency.format(report.totalIncome)}"
    lines += "Egresos: ${currency.format(report.totalExpense)}"
    lines += "Ventas netas: ${currency.format(report.totalNetSales)}"
    if (report.totalVoidedSales > 0) lines += "Ventas anuladas: ${currency.format(report.totalVoidedSales)}"
    lines += "Saldo final: ${currency.format(report.finalBalance)}"
    if (products.isNotEmpty()) {
        lines += ""
        lines += "Productos vendidos"
        products.forEach { p ->
            lines += "${p.quantity} x ${p.description} · ${currency.format(p.total)}"
        }
    }
    return lines
}
