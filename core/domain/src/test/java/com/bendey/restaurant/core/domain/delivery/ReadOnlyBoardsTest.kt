package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.billing.StuckVoidBannerState
import com.bendey.restaurant.core.domain.billing.StuckVoidCopy
import com.bendey.restaurant.core.domain.billing.StuckVoidCreditNote
import com.bendey.restaurant.core.domain.billing.stuckVoidBannerState
import com.bendey.restaurant.core.domain.copy.ForbiddenTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R10.9: aviso de comprobantes que necesitan atención (solo lectura). El tablero de Delivery vive en DeliveryBoardLogicTest. */
class ReadOnlyBoardsTest {
    // ---- aviso de comprobantes ----
    private fun nc(id: Int, stalled: Boolean, msg: String = "") = StuckVoidCreditNote(
        originalSaleId = id, originalNumber = "B001-$id", originalDocType = "BOLETA", originalTotal = 59.9,
        creditNoteId = id + 100, creditNoteNumber = "", sunatMessage = msg, stalled = stalled,
        createdAt = "2026-09-30T10:15:00-05:00",
    )

    @Test fun titulo_del_aviso_singular_y_plural() {
        assertEquals("1 comprobante necesita atención", StuckVoidCopy.title(1))
        assertEquals("3 comprobantes necesitan atención", StuckVoidCopy.title(3))
        assertTrue(StuckVoidCopy.description(1).contains("esta venta sigue vigente"))
        assertTrue(StuckVoidCopy.description(2).contains("estas ventas siguen vigentes"))
    }

    @Test fun el_aviso_no_aparece_sin_sunat_ni_sin_pendientes() {
        assertEquals(StuckVoidBannerState.Hidden, stuckVoidBannerState(false, listOf(nc(1, false)), null))
        assertEquals(StuckVoidBannerState.Hidden, stuckVoidBannerState(true, emptyList(), null))
        assertEquals(StuckVoidBannerState.Hidden, stuckVoidBannerState(true, null, null))
    }

    @Test fun con_pendientes_muestra_la_lista() {
        val s = stuckVoidBannerState(true, listOf(nc(1, false), nc(2, true)), null)
        assertTrue(s is StuckVoidBannerState.Pending)
        assertEquals(2, (s as StuckVoidBannerState.Pending).rows.size)
    }

    @Test fun un_fallo_de_consulta_se_dice_y_no_finge_que_no_hay_pendientes() {
        val s = stuckVoidBannerState(true, null, "Sin conexion")
        assertTrue(s is StuckVoidBannerState.Failed)
        // Aunque hubiera datos viejos, el fallo manda: no se oculta en silencio.
        assertTrue(stuckVoidBannerState(true, listOf(nc(1, true)), "x") is StuckVoidBannerState.Failed)
        // Sin SUNAT no hay nada que consultar: ni siquiera un fallo se muestra.
        assertEquals(StuckVoidBannerState.Hidden, stuckVoidBannerState(false, null, "x"))
    }

    @Test fun lineas_de_detalle() {
        val r = nc(7, stalled = false)
        assertEquals("BOLETA B001-7 · S/ 59.90", StuckVoidCopy.line1(r, "S/ 59.90"))
        assertEquals("NC #107 · rechazada por SUNAT · 30/09/2026", StuckVoidCopy.line2(r, StuckVoidCopy.date(r.createdAt)))
        val s = nc(8, stalled = true).copy(creditNoteNumber = "BC01-5")
        assertEquals("NC BC01-5 · sin respuesta de SUNAT · 30/09/2026", StuckVoidCopy.line2(s, StuckVoidCopy.date(s.createdAt)))
        assertEquals("", StuckVoidCopy.date(null))
        assertEquals("", StuckVoidCopy.date("basura"))
    }

    @Test fun textos_del_aviso_sin_usted_ni_jerga() {
        val textos = listOf(
            StuckVoidCopy.title(1), StuckVoidCopy.title(2), StuckVoidCopy.description(1), StuckVoidCopy.description(2),
            StuckVoidCopy.RESOLVE_HINT, StuckVoidCopy.SHOW, StuckVoidCopy.HIDE, StuckVoidCopy.LOAD_ERROR,
        )
        for (t in textos) assertTrue(t, ForbiddenTerms.find(t).isEmpty())
    }
}
