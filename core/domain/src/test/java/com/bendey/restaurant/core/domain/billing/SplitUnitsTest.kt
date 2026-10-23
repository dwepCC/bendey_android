package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitUnitsTest {
    private fun line(id: Int, qty: Double): SessionComandaSummary = SessionComandaSummary(
        id = id, productName = "Lomo", quantity = qty, unitPrice = 30.0, status = ComandaStatus.entries.first(), notes = null,
    )

    @Test
    fun `solo se parte una línea con 2 o más unidades enteras`() {
        assertTrue(SplitUnits.canSplit(line(1, 3.0)))
        assertFalse(SplitUnits.canSplit(line(1, 1.0)))
        assertFalse(SplitUnits.canSplit(line(1, 1.5)))
    }

    @Test
    fun `cantidad elegida o línea completa, y qué viaja`() {
        val lomo = line(7, 3.0)
        val gaseosa = line(8, 2.0)
        assertEquals(3.0, SplitUnits.billQuantity(lomo, emptyMap()), 0.0)
        assertEquals(1.0, SplitUnits.billQuantity(lomo, mapOf(7 to 1)), 0.0)
        assertEquals(3.0, SplitUnits.billQuantity(lomo, mapOf(7 to 3)), 0.0)
        assertEquals(listOf(ComandaQuantity(7, 1)), SplitUnits.payload(listOf(lomo, gaseosa), mapOf(7 to 1)))
        assertTrue(SplitUnits.hasPartial(listOf(lomo, gaseosa), mapOf(7 to 1)))
        assertFalse(SplitUnits.hasPartial(listOf(lomo, gaseosa), emptyMap()))
    }

    @Test
    fun `llegar a la cantidad completa saca la línea del mapa`() {
        val lomo = line(7, 3.0)
        assertEquals(mapOf(7 to 2), SplitUnits.set(emptyMap(), lomo, 2))
        assertEquals(emptyMap<Int, Int>(), SplitUnits.set(mapOf(7 to 2), lomo, 3))
        assertEquals("3", SplitUnits.qtyLabel(3.0))
        assertEquals("2.5", SplitUnits.qtyLabel(2.5))
    }

    @Test
    fun `el total cobra solo las unidades elegidas`() {
        val lomo = line(7, 3.0)
        assertEquals(30.0, sumComandasPayableTotal(listOf(lomo), 18.0, TaxConfig(), mapOf(7 to 1)), 0.001)
        assertEquals(90.0, sumComandasPayableTotal(listOf(lomo), 18.0, TaxConfig()), 0.001)
    }
}
