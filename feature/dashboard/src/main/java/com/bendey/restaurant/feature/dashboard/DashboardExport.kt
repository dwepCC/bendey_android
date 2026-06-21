package com.bendey.restaurant.feature.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalyticsRow
import com.bendey.restaurant.core.domain.dashboard.DashboardOrderTypeSlice
import com.bendey.restaurant.core.domain.dashboard.DashboardPaymentSlice
import com.bendey.restaurant.core.domain.dashboard.DashboardTopProduct
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun periodLabel(from: String, to: String): String =
    if (from == to) from else "$from — $to"

private fun rankSection(
    lines: MutableList<List<String>>,
    title: String,
    items: List<CatalogAnalyticsRow>,
) {
    lines += listOf(title)
    lines += listOf("#", "Nombre", "Cantidad", "Ingresos (S/)")
    items.forEachIndexed { index, item ->
        lines += listOf(
            (index + 1).toString(),
            item.label,
            item.quantity.toString(),
            fmt2(item.revenue),
        )
    }
    lines.add(emptyList())
}

private fun topProductSection(
    lines: MutableList<List<String>>,
    title: String,
    items: List<DashboardTopProduct>,
) {
    lines += listOf(title)
    lines += listOf("#", "Nombre", "Cantidad", "Ingresos (S/)")
    items.forEachIndexed { index, item ->
        lines += listOf(
            (index + 1).toString(),
            item.name,
            item.quantity.toString(),
            fmt2(item.revenue),
        )
    }
    lines.add(emptyList())
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

private fun writeSimplePdf(file: File, title: String, lines: List<String>) {
    file.parentFile?.mkdirs()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        isFakeBoldText = true
    }
    val lineHeight = 16f
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
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

fun exportOperationalDashboardCsv(
    context: Context,
    data: RestaurantDashboard,
    from: String,
    to: String,
    branchName: String?,
) {
    val summary = data.summary
    val rows = mutableListOf<List<String>>()
    rows += listOf("Métrica", "Valor")
    rows += listOf("Período", periodLabel(from, to))
    rows += listOf("Sucursal", branchName ?: "Sucursal")
    rows += listOf("Ingresos (S/)", fmt2(summary.totalRevenue))
    rows += listOf("Pedidos", summary.totalSessions.toString())
    rows += listOf("Cobrados", summary.paidSessions.toString())
    rows += listOf("Abiertos", summary.openSessions.toString())
    rows += listOf("Cancelados", summary.cancelledSessions.toString())
    rows += listOf("Ticket promedio (S/)", fmt2(summary.avgTicket))
    rows += listOf("Comensales", summary.totalGuests.toString())
    rows.add(emptyList())
    topProductSection(rows, "Top productos (comandas)", data.topProducts)
    if (data.orderTypes.isNotEmpty()) {
        rows += listOf("Por tipo de pedido")
        rows += listOf("Tipo", "Cantidad", "Ingresos (S/)")
        data.orderTypes.forEach { slice: DashboardOrderTypeSlice ->
            rows += listOf(slice.type, slice.count.toString(), fmt2(slice.revenue))
        }
        rows.add(emptyList())
    }
    if (data.paymentMethods.isNotEmpty()) {
        rows += listOf("Por método de pago")
        rows += listOf("Método", "Cantidad", "Monto (S/)")
        data.paymentMethods.forEach { slice: DashboardPaymentSlice ->
            rows += listOf(slice.method, slice.count.toString(), fmt2(slice.amount))
        }
    }
    val file = File(context.cacheDir, "exports/operacion-$from-$to.csv")
    writeCsv(file, rows)
    shareFile(context, file, "text/csv", "Exportar operación CSV")
}

fun exportOperationalDashboardPdf(
    context: Context,
    data: RestaurantDashboard,
    from: String,
    to: String,
    branchName: String?,
) {
    val title = "Operación · ${periodLabel(from, to)}${branchName?.let { " · $it" } ?: ""}"
    val lines = buildList {
        add("Top productos (comandas)")
        data.topProducts.forEachIndexed { index, product ->
            add("${index + 1}. ${product.name} · qty ${product.quantity} · S/ ${fmt2(product.revenue)}")
        }
    }
    val file = File(context.cacheDir, "exports/operacion-$from-$to.pdf")
    writeSimplePdf(file, title, lines)
    shareFile(context, file, "application/pdf", "Exportar operación PDF")
}

fun exportCatalogAnalyticsCsv(
    context: Context,
    data: CatalogAnalytics,
    from: String,
    to: String,
    branchName: String?,
) {
    val kpi = data.kpi
    val rows = mutableListOf<List<String>>()
    rows += listOf("Métrica", "Valor")
    rows += listOf("Período", periodLabel(from, to))
    rows += listOf("Sucursal", branchName ?: "Sucursal")
    rows += listOf("Ventas del período (S/)", fmt2(kpi.totalRevenue))
    rows += listOf("Comprobantes", kpi.salesCount.toString())
    rows += listOf("Productos vendidos", kpi.productsSold.toString())
    rows += listOf("Combos vendidos", kpi.combosSold.toString())
    rows += listOf("Ingresos extras (S/)", fmt2(kpi.extrasRevenue))
    rows += listOf("Ticket promedio (S/)", fmt2(kpi.avgTicket))
    rows += listOf("Participación combos (%)", fmt2(data.comboParticipationPct))
    rows += listOf("Ticket con combo (S/)", fmt2(data.avgTicketWithCombo))
    rows += listOf("Ticket sin combo (S/)", fmt2(data.avgTicketWithoutCombo))
    rows.add(emptyList())
    rankSection(rows, "Top productos", data.topProducts)
    rankSection(rows, "Top combos", data.topCombos)
    rankSection(rows, "Top presentaciones", data.topPresentations)
    rankSection(rows, "Top extras", data.topExtras)
    val file = File(context.cacheDir, "exports/catalogo-$from-$to.csv")
    writeCsv(file, rows)
    shareFile(context, file, "text/csv", "Exportar catálogo CSV")
}

fun exportCatalogAnalyticsPdf(
    context: Context,
    data: CatalogAnalytics,
    from: String,
    to: String,
    branchName: String?,
) {
    val title = "Catálogo · ${periodLabel(from, to)}${branchName?.let { " · $it" } ?: ""}"
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val lines = buildList {
        data.topProducts.forEachIndexed { index, row ->
            add("${index + 1}. ${row.label} · ${row.quantity} · ${currency.format(row.revenue)}")
        }
    }
    val file = File(context.cacheDir, "exports/catalogo-$from-$to.pdf")
    writeSimplePdf(file, title, lines)
    shareFile(context, file, "application/pdf", "Exportar catálogo PDF")
}

fun recentSessionStatusLabel(status: String): String = when (status.lowercase()) {
    "open" -> "Abierta"
    "paid" -> "Cobrada"
    "closed" -> "Cerrada"
    "cancelled" -> "Anulada"
    else -> status.ifBlank { "—" }
}
