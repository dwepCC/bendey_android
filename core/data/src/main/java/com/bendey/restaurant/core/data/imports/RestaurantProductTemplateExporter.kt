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

    /** Plantilla SIMPLE (R4): 4 columnas con los alias que ya entiende el lector. */
    fun generateSimpleBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        Workbook(output, "Bendey Restaurante", "1.0").use { workbook ->
            val sheet = workbook.newWorksheet("Productos")
            SIMPLE_COLUMNS.forEachIndexed { column, header -> sheet.value(0, column, header) }
            SIMPLE_EXAMPLES.forEachIndexed { index, example ->
                example.forEachIndexed { column, value ->
                    when (value) {
                        is Number -> sheet.value(index + 1, column, value.toDouble())
                        else -> sheet.value(index + 1, column, value.toString())
                    }
                }
            }
            val help = workbook.newWorksheet("Instrucciones")
            SIMPLE_HELP.forEachIndexed { row, line -> help.value(row, 0, line) }
            workbook.finish()
        }
        return output.toByteArray()
    }

    companion object {
        val SIMPLE_COLUMNS = listOf("nombre", "categoría", "precio", "área")

        private val SIMPLE_EXAMPLES: List<List<Any>> = listOf(
            listOf("Ceviche clásico", "Entradas", 35, "cocina"),
            listOf("Lomo saltado", "Fondos", 28.5, "cocina"),
            listOf("Chicha morada", "Bebidas", 6, "barra"),
        )

        private val SIMPLE_HELP = listOf(
            "Cómo usar esta plantilla",
            "",
            "1. Una fila por plato: nombre, categoría, precio y área.",
            "2. La categoría se crea sola si no existe.",
            "3. El área (cocina, barra…) debe existir en tu restaurante. Si la dejas vacía, el plato va a Cocina.",
            "4. Borra las filas de ejemplo antes de subir el archivo.",
        )

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
