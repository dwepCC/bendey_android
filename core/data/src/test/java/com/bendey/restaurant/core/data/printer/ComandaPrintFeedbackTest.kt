package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.core.domain.print.PrintOutcome
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
        val f = comandaPrintFeedback(ComandaPrintOutcome.Failed("la impresora está apagada"), 7)
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
        listOf(
            ComandaPrintOutcome.Failed("x"),
            ComandaPrintOutcome.NotConfigured,
            ComandaPrintOutcome.ServerUnreachable,
            ComandaPrintOutcome.ServerDown,
            ComandaPrintOutcome.AutoPrintOff,
            ComandaPrintOutcome.NothingToPrint,
        ).forEach {
            assertFalse("$it", comandaPrintFeedback(it, 1).markPrinted)
        }
    }

    @Test
    fun textosIdenticosATauri() {
        assertEquals(
            "Comanda #7 enviada, pero no se imprimió. No se pudo imprimir: la impresora está apagada. " +
                "Revisa la impresora y vuelve a intentar.",
            comandaPrintFeedback(ComandaPrintOutcome.Failed("la impresora está apagada"), 7).alert,
        )
        assertEquals(
            "Comanda #7 enviada, pero no se imprimió. Esta impresora no está configurada. Ve a Impresoras para elegirla.",
            comandaPrintFeedback(ComandaPrintOutcome.NotConfigured, 7).alert,
        )
    }

    @Test
    fun servidorInalcanzableNoSeMuestraComoSinConfigurar() {
        val unreachable = comandaPrintFeedback(ComandaPrintOutcome.ServerUnreachable, 7)
        assertTrue(unreachable.alert!!.contains("No encontramos el servidor de impresión de tu red"))
        assertFalse(unreachable.alert!!.contains("no está configurada"))
        assertTrue(unreachable.canReprint)
        val down = comandaPrintFeedback(ComandaPrintOutcome.ServerDown, 7)
        assertTrue(down.alert!!.contains("El servidor de impresión no respondió"))
        assertFalse(down.markPrinted)
    }

    @Test
    fun deEstadoUnificadoAResultadoDeComanda() {
        assertEquals(ComandaPrintOutcome.Printed, PrintOutcome.Ok.toComandaOutcome())
        assertEquals(ComandaPrintOutcome.NotConfigured, PrintOutcome.NotConfigured.toComandaOutcome())
        assertEquals(ComandaPrintOutcome.ServerUnreachable, PrintOutcome.ServerUnreachable.toComandaOutcome())
        assertEquals(ComandaPrintOutcome.ServerDown, PrintOutcome.ServerDown.toComandaOutcome())
        assertEquals(ComandaPrintOutcome.Failed("r"), PrintOutcome.failed("r").toComandaOutcome())
    }
}
