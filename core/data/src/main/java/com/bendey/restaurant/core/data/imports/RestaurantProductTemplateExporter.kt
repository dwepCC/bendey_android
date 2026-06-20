package com.bendey.restaurant.core.data.imports

import org.dhatim.fastexcel.Workbook
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantProductTemplateExporter @Inject constructor() {

    fun generateBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        Workbook(output, "Bendey Restaurante", "1.0").use { workbook ->
            val sheet = workbook.newWorksheet("Productos")
            TEMPLATE_COLUMNS.forEachIndexed { column, header ->
                sheet.value(0, column, header)
            }
            EXAMPLE_ROW.forEachIndexed { column, value ->
                when (value) {
                    is String -> sheet.value(1, column, value)
                    is Number -> sheet.value(1, column, value.toDouble())
                    else -> sheet.value(1, column, value.toString())
                }
            }
            workbook.finish()
        }
        return output.toByteArray()
    }

    companion object {
        private val TEMPLATE_COLUMNS = listOf(
            "nombre",
            "codigo",
            "descripcion",
            "precio_venta",
            "unidad",
            "categoria",
            "area_preparacion",
            "afectacion_igv",
            "precio_incluye_igv",
            "control_stock",
            "stock_inicial",
        )

        private val EXAMPLE_ROW: List<Any> = listOf(
            "Lomo saltado",
            "7750123456789",
            "Plato de fondo",
            28.5,
            "NIU",
            "Platos de fondo",
            "cocina",
            "10",
            "si",
            "si",
            12,
        )
    }
}
