package com.bendey.restaurant.feature.ventas

import android.content.Context
import com.bendey.restaurant.core.data.export.BendeyExportPaths
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.export.ReportCsvWriter
import com.bendey.restaurant.core.data.export.ReportPdfWriter
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.billingStatusLabel
import java.util.Locale

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

fun exportSalesListCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
): ExportShareResult {
    return try {
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
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/${exportFileBase(tab)}-$from-$to.csv")
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
): ExportShareResult {
    return try {
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
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/${exportFileBase(tab)}-$from-$to.pdf")
        ReportPdfWriter.write(file, title, lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar listado PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun shareBillingDocumentFile(
    context: Context,
    fileShareService: BendeyFileShareService,
    file: java.io.File,
    mimeType: String,
    title: String,
): ExportShareResult = fileShareService.shareFile(context, file, mimeType, title)
