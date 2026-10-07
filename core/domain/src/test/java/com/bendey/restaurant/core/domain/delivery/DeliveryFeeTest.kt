package com.bendey.restaurant.core.domain.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2.0: reglas puras de la tarifa de delivery (validación, permisos, hoja de asignar, línea del servidor, caché). */
class DeliveryFeeTest {

    // ---- monto ----

    @Test fun parseaMontosValidosConPuntoOComa() {
        assertEquals(5.0, parseDeliveryFeeInput("5")!!, 0.0)
        assertEquals(5.5, parseDeliveryFeeInput("5,5")!!, 0.0)
        assertEquals(999.99, parseDeliveryFeeInput("999.99")!!, 0.0)
        assertEquals(0.0, parseDeliveryFeeInput("0")!!, 0.0)
        assertEquals(7.25, parseDeliveryFeeInput(" 7.25 ")!!, 0.0)
    }

    @Test fun rechazaMontosFueraDeRangoOMalFormados() {
        assertNull(parseDeliveryFeeInput(""))
        assertNull(parseDeliveryFeeInput("1000"))
        assertNull(parseDeliveryFeeInput("999.995"))
        assertNull(parseDeliveryFeeInput("5.123"))
        assertNull(parseDeliveryFeeInput("-1"))
        assertNull(parseDeliveryFeeInput("abc"))
        assertNull(parseDeliveryFeeInput("."))
        assertNull(parseDeliveryFeeInput("1.2.3"))
    }

    // ---- formulario de ajustes ----

    @Test fun noSeEnciendeConMontoCeroOVacio() {
        for (amount in listOf("", "0", "0.00")) {
            val r = validateDeliveryFeeForm(true, amount, "10")
            assertTrue(amount, r is DeliveryFeeFormResult.Invalid)
            assertEquals(DeliveryFeeCopy.AMOUNT_REQUIRED_TO_ENABLE, (r as DeliveryFeeFormResult.Invalid).message)
        }
    }

    @Test fun apagadoConMontoCeroSeGuardaComoApagado() {
        val r = validateDeliveryFeeForm(false, "", "10") as DeliveryFeeFormResult.Ok
        assertEquals(DeliverySettingsUpdate(feeEnabled = false, deliveryFee = 0.0, feeIgvAffectation = "10"), r.update)
    }

    @Test fun encendidoConMontoValidoArmaElCuerpoDelPut() {
        val r = validateDeliveryFeeForm(true, "5,5", "20") as DeliveryFeeFormResult.Ok
        assertEquals(DeliverySettingsUpdate(feeEnabled = true, deliveryFee = 5.5, feeIgvAffectation = "20"), r.update)
    }

    @Test fun montoFueraDeRangoYAfectacionDesconocidaSonInvalidos() {
        assertTrue(validateDeliveryFeeForm(true, "1000", "10") is DeliveryFeeFormResult.Invalid)
        assertTrue(validateDeliveryFeeForm(false, "x", "10") is DeliveryFeeFormResult.Invalid)
        assertTrue(validateDeliveryFeeForm(true, "5", "40") is DeliveryFeeFormResult.Invalid)
        assertEquals(
            DeliveryFeeCopy.INVALID_AMOUNT,
            (validateDeliveryFeeForm(true, "1000", "10") as DeliveryFeeFormResult.Invalid).message,
        )
    }

    @Test fun lasTresAfectacionesTienenSuEtiqueta() {
        assertEquals("Gravado con IGV", DeliveryFeeRules.affectationLabel("10"))
        assertEquals("Exonerado", DeliveryFeeRules.affectationLabel("20"))
        assertEquals("Inafecto", DeliveryFeeRules.affectationLabel("30"))
        assertEquals("Gravado con IGV", DeliveryFeeRules.affectationLabel(null))
    }

    // ---- permisos ----

    @Test fun laTarifaDeUnPedidoSeEditaConCajaOAdminPeroNoConDU() {
        assertTrue(canEditDeliveryFee(listOf("o.ch")))
        assertTrue(canEditDeliveryFee(listOf("s.m")))
        assertFalse(canEditDeliveryFee(listOf("d.v", "d.u")))
        assertFalse(canEditDeliveryFee(listOf("p.u", "t.o")))
        assertFalse(canEditDeliveryFee(null))
    }

    @Test fun losAjustesSoloLosCambiaElAdmin() {
        assertTrue(canManageDeliverySettings(listOf("s.m")))
        assertFalse(canManageDeliverySettings(listOf("o.ch")))
    }

    // ---- hoja de asignar ----

    @Test fun elCampoSeMuestraSiLaTarifaEstaEncendidaOElPedidoYaTieneUna() {
        assertFalse(showsAssignFeeField(null, null))
        assertFalse(showsAssignFeeField(DeliverySettings(feeEnabled = false, deliveryFee = 5.0), null))
        assertTrue(showsAssignFeeField(DeliverySettings(feeEnabled = true, deliveryFee = 5.0), null))
        assertTrue(showsAssignFeeField(DeliverySettings(feeEnabled = false), 7.5))
        assertFalse(showsAssignFeeField(DeliverySettings(feeEnabled = false), 0.0))
    }

    @Test fun prellenaConLaTarifaDelPedidoOLaDeAjustes() {
        val s = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)
        assertEquals("7.50", assignFeeInitialText(7.5, s))
        assertEquals("5.00", assignFeeInitialText(null, s))
        assertEquals("", assignFeeInitialText(null, null))
    }

    @Test fun soloEnviaLaTarifaSiCambio() {
        assertNull(assignFeeToSend("5.00", "5.00"))
        assertNull("mismo valor escrito distinto", assignFeeToSend("5.00", "5"))
        assertEquals(7.5, assignFeeToSend("5.00", "7.5")!!, 0.0)
        assertEquals("vaciar el campo quita la tarifa", 0.0, assignFeeToSend("5.00", "")!!, 0.0)
        assertNull("vacío sin cambio", assignFeeToSend("", ""))
        assertNull("ilegible no se envía", assignFeeToSend("5.00", "abc"))
        assertEquals(3.0, assignFeeToSend("", "3")!!, 0.0)
    }

    @Test fun elTextoDelCampoVacioCuentaComoValido() {
        assertTrue(isValidAssignFeeText(""))
        assertTrue(isValidAssignFeeText("5.50"))
        assertFalse(isValidAssignFeeText("1000"))
    }

    // ---- línea del servidor ----

    @Test fun detectaLaLineaServicioDeDelivery() {
        assertTrue(isDeliveryFeeLine("DELIVERY", "Servicio de delivery"))
        assertTrue(isDeliveryFeeLine("delivery", null))
        assertTrue(isDeliveryFeeLine(null, "Servicio de delivery"))
        assertTrue(isDeliveryFeeLine("", "  servicio de delivery "))
        assertFalse(isDeliveryFeeLine("P1", "Servicio de delivery"))
        assertFalse(isDeliveryFeeLine("P1", "Ceviche"))
        assertFalse(isDeliveryFeeLine(null, null))
    }

    // ---- caché compartida ----

    @Test fun laCacheVenceInvalidaYGuarda() {
        var now = 1_000L
        val cache = DeliverySettingsCache(ttlMs = 100, clock = { now })
        assertNull(cache.fresh())
        val s = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)
        cache.put(s)
        assertSame(s, cache.fresh())
        assertSame(s, cache.value.value)
        now += 101
        assertNull("venció", cache.fresh())
        assertSame("el último valor sigue visible", s, cache.value.value)
        cache.put(s); cache.invalidate()
        assertNull(cache.fresh())
        assertNull(cache.value.value)
    }
}
