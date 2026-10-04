package com.bendey.restaurant.core.data.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Una ronda solo se marca impresa si salió en papel; si no, el mozo debe enterarse. */
class ComandaPrintFeedbackTest {

    @Test
    fun impresaMarcaYAvisaCorto() {
        val f = comandaPrintFeedback(ComandaPrintOutcome.Printed, 7)
        assertEquals("Comanda #7 enviada", f.snack)
        assertNull(f.alert)
        assertTrue(f.markPrinted)
    }

    @Test
    fun fallidaNoMarcaYOfreceReimprimir() {
        val f = comandaPrintFeedback(ComandaPrintOutcome.Failed, 7)
        assertFalse(f.markPrinted)
        assertNotNull(f.alert)
        assertTrue(f.canReprint)
        assertNull(f.snack)
    }

    @Test
    fun sinImpresoraNoMarcaYNoOfreceReimprimir() {
        val f = comandaPrintFeedback(ComandaPrintOutcome.NotConfigured, 7)
        assertFalse(f.markPrinted)
        assertNotNull(f.alert)
        assertFalse(f.canReprint)
    }

    @Test
    fun impresionAutomaticaApagadaNoEsFalloNiSeMarca() {
        val f = comandaPrintFeedback(ComandaPrintOutcome.AutoPrintOff, 7)
        assertNull(f.alert)
        assertFalse(f.markPrinted)
        assertNotNull(f.snack)
    }

    @Test
    fun nuncaSeMarcaImpresaSalvoCuandoSeImprimio() {
        ComandaPrintOutcome.entries.filter { it != ComandaPrintOutcome.Printed }.forEach {
            assertFalse("$it", comandaPrintFeedback(it, 1).markPrinted)
        }
    }
}
