package com.bendey.restaurant.core.domain.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zonas de reparto (§26) y ganancias de repartidores (§27): lógica pura. */
class DeliveryZonesTest {

    @Test fun elCodigoDeDistritoSonSeisDigitos() {
        assertTrue(DeliveryZoneRules.isValidCode("080400"))
        assertFalse(DeliveryZoneRules.isValidCode("0804"))
        assertFalse(DeliveryZoneRules.isValidCode("08040a"))
        assertFalse(DeliveryZoneRules.isValidCode(""))
    }

    @Test fun losMontosAceptanComaVacioCeroYRechazanLoDemas() {
        assertEquals(0.0, DeliveryZoneRules.parseAmount("")!!, 0.0)
        assertEquals(0.0, DeliveryZoneRules.parseAmount("0")!!, 0.0)
        assertEquals(5.5, DeliveryZoneRules.parseAmount("5,5")!!, 0.0)
        assertEquals(999.99, DeliveryZoneRules.parseAmount("999.99")!!, 0.0)
        assertNull(DeliveryZoneRules.parseAmount("1000"))
        assertNull(DeliveryZoneRules.parseAmount("1.234"))
        assertNull(DeliveryZoneRules.parseAmount("abc"))
    }

    @Test fun losMinutosVanDeCeroADoscientosCuarenta() {
        assertEquals(0, DeliveryZoneRules.parseEta(""))
        assertEquals(240, DeliveryZoneRules.parseEta("240"))
        assertNull(DeliveryZoneRules.parseEta("241"))
        assertNull(DeliveryZoneRules.parseEta("-1"))
    }

    @Test fun validarDevuelveElPrimerProblemaOCero() {
        assertNull(DeliveryZoneRules.validate("080400", "Calca centro", "3.5", "0", "30"))
        assertEquals("Elige el distrito (región, provincia y distrito).", DeliveryZoneRules.validate("", "X", "0", "0", "0"))
        assertEquals("Escribe el nombre de la zona.", DeliveryZoneRules.validate("080400", "   ", "0", "0", "0"))
        assertTrue(DeliveryZoneRules.validate("080400", "X", "1000", "0", "0")!!.startsWith("La tarifa"))
        assertTrue(DeliveryZoneRules.validate("080400", "X", "0", "1000", "0")!!.startsWith("El pedido mínimo"))
        assertTrue(DeliveryZoneRules.validate("080400", "X", "0", "0", "999")!!.startsWith("Los minutos"))
    }

    @Test fun laDescripcionAclaraQueCeroEsGratisOElGeneral() {
        assertEquals("Envío gratis · mínimo general · tiempo general", DeliveryZoneRules.describe(DeliveryZone(1, "080400", "Calca")))
        assertEquals(
            "Envío S/ 4.50 · mínimo S/ 20.00 · 35 min",
            DeliveryZoneRules.describe(DeliveryZone(1, "080400", "Calca", fee = 4.5, minOrder = 20.0, etaMinutes = 35)),
        )
    }

    @Test fun repartirSoloEnZonasSinZonasActivasBloqueaATodos() {
        assertTrue(restrictWouldBlockAll(emptyList()))
        assertTrue(restrictWouldBlockAll(listOf(DeliveryZone(1, "080400", "A", active = false))))
        assertFalse(restrictWouldBlockAll(listOf(DeliveryZone(1, "080400", "A"), DeliveryZone(2, "080401", "B", active = false))))
    }

    @Test fun gananciasLasVenOchYSmPeroSoloOchPaga() {
        assertTrue(canSeeDriverEarnings(listOf("o.ch"), "cashier"))
        assertTrue(canSeeDriverEarnings(listOf("s.m"), "admin"))
        assertFalse(canSeeDriverEarnings(listOf("c.v"), "waiter"))
        assertFalse(canSeeDriverEarnings(listOf("o.ch", "s.m"), "driver"))
        assertTrue(canPayDriverEarnings(listOf("o.ch"), "cashier"))
        assertFalse(canPayDriverEarnings(listOf("s.m"), "admin"))
        assertFalse(canPayDriverEarnings(listOf("o.ch"), "driver"))
    }

    @Test fun laConfirmacionDicePorCuantoYQueSaleDeTuCaja() {
        val e = PendingEarning(3, "Luis", 4, 20.0)
        assertEquals("Pagar a Luis", DeliveryEarningsCopy.confirmTitle(e))
        assertEquals("Vas a pagar S/ 20.00 por 4 entregas. Este monto sale de TU caja abierta.", DeliveryEarningsCopy.confirmText(e))
        assertEquals("1 entrega", DeliveryEarningsCopy.deliveriesLabel(1))
    }

    @Test fun elAvisoDePagoAvisaSiNoSeAnotoEnCaja() {
        assertEquals("Pagaste S/ 20.00 a Luis por 4 entregas.", DeliveryEarningsCopy.paidMessage(EarningsPayout("Luis", 20.0, 4, true)))
        assertTrue(DeliveryEarningsCopy.paidMessage(EarningsPayout("Luis", 20.0, 4, false)).contains("revísala"))
    }
}
