package com.bendey.restaurant.core.domain.onboarding.wizard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PastePreviewTest {

    private fun preview(text: String, existing: Set<String> = emptySet()) =
        buildPastePreview(parsePasteMenu(text), existing)

    @Test
    fun dudosasArrancanDesmarcadasYLasInvalidasNoSePuedenImportar() {
        val rows = preview("Ceviche 35\nPollo 15 / 20\nSin precio")
        assertTrue(rows[0].selected)
        assertFalse(rows[1].selected)
        assertTrue(rows[1].doubtful)
        assertFalse(rows[2].selected)
        assertEquals("Falta el precio", rows[2].error)
        assertEquals(listOf("Ceviche"), pasteImportRows(rows).map { it.name })
    }

    @Test
    fun laDudosaSoloSeImportaSiElUsuarioLaMarca() {
        val rows = preview("Pollo 15 / 20").map { it.copy(selected = true) }
        val out = pasteImportRows(rows)
        assertEquals(1, out.size)
        assertEquals(20.0, out[0].salePrice)
    }

    @Test
    fun excluyeLosPlatosQueYaExistenEnLaCarta() {
        val rows = preview("Ceviche 35\nSopa 12", existing = setOf("CEVICHE"))
        assertTrue(rows[0].alreadyExists)
        assertEquals(listOf("Sopa"), pasteImportRows(rows).map { it.name })
    }

    @Test
    fun importaSinStockSinRecetasYConAreaVacia() {
        val row = pasteImportRows(preview("Entradas:\nCeviche 35.5")).single()
        assertFalse(row.manageStock)
        assertEquals(0.0, row.initialStock)
        assertEquals("", row.preparationArea)
        assertEquals("Entradas", row.categoryName)
        assertEquals(35.5, row.salePrice)
    }

    @Test
    fun editarUnaFilaInvalidaLaCorrigeYLaMarca() {
        val bad = preview("Sin precio").single()
        val fixed = bad.edited(priceText = "18,50")
        assertNull(fixed.error)
        assertTrue(fixed.selected)
        assertEquals(18.5, fixed.price)
        assertEquals(1, pasteImportRows(listOf(fixed)).size)
    }

    @Test
    fun editarUnPrecioRaroLaDejaDudosaSinMarcar() {
        val ok = preview("Ceviche 35").single()
        val odd = ok.edited(priceText = "0")
        assertTrue(odd.doubtful)
        assertEquals(PasteMenu.MSG_ODD_PRICE, odd.warning)
    }

    @Test
    fun editarElNombreAUnoExistenteLaExcluye() {
        val row = preview("Ceviche 35").single()
        val edited = row.edited(name = "Sopa", existingNames = setOf("sopa"))
        assertTrue(edited.alreadyExists)
        assertTrue(pasteImportRows(listOf(edited)).isEmpty())
    }

    @Test
    fun mensajeDeResultado() {
        assertEquals("Creamos 10 platos.", importResultMessage(10, 0))
        assertEquals("Creamos 8 platos. 2 no se pudieron crear.", importResultMessage(8, 2))
    }

    @Test
    fun parsePriceAceptaComaYMoneda() {
        assertEquals(28.5, parsePrice("28,50"))
        assertEquals(35.0, parsePrice("S/ 35"))
        assertNull(parsePrice(""))
        assertNull(parsePrice("abc"))
    }
}
