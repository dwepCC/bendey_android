package com.bendey.restaurant.feature.productos.reportes

import com.bendey.restaurant.core.domain.locale.LabelKey

enum class ReportesTab(val label: String) {
    KARDEX("Kardex"),
    VALORIZACION("Valorización"),
    CONTEO("Conteo físico"),
    PRODUCTOS("Productos"),
    VENTAS("Ventas"),
    VENDIDOS("Vendidos"),
    RECETAS("Recetas"),
}

enum class SalesReportSubTab(private val peLabel: String) {
    NOTAS("Notas de venta"),
    DOCUMENTOS("Boletas / Facturas"),
    ;

    /** Texto de la pestaña; «Boletas / Facturas» se arma con las etiquetas del país (I5). */
    val label: String
        get() = when (this) {
            DOCUMENTOS -> "${LabelKey.DOC_RECEIPT.text}s / ${LabelKey.DOC_INVOICE.text}s"
            else -> peLabel
        }
}

enum class RecetasSubTab(val label: String) {
    MARGEN("Costo y ganancia"),
    STOCK("Productos con poco stock"),
}
