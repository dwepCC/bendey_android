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
import java.time.Instant

/** R10.9: tablero de entregas y aviso de comprobantes que necesitan atención (ambos solo lectura). */
class ReadOnlyBoardsTest {
    private val now = Instant.parse("2026-10-04T15:00:00Z")

    private fun item(status: String, assignedAt: String?) = DeliveryBoardItem(
        assignmentId = 1, sessionId = 1, status = status, assignedAt = assignedAt,
        driverId = 1, driverName = "Luis", customerName = "Ana", deliveryAddress = "Av. Sol 123",
    )

    // ---- tablero de entregas ----
    @Test fun etiquetas_identicas_a_tauri() {
        val esperado = mapOf(
            "assigned" to "Asignado", "accepted" to "Aceptado", "rejected" to "Rechazado",
            "picked_up" to "Recogido", "on_the_way" to "En camino", "delivered" to "Entregado", "failed" to "Incidencia",
        )
        esperado.forEach { (k, v) -> assertEquals(v, deliveryStatusLabel(k)) }
        assertEquals("En curso", deliveryStatusLabel("otro"))
    }

    @Test fun tiempo_desde_que_se_asigno() {
        assertEquals("hace 12 min", deliveryElapsedText("2026-10-04T14:48:00Z", now))
        assertEquals("hace 1 h 05 min", deliveryElapsedText("2026-10-04T13:55:00Z", now))
        assertEquals("recién asignada", deliveryElapsedText("2026-10-04T14:59:30Z", now))
        assertEquals("", deliveryElapsedText(null, now))
        assertEquals("", deliveryElapsedText("no es fecha", now))
    }

    @Test fun acepta_fechas_con_zona_horaria_de_lima() {
        assertEquals("hace 30 min", deliveryElapsedText("2026-10-04T09:30:00-05:00", now))
    }

    @Test fun una_entrega_sin_aceptar_mucho_rato_pide_atencion() {
        assertTrue(deliveryNeedsAttention(item("assigned", "2026-10-04T14:45:00Z"), now))
        assertFalse(deliveryNeedsAttention(item("assigned", "2026-10-04T14:55:00Z"), now))
        assertFalse(deliveryNeedsAttention(item("on_the_way", "2026-10-04T10:00:00Z"), now))
        assertFalse(deliveryNeedsAttention(item("assigned", null), now))
    }

    @Test fun titulo_segun_cantidad() {
        assertEquals("Entregas activas", deliveryBoardTitle(0))
        assertEquals("Entregas activas · 1 en curso", deliveryBoardTitle(1))
        assertEquals("Entregas activas · 4 en curso", deliveryBoardTitle(4))
    }

    @Test fun refresco_cada_60_segundos() {
        assertEquals(60_000L, DELIVERY_BOARD_REFRESH_MS)
    }

    @Test fun linea_de_repartidor_sin_datos_no_dice_null() {
        assertEquals("Av. Sol 123 · Repartidor: Luis", DeliveryBoardCopy.driverLine("Av. Sol 123", "Luis"))
        assertEquals("Sin dirección · Repartidor: sin asignar", DeliveryBoardCopy.driverLine("", ""))
    }

    @Test fun textos_del_tablero_sin_usted_ni_jerga() {
        val textos = listOf(
            DeliveryBoardCopy.TITLE, DeliveryBoardCopy.EMPTY_TITLE, DeliveryBoardCopy.EMPTY_DESCRIPTION,
            DeliveryBoardCopy.ERROR_TITLE, DeliveryBoardCopy.ERROR_DESCRIPTION, DeliveryBoardCopy.READ_ONLY_HINT,
        )
        for (t in textos) assertTrue(t, ForbiddenTerms.find(t).isEmpty())
    }

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
