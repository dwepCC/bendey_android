package com.bendey.restaurant.feature.productos.reportes

enum class ReportesTab(val label: String) {
    KARDEX("Kardex"),
    VALORIZACION("Valorización"),
    CONTEO("Conteo físico"),
    PRODUCTOS("Productos"),
    VENTAS("Ventas"),
    RECETAS("Recetas"),
}

enum class SalesReportSubTab(val label: String) {
    NOTAS("Notas de venta"),
    DOCUMENTOS("Boletas / Facturas"),
}

enum class RecetasSubTab(val label: String) {
    MARGEN("Costo y ganancia"),
    STOCK("Productos con poco stock"),
}
