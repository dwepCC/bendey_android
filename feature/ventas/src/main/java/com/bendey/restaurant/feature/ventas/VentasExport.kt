package com.bendey.restaurant.feature.ventas

import android.content.Context
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.data.export.exportSalesListCsv
import com.bendey.restaurant.core.data.export.exportSalesListPdf
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.VentasTab

fun exportSalesListCsv(
    context: Context,
    fileShareService: BendeyFileShareService,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
): ExportShareResult = com.bendey.restaurant.core.data.export.exportSalesListCsv(
    context = context,
    fileShareService = fileShareService,
    tab = tab,
    sales = sales,
    from = from,
    to = to,
    reportStyle = false,
)

fun exportSalesListPdf(
    context: Context,
    fileShareService: BendeyFileShareService,
    tab: VentasTab,
    sales: List<SaleSummary>,
    from: String,
    to: String,
): ExportShareResult = com.bendey.restaurant.core.data.export.exportSalesListPdf(
    context = context,
    fileShareService = fileShareService,
    tab = tab,
    sales = sales,
    from = from,
    to = to,
    reportStyle = false,
)

fun shareBillingDocumentFile(
    context: Context,
    fileShareService: BendeyFileShareService,
    file: java.io.File,
    mimeType: String,
    title: String,
): ExportShareResult = fileShareService.shareFile(context, file, mimeType, title)
