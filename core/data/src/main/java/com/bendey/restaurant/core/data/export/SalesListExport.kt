package com.bendey.restaurant.core.data.export

import android.content.Context
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.billingStatusLabel
import com.bendey.restaurant.core.domain.sales.formatPaymentsCompact
import java.util.Locale

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

fun salesListExportTitle(tab: VentasTab, reportStyle: Boolean = false): String = when (tab) {
    VentasTab.NOTAS ->
        if (reportStyle) "Reporte — Notas de venta" else "Notas de venta"
    VentasTab.CREDITOS -> "Notas de crédito"
    VentasTab.FACTURACION ->
        if (reportStyle) "Reporte — Boletas y facturas" else "Facturas y boletas"
}

private fun exportFileBase(tab: VentasTab, reportStyle: Boolean): String =
    salesListExportTitle(tab, reportStyle)
        .replace("\\s+".toRegex(), "-")
        .replace("—", "")
        .lowercase(Locale.ROOT)

private fun saleStatusExportLabel(status: String): String =
    if (status.trim().equals("cancelled", ignoreCase = true)) "Anulada" else "Activa"

fun exportSalesListCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
    reportStyle: Boolean = false,
): ExportShareResult {
    return try {
        val includeBilling = tab != VentasTab.NOTAS
        val rows = mutableListOf<List<String>>()
        val headers = mutableListOf("Fecha", "Comprobante", "Cliente", "Total (S/)", "Métodos de pago", "Estado")
        if (includeBilling) headers += "Estado SUNAT"
        rows += headers
        sales.forEach { sale ->
            val row = mutableListOf(
                sale.issueDate,
                "${sale.docType} ${sale.displayNumber}".trim(),
                sale.contactName ?: "—",
                fmt2(sale.total),
                formatPaymentsCompact(sale, maxChars = 512),
                saleStatusExportLabel(sale.status),
            )
            if (includeBilling) {
                row += billingStatusLabel(sale.billingStatus)
            }
            rows += row
        }
        val file = BendeyExportPaths.exportFile(
            context,
            "${BendeyExportPaths.EXPORTS}/${exportFileBase(tab, reportStyle)}-$from-$to.csv",
        )
        ReportCsvWriter.write(file, rows)
        fileShareService.shareFile(context, file, "text/csv", "Exportar listado Excel")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun exportSalesListPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
    reportStyle: Boolean = false,
): ExportShareResult {
    return try {
        val includeBilling = tab != VentasTab.NOTAS
        val title = "${salesListExportTitle(tab, reportStyle)} · $from — $to"
        val header = buildString {
            append(String.format("%-12s", "Fecha"))
            append(String.format("%-20s", "Comprobante"))
            append(String.format("%-20s", "Cliente"))
            append(String.format("%10s", "Total"))
            append(String.format("%-28s", "Métodos de pago"))
            append(String.format("%-10s", "Estado"))
            if (includeBilling) append(String.format("%-16s", "Estado SUNAT"))
        }
        val lines = mutableListOf(header, "-".repeat(header.length.coerceAtMost(140)))
        sales.forEach { sale ->
            val line = buildString {
                append(String.format("%-12s", sale.issueDate.take(12)))
                append(String.format("%-20s", "${sale.docType} ${sale.displayNumber}".trim().take(20)))
                append(String.format("%-20s", (sale.contactName ?: "—").take(20)))
                append(String.format("%10s", fmt2(sale.total)))
                append(String.format("%-28s", formatPaymentsCompact(sale, maxChars = 28)))
                append(String.format("%-10s", saleStatusExportLabel(sale.status).take(10)))
                if (includeBilling) {
                    append(String.format("%-16s", billingStatusLabel(sale.billingStatus).take(16)))
                }
            }
            lines += line
        }
        val file = BendeyExportPaths.exportFile(
            context,
            "${BendeyExportPaths.EXPORTS}/${exportFileBase(tab, reportStyle)}-$from-$to.pdf",
        )
        ReportPdfWriter.write(file, title, lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar listado PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}
