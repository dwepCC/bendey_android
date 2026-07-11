package com.bendey.restaurant.feature.productos.reportes

enum class ReportesTab(val label: String) {
    KARDEX("Kardex"),
    PRODUCTOS("Productos"),
    VENTAS("Ventas"),
}

enum class SalesReportSubTab(val label: String) {
    NOTAS("Notas de venta"),
    DOCUMENTOS("Boletas / Facturas"),
}
