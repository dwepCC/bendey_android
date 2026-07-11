package com.bendey.restaurant.feature.productos.reportes

import android.content.Context
import com.bendey.restaurant.core.data.export.BendeyExportPaths
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.export.ReportCsvWriter
import com.bendey.restaurant.core.data.export.ReportPdfWriter
import com.bendey.restaurant.core.domain.inventory.StockMovementItem
import com.bendey.restaurant.core.domain.products.ProductReportItem
import com.bendey.restaurant.core.domain.sales.SalesByProductRow
import java.util.Locale

private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun fmtQty(value: Double): String = String.format(Locale.US, "%.3f", value)

private fun movementTypeLabel(type: String): String = when (type.lowercase(Locale.ROOT)) {
    "in" -> "Entrada"
    "out" -> "Salida"
    "adjustment_in" -> "Ajuste (+)"
    "adjustment_out" -> "Ajuste (−)"
    "transfer" -> "Transferencia"
    else -> type
}

fun exportKardexCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<StockMovementItem>,
    from: String,
    to: String,
): ExportShareResult = try {
    val data = mutableListOf<List<String>>()
    data += listOf("Fecha", "Código", "Producto", "Tipo", "Cantidad", "Saldo", "Sucursal", "Usuario", "Referencia", "Notas")
    rows.forEach { row ->
        data += listOf(
            row.createdAt,
            row.productCode.orEmpty(),
            row.productName.orEmpty(),
            movementTypeLabel(row.type),
            fmtQty(row.quantity),
            row.balance?.let { fmtQty(it) }.orEmpty(),
            row.branchName.orEmpty(),
            row.userName.orEmpty(),
            row.reference.orEmpty(),
            row.notes.orEmpty(),
        )
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/reporte-kardex-$from-$to.csv")
    ReportCsvWriter.write(file, data)
    fileShareService.shareFile(context, file, "text/csv", "Exportar kardex Excel")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}

fun exportKardexPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<StockMovementItem>,
    from: String,
    to: String,
): ExportShareResult = try {
    val title = "Reporte de kardex · $from — $to"
    val lines = rows.map { row ->
        "${row.createdAt.take(16)} | ${row.productCode.orEmpty().take(10)} | ${row.productName.orEmpty().take(18)} | ${movementTypeLabel(row.type)} | ${fmtQty(row.quantity)}"
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/reporte-kardex-$from-$to.pdf")
    ReportPdfWriter.writePortraitA4(file, title, lines)
    fileShareService.shareFile(context, file, "application/pdf", "Exportar kardex PDF")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}

fun exportProductsReportCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<ProductReportItem>,
): ExportShareResult = try {
    val data = mutableListOf<List<String>>()
    data += listOf("Código", "Nombre", "Categoría", "P. venta", "Stock mín.", "Stock total", "Activo")
    rows.forEach { row ->
        data += listOf(
            row.code,
            row.name,
            row.categoryName.orEmpty(),
            fmt2(row.salePrice),
            if (row.manageStock) fmtQty(row.minStock) else "—",
            if (row.manageStock) fmtQty(row.stockTotal) else "—",
            if (row.active) "Sí" else "No",
        )
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/reporte-productos.csv")
    ReportCsvWriter.write(file, data)
    fileShareService.shareFile(context, file, "text/csv", "Exportar productos Excel")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}

fun exportProductsReportPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<ProductReportItem>,
): ExportShareResult = try {
    val lines = rows.map { row ->
        "${row.code.take(12)} | ${row.name.take(22)} | ${row.categoryName.orEmpty().take(12)} | S/ ${fmt2(row.salePrice)} | ${if (row.manageStock) fmtQty(row.stockTotal) else "—"}"
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/reporte-productos.pdf")
    ReportPdfWriter.writePortraitA4(file, "Reporte de productos", lines)
    fileShareService.shareFile(context, file, "application/pdf", "Exportar productos PDF")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}

fun exportSalesByProductCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<SalesByProductRow>,
    from: String,
    to: String,
): ExportShareResult = try {
    val data = mutableListOf<List<String>>()
    data += listOf("Categoría", "Código", "Producto", "Cantidad", "Total (S/)", "Comprobantes")
    rows.forEach { row ->
        data += listOf(
            row.categoryName,
            row.productCode,
            row.productName,
            fmtQty(row.quantitySold),
            fmt2(row.totalAmount),
            row.salesCount.toString(),
        )
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/ventas-por-producto-$from-$to.csv")
    ReportCsvWriter.write(file, data)
    fileShareService.shareFile(context, file, "text/csv", "Exportar ventas Excel")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}

fun exportSalesByProductPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    rows: List<SalesByProductRow>,
    from: String,
    to: String,
): ExportShareResult = try {
    val lines = rows.map { row ->
        "${row.productCode.take(10)} | ${row.productName.take(20)} | ${fmtQty(row.quantitySold)} | S/ ${fmt2(row.totalAmount)}"
    }
    val file = BendeyExportPaths.exportFile(context, "${BendeyExportPaths.EXPORTS}/ventas-por-producto-$from-$to.pdf")
    ReportPdfWriter.writePortraitA4(file, "Ventas por producto · $from — $to", lines)
    fileShareService.shareFile(context, file, "application/pdf", "Exportar ventas PDF")
} catch (e: Exception) {
    fileShareService.failureFrom(e)
}
