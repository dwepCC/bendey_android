package com.bendey.restaurant.feature.dashboard

import android.content.Context
import com.bendey.restaurant.core.data.export.BendeyExportPaths
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.export.ReportCsvWriter
import com.bendey.restaurant.core.data.export.ReportPdfWriter
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalyticsRow
import com.bendey.restaurant.core.domain.dashboard.DashboardOrderTypeSlice
import com.bendey.restaurant.core.domain.dashboard.DashboardPaymentSlice
import com.bendey.restaurant.core.domain.dashboard.DashboardTopProduct
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import java.text.NumberFormat
import java.util.Locale

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

fun exportOperationalDashboardCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    data: RestaurantDashboard,
    from: String,
    to: String,
    branchName: String?,
): ExportShareResult {
    return try {
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
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/operacion-$from-$to.csv")
        ReportCsvWriter.write(file, rows)
        fileShareService.shareFile(context, file, "text/csv", "Exportar operación CSV")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun exportOperationalDashboardPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    data: RestaurantDashboard,
    from: String,
    to: String,
    branchName: String?,
): ExportShareResult {
    return try {
        val title = "Operación · ${periodLabel(from, to)}${branchName?.let { " · $it" } ?: ""}"
        val lines = buildList {
            add("Top productos (comandas)")
            data.topProducts.forEachIndexed { index, product ->
                add("${index + 1}. ${product.name} · qty ${product.quantity} · S/ ${fmt2(product.revenue)}")
            }
        }
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/operacion-$from-$to.pdf")
        ReportPdfWriter.writePortraitA4(file, title, lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar operación PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun exportCatalogAnalyticsCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    data: CatalogAnalytics,
    from: String,
    to: String,
    branchName: String?,
): ExportShareResult {
    return try {
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
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/catalogo-$from-$to.csv")
        ReportCsvWriter.write(file, rows)
        fileShareService.shareFile(context, file, "text/csv", "Exportar catálogo CSV")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun exportCatalogAnalyticsPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    data: CatalogAnalytics,
    from: String,
    to: String,
    branchName: String?,
): ExportShareResult {
    return try {
        val title = "Catálogo · ${periodLabel(from, to)}${branchName?.let { " · $it" } ?: ""}"
        val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        val lines = buildList {
            data.topProducts.forEachIndexed { index, row ->
                add("${index + 1}. ${row.label} · ${row.quantity} · ${currency.format(row.revenue)}")
            }
        }
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/catalogo-$from-$to.pdf")
        ReportPdfWriter.writePortraitA4(file, title, lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar catálogo PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun recentSessionStatusLabel(status: String): String = when (status.lowercase()) {
    "open" -> "Abierta"
    "paid" -> "Cobrada"
    "closed" -> "Cerrada"
    "cancelled" -> "Anulada"
    else -> status.ifBlank { "—" }
}
