package com.bendey.restaurant.core.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2.0: la tarifa de delivery queda fuera de la base del descuento y del RC y se suma despues. */
class CheckoutDeliveryFeeTest {
    private val pct = CheckoutDiscountMode.PERCENT
    private val amt = CheckoutDiscountMode.AMOUNT

    @Test fun descuentoPorcentajeSoloSobreLosItems() {
        // items 50 + tarifa 5 = 55; 10% de 50 = 5 -> 50
        assertEquals(5.0, calcCheckoutDiscountAmount(55.0, pct, 10.0, 5.0), 0.001)
        assertEquals(50.0, calcPayableTotal(55.0, pct, 10.0, 5.0), 0.001)
    }

    @Test fun descuentoMontoFijoNoPasaDeLosItems() {
        assertEquals(8.0, calcCheckoutDiscountAmount(55.0, amt, 8.0, 5.0), 0.001)
        assertEquals(47.0, calcPayableTotal(55.0, amt, 8.0, 5.0), 0.001)
        assertEquals(50.0, calcCheckoutDiscountAmount(55.0, amt, 999.0, 5.0), 0.001)
        assertEquals(5.0, calcPayableTotal(55.0, amt, 999.0, 5.0), 0.001)
    }

    @Test fun recargoSobreLosItemsSinLaTarifa() {
        // items 100 + tarifa 10, RC 10%, IGV 18%: base RC = 100/1.18*10%
        val rc = 100.0 / 1.18 * 0.10
        val total = calcPayableTotalWithServiceCharge(110.0, pct, 0.0, 10.0, true, 18.0, nonDiscountableAmount = 10.0)
        assertEquals(110.0 + Math.round(rc * 100) / 100.0, total, 0.011)
    }

    @Test fun descuentoYRecargoJuntos() {
        // items 100, tarifa 10, desc 10% = 10 -> items netos 90; RC 10% sobre 90/1.18
        val rc = Math.round(90.0 / 1.18 * 0.10 * 100) / 100.0
        val total = calcPayableTotalWithServiceCharge(110.0, pct, 10.0, 10.0, true, 18.0, nonDiscountableAmount = 10.0)
        assertEquals(90.0 + 10.0 + rc, total, 0.011)
    }

    @Test fun regresionSinTarifaIdenticoAlAnterior() {
        for (raw in listOf(0.0, 12.5, 100.0)) for (v in listOf(0.0, 10.0, 200.0)) for (m in listOf(pct, amt)) {
            assertEquals(calcCheckoutDiscountAmount(raw, m, v), calcCheckoutDiscountAmount(raw, m, v, 0.0), 0.0)
            assertEquals(calcPayableTotal(raw, m, v), calcPayableTotal(raw, m, v, 0.0), 0.0)
            assertEquals(
                calcPayableTotalWithServiceCharge(raw, m, v, 5.0, true, 18.0),
                calcPayableTotalWithServiceCharge(raw, m, v, 5.0, true, 18.0, 0.0), 0.0,
            )
        }
    }

    @Test fun nuncaNegativoNiMenorQueLaTarifa() {
        for (v in listOf(0.0, 50.0, 100.0, 500.0)) for (m in listOf(pct, amt)) {
            val t = calcPayableTotalWithServiceCharge(55.0, m, v, 5.0, true, 18.0, nonDiscountableAmount = 5.0)
            assertTrue("v=$v m=$m t=$t", t >= 5.0)
        }
        // tarifa mayor que el total (datos raros): no explota ni da negativo
        assertTrue(calcPayableTotal(3.0, pct, 50.0, 5.0) >= 0.0)
    }
}
