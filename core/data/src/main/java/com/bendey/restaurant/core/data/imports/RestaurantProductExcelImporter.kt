package com.bendey.restaurant.core.data.imports

import com.bendey.restaurant.core.domain.catalog.BulkImportRow
import com.bendey.restaurant.core.domain.catalog.BulkImportRowError
import com.bendey.restaurant.core.domain.catalog.BulkImportValidationResult
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantProductExcelImporter @Inject constructor() {

    fun validate(bytes: ByteArray): BulkImportValidationResult {
        val errors = mutableListOf<BulkImportRowError>()
        val rows = mutableListOf<BulkImportRow>()
        val codesInFile = mutableMapOf<String, Int>()

        ByteArrayInputStream(bytes).use { input ->
            ReadableWorkbook(input).use { workbook ->
                val sheetRows = workbook.firstSheet.openStream().use { it.toList() }
                if (sheetRows.isEmpty()) {
                    return BulkImportValidationResult(
                        rows = emptyList(),
                        errors = listOf(BulkImportRowError(0, "archivo", "El archivo está vacío")),
                    )
                }

                val headers = (0 until sheetRows.first().cellCount).map { col ->
                    normalizeHeader(rowText(sheetRows.first(), col))
                }
                if (!headers.contains("nombre") || !headers.contains("precio_venta")) {
                    return BulkImportValidationResult(
                        rows = emptyList(),
                        errors = listOf(
                            BulkImportRowError(
                                1,
                                "encabezados",
                                "Faltan columnas obligatorias: nombre, precio_venta",
                            ),
                        ),
                    )
                }

                sheetRows.drop(1).forEachIndexed { index, row ->
                    val rowNumber = index + 2
                    val values = headers.associateWith { column ->
                        val colIndex = headers.indexOf(column)
                        rowText(row, colIndex)
                    }
                    val name = values["nombre"].orEmpty().trim()
                    if (name.isEmpty()) return@forEachIndexed

                    val price = values["precio_venta"].orEmpty().replace(",", ".").toDoubleOrNull()
                    if (price == null || price < 0.01) {
                        errors += BulkImportRowError(rowNumber, "precio_venta", "Precio de venta inválido")
                        return@forEachIndexed
                    }

                    val code = values["codigo"].orEmpty().trim()
                    if (code.isNotEmpty()) {
                        val prev = codesInFile[code]
                        if (prev != null) {
                            errors += BulkImportRowError(rowNumber, "codigo", "Código duplicado en fila $prev")
                        } else {
                            codesInFile[code] = rowNumber
                        }
                    }

                    val stockInitial = values["stock_inicial"].orEmpty().replace(",", ".").toDoubleOrNull() ?: 0.0
                    val manageStock = parseBool(values["control_stock"]) || stockInitial > 0

                    rows += BulkImportRow(
                        rowNumber = rowNumber,
                        name = name,
                        code = code,
                        description = values["descripcion"].orEmpty().trim(),
                        salePrice = price,
                        unit = values["unidad"].orEmpty().trim().uppercase().ifBlank { "NIU" },
                        categoryName = values["categoria"].orEmpty().trim(),
                        preparationArea = values["area_preparacion"].orEmpty().trim().lowercase(),
                        igvAffectationType = values["afectacion_igv"].orEmpty().trim().ifBlank { "10" },
                        priceIncludesIgv = parseBool(values["precio_incluye_igv"], default = true),
                        manageStock = manageStock,
                        initialStock = stockInitial.coerceAtLeast(0.0),
                    )
                }
            }
        }

        return BulkImportValidationResult(rows = rows, errors = errors)
    }

    private fun rowText(row: Row, columnIndex: Int): String {
        if (columnIndex < 0 || columnIndex >= row.cellCount) return ""
        val cell = row.getCell(columnIndex) ?: return ""
        return cell.rawValue?.toString()?.trim().orEmpty()
    }

    private fun normalizeHeader(value: String): String {
        val normalized = value.trim().lowercase()
        return HEADER_ALIASES[normalized] ?: normalized
    }

    private fun parseBool(value: String?, default: Boolean = false): Boolean {
        val v = value?.trim()?.lowercase().orEmpty()
        if (v.isEmpty()) return default
        return v in setOf("1", "true", "si", "sí", "yes", "y")
    }

    companion object {
        private val HEADER_ALIASES = mapOf(
            "name" to "nombre",
            "producto" to "nombre",
            "plato" to "nombre",
            "code" to "codigo",
            "sku" to "codigo",
            "description" to "descripcion",
            "precio_de_venta" to "precio_venta",
            "precio" to "precio_venta",
            "price" to "precio_venta",
            "sale_price" to "precio_venta",
            "precio_venta_soles" to "precio_venta",
            "unit" to "unidad",
            "category" to "categoria",
            "area" to "area_preparacion",
            "preparation_area" to "area_preparacion",
            "igv" to "afectacion_igv",
            "igv_affectation_type" to "afectacion_igv",
            "incluye_igv" to "precio_incluye_igv",
            "manage_stock" to "control_stock",
            "cantidad_inicial" to "stock_inicial",
            "initial_stock" to "stock_inicial",
        )
    }
}
