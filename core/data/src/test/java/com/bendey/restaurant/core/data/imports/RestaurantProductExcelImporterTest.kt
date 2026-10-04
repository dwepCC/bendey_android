package com.bendey.restaurant.core.data.imports

import org.dhatim.fastexcel.Workbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class RestaurantProductExcelImporterTest {

    private val importer = RestaurantProductExcelImporter()
    private val areas = listOf("Cocina", "Barra")

    private fun xlsx(headers: List<String>, rows: List<List<Any>>): ByteArray {
        val out = ByteArrayOutputStream()
        Workbook(out, "test", "1.0").use { wb ->
            val sheet = wb.newWorksheet("Productos")
            headers.forEachIndexed { c, h -> sheet.value(0, c, h) }
            rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, v ->
                    if (v is Number) sheet.value(r + 1, c, v.toDouble()) else sheet.value(r + 1, c, v.toString())
                }
            }
            wb.finish()
        }
        return out.toByteArray()
    }

    private val simple = listOf("nombre", "categoría", "precio", "área")

    @Test
    fun plantillaSimpleConTildesSeEntiendeConLosAliasExistentes() {
        val result = importer.validate(
            xlsx(simple, listOf(listOf("Ceviche clásico", "Entradas", 35, "cocina"))),
            areas,
        )
        assertTrue(result.errors.isEmpty())
        val row = result.rows.single()
        assertEquals("Ceviche clásico", row.name)
        assertEquals("Entradas", row.categoryName)
        assertEquals(35.0, row.salePrice, 0.0)
        assertEquals("cocina", row.preparationArea)
        assertEquals("NIU", row.unit)
    }

    @Test
    fun soloLasFilasValidasQuedanEnRowsYLasMalasTraenMotivoYFilaOriginal() {
        val result = importer.validate(
            xlsx(
                simple,
                listOf(
                    listOf("Sopa", "Entradas", 12, "cocina"),
                    listOf("Plato sin precio", "Fondos", "", "cocina"),
                    listOf("Chicha", "Bebidas", 6, "barra"),
                    listOf("Pizza", "Fondos", 30, "horno"),
                ),
            ),
            areas,
        )
        assertEquals(listOf("Sopa", "Chicha"), result.rows.map { it.name })
        assertEquals(listOf(3, 5), result.errors.map { it.row })
        assertTrue(result.errors[1].message.contains("horno"))
        assertEquals(listOf("Plato sin precio", "Fondos", "", "cocina"), result.errors[0].rawValues)
        assertEquals(2, com.bendey.restaurant.core.domain.catalog.BulkImportReport.omittedRows(result))
    }

    @Test
    fun areaVaciaSeResuelveComoCocina() {
        val result = importer.validate(xlsx(simple, listOf(listOf("Sopa", "Entradas", 12, ""))), areas)
        assertEquals("cocina", result.rows.single().preparationArea)
    }

    @Test
    fun sinAreasDelRestauranteNoSeValidaElArea() {
        val result = importer.validate(xlsx(simple, listOf(listOf("Sopa", "Entradas", 12, "inventada"))))
        assertTrue(result.errors.isEmpty())
        assertEquals("inventada", result.rows.single().preparationArea)
    }

    @Test
    fun codigoDuplicadoEsErrorYLaFilaNoSeImporta() {
        val result = importer.validate(
            xlsx(
                listOf("nombre", "codigo", "precio_venta"),
                listOf(listOf("A", "X1", 10), listOf("B", "X1", 12)),
            ),
        )
        assertEquals(listOf("A"), result.rows.map { it.name })
        assertEquals(1, result.errors.size)
        assertEquals(3, result.errors.single().row)
    }

    @Test
    fun sinColumnasObligatoriasEsUnErrorDeEncabezados() {
        val result = importer.validate(xlsx(listOf("producto", "otra"), listOf(listOf("A", "B"))))
        assertTrue(result.rows.isEmpty())
        assertEquals("encabezados", result.errors.single().column)
    }

    @Test
    fun laPlantillaSimpleDescargadaSeValidaSinErrores() {
        val bytes = RestaurantProductTemplateExporter().generateSimpleBytes()
        val result = importer.validate(bytes, listOf("Cocina", "Barra"))
        assertEquals(3, result.rows.size)
        assertTrue(result.errors.isEmpty())
    }
}
