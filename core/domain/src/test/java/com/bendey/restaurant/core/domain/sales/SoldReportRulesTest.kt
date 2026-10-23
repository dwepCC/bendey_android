package com.bendey.restaurant.core.domain.sales

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoldReportRulesTest {
    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun `hoy y mes actual en Lima, elegir no tiene rango propio`() {
        val hoy = SoldReportRules.preset(SoldReportPreset.TODAY, today)!!
        assertEquals("2026-10-22", hoy.fromIso)
        assertEquals("2026-10-22", hoy.toIso)
        val mes = SoldReportRules.preset(SoldReportPreset.MONTH, today)!!
        assertEquals("2026-10-01", mes.fromIso)
        assertEquals("2026-10-22", mes.toIso)
        assertNull(SoldReportRules.preset(SoldReportPreset.CUSTOM, today))
    }

    @Test
    fun `atencion mesa o tipo de venta directa`() {
        assertEquals("Mesa 4", SoldReportRules.serviceLabel("mesa", "dine_in", "Mesa 4"))
        assertEquals("Mesa", SoldReportRules.serviceLabel("mesa", "dine_in", null))
        assertEquals("Para llevar", SoldReportRules.serviceLabel("directa", "takeaway", null))
        assertEquals("Delivery", SoldReportRules.serviceLabel("directa", "delivery", null))
        assertEquals("Venta directa", SoldReportRules.serviceLabel("directa", "quick_sale", null))
    }

    @Test
    fun `cantidades, combos y pagos`() {
        assertEquals("2", SoldReportRules.qty(2.0))
        assertEquals("1.5", SoldReportRules.qty(1.5))
        assertEquals("Combo Duo (2× Pizza, 1× Gaseosa)", SoldReportRules.itemLabel("Combo Duo", listOf("2× Pizza", "1× Gaseosa")))
        assertEquals("Lomo", SoldReportRules.itemLabel("Lomo", emptyList()))
        assertEquals("—", SoldReportRules.paymentsLabel(emptyList()))
        assertEquals("Boleta", SoldReportRules.docKindLabel("boleta"))
        assertEquals("Nota de venta", SoldReportRules.docKindLabel("nota_venta"))
    }
}
