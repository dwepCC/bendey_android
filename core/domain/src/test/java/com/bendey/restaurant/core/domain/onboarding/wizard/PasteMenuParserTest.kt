package com.bendey.restaurant.core.domain.onboarding.wizard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TABLA DE CASOS COMPARTIDA con Tauri (pasteMenuParser.test.ts): mismas entradas, mismas salidas.
 * Formato: línea, nombre, precio, categoría, dudosa, error.
 */
class PasteMenuParserTest {

    private data class Case(
        val line: String,
        val name: String,
        val price: Double?,
        val category: String,
        val doubtful: Boolean,
        val error: String?,
    )

    private fun ok(line: String, name: String, price: Double) = Case(line, name, price, "General", false, null)
    private fun dudosa(line: String, name: String, price: Double) = Case(line, name, price, "General", true, null)

    private val cases = listOf(
        ok("Ceviche clásico 35", "Ceviche clásico", 35.0),
        ok("Lomo saltado S/ 28.50", "Lomo saltado", 28.5),
        ok("Lomo saltado S/28.50", "Lomo saltado", 28.5),
        ok("Lomo saltado s/. 28,50", "Lomo saltado", 28.5),
        ok("Chicha morada - 6", "Chicha morada", 6.0),
        ok("Chicha morada – 6", "Chicha morada", 6.0),
        ok("Chicha morada — 6", "Chicha morada", 6.0),
        ok("Chicha morada: 6", "Chicha morada", 6.0),
        ok("Chicha morada | 6", "Chicha morada", 6.0),
        ok("Chicha morada\t6", "Chicha morada", 6.0),
        ok("Arroz con pollo ..... 18", "Arroz con pollo", 18.0),
        ok("• Inca Kola 3,50", "Inca Kola", 3.5),
        ok("- Gaseosa 4", "Gaseosa", 4.0),
        ok("* Agua 2.5", "Agua", 2.5),
        ok("1) Sopa 12", "Sopa", 12.0),
        ok("2. Sopa criolla 14", "Sopa criolla", 14.0),
        ok("Ceviche mixto 35 soles", "Ceviche mixto", 35.0),
        ok("CEVICHE MIXTO 40", "Ceviche Mixto", 40.0),
        ok("  Papa   rellena    9  ", "Papa rellena", 9.0),
        // Dudosas
        dudosa("Pollo 15 / 20", "Pollo", 20.0),
        dudosa("Pollo S/ 15 / 20", "Pollo", 20.0),
        dudosa("Pollo 15 o 20", "Pollo", 20.0),
        dudosa("Pollo 10-15", "Pollo", 15.0),
        dudosa("Pollo 12 15", "Pollo 12", 15.0),
        dudosa("Agua 0", "Agua", 0.0),
        dudosa("Langosta 12000", "Langosta", 12000.0),
        dudosa("A 10", "A", 10.0),
        dudosa("Parrilla 1,500", "Parrilla", 1500.0),
        // Inválidas
        Case("Sin precio", "Sin precio", null, "General", false, "Falta el precio"),
        Case("35", "", 35.0, "General", false, "Falta el nombre"),
        Case("S/ 35", "", 35.0, "General", false, "Falta el nombre"),
    )

    @Test
    fun tablaDeCasosCompartida() {
        for (c in cases) {
            val r = parsePasteMenu(c.line)
            assertEquals(1, r.rows.size, "filas de '${c.line}'")
            val row = r.rows[0]
            assertEquals(c.name, row.name, "nombre de '${c.line}'")
            assertEquals(c.price, row.price, "precio de '${c.line}'")
            assertEquals(c.category, row.category, "categoría de '${c.line}'")
            assertEquals(c.doubtful, row.doubtful, "dudosa de '${c.line}'")
            assertEquals(c.error, row.error, "error de '${c.line}'")
        }
    }

    @Test
    fun lasDudosasLlevanAvisoYLasValidasNo() {
        assertEquals(PasteMenu.MSG_SEVERAL_PRICES, parsePasteMenu("Pollo 15 / 20").rows[0].warning)
        assertEquals(PasteMenu.MSG_ODD_PRICE, parsePasteMenu("Agua 0").rows[0].warning)
        assertEquals(PasteMenu.MSG_SHORT_NAME, parsePasteMenu("A 10").rows[0].warning)
        assertNull(parsePasteMenu("Ceviche 35").rows[0].warning)
    }

    @Test
    fun categoriaConDosPuntosCreaLaCategoriaYLasFilasSiguientesLaUsan() {
        val r = parsePasteMenu("Entradas:\nCeviche 35\nTiradito 38\nBebidas:\nChicha 6")
        assertEquals(listOf("Entradas", "Bebidas"), r.categories)
        assertEquals(
            listOf("Ceviche" to "Entradas", "Tiradito" to "Entradas", "Chicha" to "Bebidas"),
            r.rows.map { it.name to it.category },
        )
    }

    @Test
    fun unaCategoriaSinPlatosIgualQuedaEnLaLista() {
        val r = parsePasteMenu("Entradas:")
        assertEquals(listOf("Entradas"), r.categories)
        assertTrue(r.rows.isEmpty())
    }

    @Test
    fun lineaConDosPuntosPeroConDigitosNoEsCategoria() {
        val r = parsePasteMenu("Menú 2:")
        assertEquals(listOf("General"), r.categories)
        assertEquals(2.0, r.rows[0].price)
    }

    @Test
    fun sinCategoriaDeclaradaSeUsaGeneral() {
        assertEquals(listOf("General"), parsePasteMenu("Ceviche 35").categories)
    }

    @Test
    fun ignoraLineasVaciasYConSoloEspacios() {
        val r = parsePasteMenu("\n  \nCeviche 35\r\n\r\n\t\nSopa 12\n")
        assertEquals(listOf("Ceviche", "Sopa"), r.rows.map { it.name })
        assertEquals(listOf(3, 6), r.rows.map { it.line })
    }

    @Test
    fun duplicadoAvisaPeroNoEsError() {
        val r = parsePasteMenu("Ceviche 35\nCEVICHE 35")
        assertNull(r.rows[1].error)
        assertEquals(PasteMenu.MSG_DUPLICATE, r.rows[1].warning)
        assertNull(r.rows[0].warning)
    }

    @Test
    fun mismoNombreEnOtraCategoriaNoEsDuplicado() {
        val r = parsePasteMenu("Entradas:\nSopa 10\nFondos:\nSopa 10")
        assertNull(r.rows[1].warning)
    }

    @Test
    fun trescientasFilasEntranYTrescientasUnaSeRecortanConTope() {
        val ok = parsePasteMenu((1..300).joinToString("\n") { "Plato ${it}x $it" })
        assertEquals(300, ok.rows.size)
        assertFalse(ok.truncated)

        val over = parsePasteMenu((1..301).joinToString("\n") { "Plato ${it}x $it" })
        assertEquals(300, over.rows.size)
        assertTrue(over.truncated)
    }

    @Test
    fun lasCategoriasNoCuentanParaElTope() {
        val text = (listOf("Entradas:") + (0 until 300).map { "Plato ${it}x ${it + 1}" }).joinToString("\n")
        val r = parsePasteMenu(text)
        assertEquals(300, r.rows.size)
        assertFalse(r.truncated)
    }

    @Test
    fun normalizaNombreSoloSiTodoEstaEnMayusculas() {
        assertEquals("Ceviche Mixto", normalizeProductName("CEVICHE MIXTO"))
        assertEquals("Ceviche MIXTO", normalizeProductName("Ceviche MIXTO"))
        assertEquals("ají de gallina", normalizeProductName("ají de gallina"))
        assertEquals(120, normalizeProductName("a".repeat(200)).length)
    }

    @Test
    fun classifyPasteRowRevalidaUnaFilaEditada() {
        assertEquals(RowClassification(false), classifyPasteRow("Ceviche", 35.0))
        assertEquals(PasteMenu.MSG_MISSING_NAME, classifyPasteRow("", 35.0).error)
        assertEquals(PasteMenu.MSG_MISSING_PRICE, classifyPasteRow("Ceviche", null).error)
        assertTrue(classifyPasteRow("Ceviche", 0.0).doubtful)
        assertTrue(classifyPasteRow("Ceviche", 10000.0).doubtful)
    }
}
