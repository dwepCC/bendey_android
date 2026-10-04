package com.bendey.restaurant.core.domain.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BulkImportReportTest {

    private fun row(n: Int) = BulkImportRow(n, "P$n", "", "", 10.0, "NIU", "", "", "10", true, false, 0.0)

    private val validation = BulkImportValidationResult(
        rows = listOf(row(2), row(4)),
        errors = listOf(
            BulkImportRowError(3, "precio_venta", "Precio de venta inválido", listOf("Sin, precio", "Fondos", "")),
            BulkImportRowError(5, "area_preparacion", "El área \"x\" no existe", listOf("Pizza", "Fondos", "30")),
        ),
        headers = listOf("nombre", "categoria", "precio_venta"),
    )

    @Test
    fun resumenYBotonHablanDeLasFilasValidas() {
        assertEquals("Se importarán 2 platos. Se omiten 2 filas con errores.", BulkImportReport.summary(validation))
        assertEquals("Importar solo las filas válidas (2)", BulkImportReport.importButtonLabel(validation))
        assertEquals("Importar (2)", BulkImportReport.importButtonLabel(validation.copy(errors = emptyList())))
    }

    @Test
    fun elCsvDeErroresTraeTodasLasFilasSinTruncarYUnaColumnaDeMotivo() {
        val many = validation.copy(
            errors = (1..100).map { BulkImportRowError(it + 1, "precio_venta", "malo $it", listOf("x$it")) },
            headers = listOf("nombre"),
        )
        val lines = BulkImportReport.errorsCsv(many).trimEnd().split("\r\n")
        assertEquals(101, lines.size)
        assertTrue(lines[0].endsWith("fila,columna,motivo,nombre"))
    }

    @Test
    fun elCsvEscapaComasComillasYFormulas() {
        val csv = BulkImportReport.errorsCsv(validation)
        assertTrue(csv.startsWith("﻿"))
        assertTrue(csv.contains("\"Sin, precio\""))
        assertTrue(csv.contains("\"El área \"\"x\"\" no existe\""))
        val formula = BulkImportReport.failuresCsv(listOf(BulkImportRowError(2, "import", "=SUMA(1)")))
        assertTrue(formula.contains("'=SUMA(1)"))
    }
}
