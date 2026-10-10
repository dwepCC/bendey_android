package com.bendey.restaurant.core.domain.delivery

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D3/D5: rangos del reporte, textos, insignia de retraso y validación de las reglas del delivery. */
class DeliveryReportTest {
    private val today = LocalDate.of(2026, 10, 9)

    @Test fun hoySiete30Dias() {
        assertEquals(ReportRange(today, today), ReportRangeRules.preset(ReportPreset.TODAY, today))
        val r7 = ReportRangeRules.preset(ReportPreset.LAST_7, today)!!
        assertEquals("2026-10-03", r7.fromIso)
        assertEquals("2026-10-09", r7.toIso)
        assertEquals(7, r7.days)
        val r30 = ReportRangeRules.preset(ReportPreset.LAST_30, today)!!
        assertEquals("2026-09-10", r30.fromIso)
        assertEquals(30, r30.days)
        assertNull(ReportRangeRules.preset(ReportPreset.CUSTOM, today))
    }

    @Test fun validaRangoPersonalizado() {
        assertNull(ReportRangeRules.validate(today.minusDays(91), today, today))
        assertNotNull(ReportRangeRules.validate(today.minusDays(92), today, today))
        assertNotNull(ReportRangeRules.validate(today, today.minusDays(1), today))
        assertNotNull(ReportRangeRules.validate(today, today.plusDays(1), today))
        assertNull(ReportRangeRules.validate(today, today, today))
    }

    @Test fun textosDelReporte() {
        assertEquals("S/ 12.50", DeliveryReportText.money(12.5))
        assertEquals("12.3 %", DeliveryReportText.percent(0.1234))
        assertEquals("Sin datos", DeliveryReportText.time(DeliveryReportTime(0.0, 0)))
        assertEquals("18 min (3 pedidos)", DeliveryReportText.time(DeliveryReportTime(18.2, 3)))
        assertEquals("12 min (1 pedido)", DeliveryReportText.time(DeliveryReportTime(12.0, 1)))
        assertEquals("Marketplace · Delivery", DeliveryReportText.channelLabel("marketplace", "delivery"))
        assertEquals("Local · Recoger", DeliveryReportText.channelLabel("local", "takeaway"))
        assertNull(DeliveryReportText.refundsNotice(DeliveryReportRefunds(0, 0.0)))
        assertEquals("2 cobros por devolver (S/ 30.00)", DeliveryReportText.refundsNotice(DeliveryReportRefunds(2, 30.0)))
        assertEquals("1 cobro por devolver (S/ 5.00)", DeliveryReportText.refundsNotice(DeliveryReportRefunds(1, 5.0)))
    }

    @Test fun permisosDelReporte() {
        assertTrue(canSeeDeliveryReport(listOf("c.v"), "cashier"))
        assertTrue(canSeeDeliveryReport(listOf("s.m"), null))
        assertFalse(canSeeDeliveryReport(listOf("o.ch"), "cashier"))
        assertFalse(canSeeDeliveryReport(listOf("c.v", "s.m"), "driver"))
        assertFalse(canSeeDeliveryReport(null, null))
    }

    private fun card(late: Int = 0, promised: String? = null) = DeliveryCard(
        sessionId = 1, assignmentId = null, source = "marketplace", customerName = "Ana", customerPhone = "", address = "",
        reference = "", itemsCount = 1, totalAmount = 10.0, orderStatus = "preparing", createdAt = null,
        promisedAt = promised, lateMinutes = late,
    )

    @Test fun insigniaDeRetraso() {
        assertEquals("Retrasado 12 min", DeliveryLateness.lateLabel(card(late = 12)))
        assertNull(DeliveryLateness.lateLabel(card(late = 0)))
        assertNull(DeliveryLateness.promisedLabel(card(late = 5, promised = "2026-10-09T19:30:00-05:00")))
    }

    @Test fun horaPrometidaEnLima() {
        assertEquals("Prometido 19:30", DeliveryLateness.promisedLabel(card(promised = "2026-10-09T19:30:00-05:00")))
        assertEquals("Prometido 19:30", DeliveryLateness.promisedLabel(card(promised = "2026-10-10T00:30:00Z")))
        assertNull(DeliveryLateness.promisedLabel(card(promised = null)))
        assertNull(DeliveryLateness.promisedLabel(card(promised = "basura")))
    }

    @Test fun validaReglasDelDelivery() {
        assertEquals(0.0, DeliveryRulesRules.parseMinOrder("0")!!, 0.0)
        assertEquals(15.5, DeliveryRulesRules.parseMinOrder("15,5")!!, 0.0)
        assertEquals(9999.99, DeliveryRulesRules.parseMinOrder("9999.99")!!, 0.0)
        assertNull(DeliveryRulesRules.parseMinOrder("10000"))
        assertNull(DeliveryRulesRules.parseMinOrder("1.234"))
        assertNull(DeliveryRulesRules.parseMinOrder(""))
        assertNull(DeliveryRulesRules.parseMinOrder("-1"))
        assertEquals(240, DeliveryRulesRules.parseEta("240"))
        assertEquals(0, DeliveryRulesRules.parseEta("0"))
        assertNull(DeliveryRulesRules.parseEta("241"))
        assertNull(DeliveryRulesRules.parseEta("1.5"))
        assertNull(DeliveryRulesRules.parseEta(""))
    }
}
