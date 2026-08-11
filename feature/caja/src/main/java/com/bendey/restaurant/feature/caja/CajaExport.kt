package com.bendey.restaurant.feature.caja

import android.content.Context
import com.bendey.restaurant.core.data.export.BendeyExportPaths
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.export.ReportCsvWriter
import com.bendey.restaurant.core.data.export.ReportPdfWriter
import com.bendey.restaurant.core.domain.cash.CashMovementReportRow
import com.bendey.restaurant.core.domain.cash.CashPaymentDetailRow
import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabelEs
import java.text.NumberFormat
import java.util.Locale

private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

fun exportCashMovementsCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    cashRows: List<CashMovementReportRow>,
    electronicRows: List<CashPaymentDetailRow>,
    from: String,
    to: String,
): ExportShareResult {
    return try {
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
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/movimientos-caja-$from-$to.csv")
        ReportCsvWriter.write(file, rows)
        fileShareService.shareFile(context, file, "text/csv", "Exportar movimientos Excel")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun shareSessionReportPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    title: String,
    lines: List<String>,
): ExportShareResult {
    return try {
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/reporte-caja.pdf")
        ReportPdfWriter.writePortraitA4(file, title, lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar reporte PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun shareArqueoPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    lines: List<String>,
): ExportShareResult {
    return try {
        val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/arqueo-caja.pdf")
        ReportPdfWriter.writePortraitA4(file, "Arqueo de caja", lines)
        fileShareService.shareFile(context, file, "application/pdf", "Exportar arqueo PDF")
    } catch (e: Exception) {
        fileShareService.failureFrom(e)
    }
}

fun formatSessionReportLines(
    report: com.bendey.restaurant.core.domain.cash.CashSessionReport,
    products: List<com.bendey.restaurant.core.domain.cash.CashSessionProductSold>,
    comboComponents: List<com.bendey.restaurant.core.domain.cash.CashSessionComboComponent> = emptyList(),
): List<String> {
    val session = report.session
    val lines = mutableListOf<String>()
    lines += "Reporte sesión #${session.id}"
    session.branchName?.let { lines += it }
    session.openedAt?.let { lines += "Apertura: $it" }
    session.closedAt?.let { lines += "Cierre: $it" }
    lines += "Ventas netas: ${currency.format(report.totalNetSales)}"
    if (report.totalVoidedSales > 0) lines += "Ventas anuladas: ${currency.format(report.totalVoidedSales)}"
    lines += ""
    lines += "Efectivo en caja"
    lines += "Saldo de apertura: ${currency.format(session.openingBalance)}"
    lines += "+ Ingresos en efectivo: ${currency.format(report.totalIncome)}"
    lines += "- Egresos en efectivo: ${currency.format(report.totalExpense)}"
    lines += "= Esperado en caja: ${currency.format(report.finalBalance)}"
    session.closingBalance?.let { contado ->
        lines += "Contado al cerrar: ${currency.format(contado)}"
        val dif = contado - report.finalBalance
        lines += "Diferencia: ${if (dif >= 0) "+" else "-"}${currency.format(kotlin.math.abs(dif))}"
    }
    if (report.salesByMethod.isNotEmpty()) {
        lines += ""
        lines += "Ventas por método"
        report.salesByMethod.forEach { row ->
            lines += "${salePaymentMethodLabelEs(row.method)}: ${currency.format(row.total)}"
        }
    }
    if (report.nonCashSalesByMethod.isNotEmpty()) {
        lines += ""
        lines += "Ventas no efectivo"
        report.nonCashSalesByMethod.forEach { row ->
            lines += "${salePaymentMethodLabelEs(row.method)}: ${currency.format(row.total)}"
        }
    }
    if (report.nonCashByMethod.isNotEmpty()) {
        lines += ""
        lines += "Medios electrónicos (neto = ventas − compras − egresos)"
        report.nonCashByMethod.forEach { row ->
            lines += "${salePaymentMethodLabelEs(row.method)}: ${currency.format(row.total)}"
        }
    }
    if (products.isNotEmpty()) {
        lines += ""
        lines += "Productos vendidos"
        products.forEach { p ->
            lines += "${p.quantity} x ${p.description} · ${currency.format(p.total)}"
        }
    }
    // Sin importe: la plata de estos platos ya está contada en la línea del combo.
    if (comboComponents.isNotEmpty()) {
        lines += ""
        lines += "Platos de combos (ya incluidos en el precio del combo)"
        comboComponents.forEach { c ->
            lines += "${c.quantity} x ${c.description}"
        }
    }
    return lines
}
