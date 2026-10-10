package com.bendey.restaurant.core.domain.ops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpsRulesTest {

    // ---- Control de descuentos ----

    @Test fun sinDatoNoBloquea() = assertFalse(OpsControlsRules.discountsLocked(null))

    @Test fun controlApagadoNuncaBloquea() {
        assertFalse(OpsControlsRules.discountsLocked(OpsControls(discountControl = false, canDiscount = false)))
    }

    @Test fun controlEncendidoSinPermisoBloquea() {
        assertTrue(OpsControlsRules.discountsLocked(OpsControls(discountControl = true, canDiscount = false)))
    }

    @Test fun controlEncendidoConPermisoNoBloquea() {
        assertFalse(OpsControlsRules.discountsLocked(OpsControls(discountControl = true, canDiscount = true)))
    }

    @Test fun soloSmEditaElControl() {
        assertTrue(OpsControlsRules.canEdit(listOf("s.m")))
        assertFalse(OpsControlsRules.canEdit(listOf("o.ch")))
        assertFalse(OpsControlsRules.canEdit(null))
    }

    // ---- Reabrir ----

    @Test fun reabrirSoloSiAnuladaConPedidoYPuedeCobrar() {
        assertTrue(ReopenRules.isOffered(cancelled = true, restaurantSessionId = 7, canCharge = true))
        assertFalse(ReopenRules.isOffered(cancelled = false, restaurantSessionId = 7, canCharge = true))
        assertFalse(ReopenRules.isOffered(cancelled = true, restaurantSessionId = null, canCharge = true))
        assertFalse(ReopenRules.isOffered(cancelled = true, restaurantSessionId = 0, canCharge = true))
        assertFalse(ReopenRules.isOffered(cancelled = true, restaurantSessionId = 7, canCharge = false))
    }

    @Test fun reabrirValidaMotivoYPin() {
        assertNotNull(ReopenRules.validate("ab", "1234"))
        assertNotNull(ReopenRules.validate("  a ", "1234"))
        assertNotNull(ReopenRules.validate("cliente mal", ""))
        assertNull(ReopenRules.validate("cliente mal", "1234"))
    }

    // ---- Platos servidos ----

    @Test fun soloEseCodigoPideConfirmacion() {
        assertTrue(ServedItemsRules.needsConfirmation("SESSION_HAS_SERVED_ITEMS"))
        assertFalse(ServedItemsRules.needsConfirmation("SESSION_NOT_OPEN"))
        assertFalse(ServedItemsRules.needsConfirmation(null))
    }

    // ---- Horario de atención ----

    @Test fun parseaHoraEstricta() {
        assertEquals(0, BusinessHoursRules.parseHm("00:00"))
        assertEquals(23 * 60 + 59, BusinessHoursRules.parseHm("23:59"))
        assertNull(BusinessHoursRules.parseHm("24:00"))
        assertNull(BusinessHoursRules.parseHm("9:00"))
        assertNull(BusinessHoursRules.parseHm("09:60"))
        assertNull(BusinessHoursRules.parseHm("ab:cd"))
        assertNull(BusinessHoursRules.parseHm(""))
    }

    @Test fun aperturaIgualAlCierreEsInvalida() {
        assertNotNull(BusinessHoursRules.validateSlot(SlotDraft("11:00", "11:00")))
        assertNotNull(BusinessHoursRules.validateSlot(SlotDraft("", "11:00")))
        assertNull(BusinessHoursRules.validateSlot(SlotDraft("11:00", "22:00")))
    }

    @Test fun cierreMenorCruzaLaMedianocheYSeExplica() {
        val slot = SlotDraft("18:00", "02:00")
        assertNull(BusinessHoursRules.validateSlot(slot))
        assertTrue(BusinessHoursRules.crossesMidnight(slot))
        assertNotNull(BusinessHoursRules.midnightExplanation(slot))
        assertFalse(BusinessHoursRules.crossesMidnight(SlotDraft("11:00", "22:00")))
        assertNull(BusinessHoursRules.midnightExplanation(SlotDraft("11:00", "22:00")))
    }

    @Test fun diaAbiertoSinTramosNoSeGuarda() {
        val week = BusinessHoursRules.fromIntervals(emptyList()) + (1 to DayDraft(open = true, slots = emptyList()))
        assertNotNull(BusinessHoursRules.validateWeek(week))
    }

    @Test fun maximoCuatroTramosPorDia() {
        var week = BusinessHoursRules.setDayOpen(BusinessHoursRules.fromIntervals(emptyList()), 3, true)
        repeat(10) { week = BusinessHoursRules.addSlot(week, 3) }
        assertEquals(BusinessHoursRules.MAX_PER_DAY, week[3]!!.slots.size)
        assertNull(BusinessHoursRules.validateWeek(week))
    }

    @Test fun encenderUnDiaPoneUnTramoPorDefecto() {
        val week = BusinessHoursRules.setDayOpen(BusinessHoursRules.fromIntervals(emptyList()), 2, true)
        assertEquals(listOf(SlotDraft("11:00", "22:00")), week[2]!!.slots)
        assertEquals(listOf(BusinessInterval(2, "11:00", "22:00")), BusinessHoursRules.toIntervals(week))
    }

    @Test fun apagarUnDiaLoQuitaDelPut() {
        var week = BusinessHoursRules.setDayOpen(BusinessHoursRules.fromIntervals(emptyList()), 2, true)
        week = BusinessHoursRules.setDayOpen(week, 2, false)
        assertTrue(BusinessHoursRules.toIntervals(week).isEmpty())
    }

    @Test fun copiarATodosReplicaLosTramos() {
        var week = BusinessHoursRules.setDayOpen(BusinessHoursRules.fromIntervals(emptyList()), 1, true)
        week = BusinessHoursRules.updateSlot(week, 1, 0, SlotDraft("08:00", "15:00"))
        week = BusinessHoursRules.addSlot(week, 1)
        val all = BusinessHoursRules.copyToAll(week, 1)
        assertEquals(14, BusinessHoursRules.toIntervals(all).size)
        assertTrue((1..7).all { all[it]!!.open && all[it]!!.slots == week[1]!!.slots })
    }

    @Test fun ida_y_vuelta_de_tramos_guardados() {
        val saved = listOf(
            BusinessInterval(1, "18:00", "02:00"),
            BusinessInterval(1, "08:00", "12:00"),
            BusinessInterval(7, "10:00", "20:00"),
        )
        val week = BusinessHoursRules.fromIntervals(saved)
        assertTrue(week[1]!!.open)
        assertFalse(week[2]!!.open)
        // ordenados por apertura, como los devuelve el servidor
        assertEquals(listOf("08:00", "18:00"), week[1]!!.slots.map { it.openAt })
        assertFalse(BusinessHoursRules.isDirty(week, saved))
        assertTrue(BusinessHoursRules.isDirty(BusinessHoursRules.setDayOpen(week, 2, true), saved))
    }

    @Test fun mensajeDeEstado() {
        assertEquals(BusinessHoursRules.ALWAYS_OPEN_NOTICE, BusinessHoursStatus(configured = false).summary)
        assertEquals("Abierto ahora", BusinessHoursStatus(configured = true, open = true).summary)
        assertEquals(
            "El restaurante está cerrado por ahora. Abre mañana a las 11:00.",
            BusinessHoursStatus(configured = true, open = false, message = "El restaurante está cerrado por ahora. Abre mañana a las 11:00.").summary,
        )
    }
}
