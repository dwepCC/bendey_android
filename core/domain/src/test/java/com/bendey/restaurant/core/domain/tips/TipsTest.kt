package com.bendey.restaurant.core.domain.tips

import com.bendey.restaurant.core.domain.delivery.RangedReportPresenter
import com.bendey.restaurant.core.domain.delivery.ReportPreset
import com.bendey.restaurant.core.domain.model.AppResult
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Propinas: monto sugerido, validación, propina a enviar, permisos, textos y reporte por rango. */
class TipsTest {
    @Test fun sugeridaAlCentimo() {
        assertEquals(5.0, TipRules.suggest(100.0, 5), 0.0)
        assertEquals(10.0, TipRules.suggest(100.0, 10), 0.0)
        // 5 % de 21.10 = 1.055 -> mitad hacia arriba = 1.06
        assertEquals(1.06, TipRules.suggest(21.10, 5), 0.0)
        assertEquals(3.35, TipRules.suggest(33.5, 10), 0.0)
        assertEquals(0.0, TipRules.suggest(0.0, 10), 0.0)
        assertEquals(0.0, TipRules.suggest(-5.0, 10), 0.0)
        assertEquals("5.00", TipRules.format(5.0))
    }

    @Test fun validaElMonto() {
        assertNull(TipRules.validate(""))
        assertNull(TipRules.validate("0"))
        assertNull(TipRules.validate("5000"))
        assertNull(TipRules.validate("12,50"))
        assertNotNull(TipRules.validate("5000.01"))
        assertNotNull(TipRules.validate("-1"))
        assertNotNull(TipRules.validate("abc"))
    }

    @Test fun propinaAEnviar() {
        // Sin monto o en cero = sin propina.
        assertNull(TipRules.toInput(TipDraft(), "cash"))
        assertNull(TipRules.toInput(TipDraft(amount = "0"), "cash"))
        assertNull(TipRules.toInput(TipDraft(amount = "6000"), "cash"))
        // Medio por defecto = el principal del cobro; editable.
        val a = TipRules.toInput(TipDraft(amount = "7,5"), "yape")!!
        assertEquals(TipInput(7.5, "yape", null), a)
        val b = TipRules.toInput(TipDraft(amount = "3", method = "cash", staffId = 4), "yape")!!
        assertEquals(TipInput(3.0, "cash", 4), b)
        assertTrue(TipRules.hasTip(TipDraft(amount = "0.01")))
        assertFalse(TipRules.hasTip(TipDraft(amount = "")))
    }

    @Test fun permisosYPestanas() {
        assertTrue(canPayTips(listOf("o.ch")))
        assertTrue(canPayTips(listOf("s.m")))
        assertFalse(canPayTips(listOf("c.v")))
        assertTrue(canSeeTipsReport(listOf("c.v")))
        assertFalse(canSeeTipsReport(listOf("t.o")))
        assertEquals(listOf(PropinasTab.MIS_PROPINAS), PropinasTabs.available(listOf("t.o")))
        assertEquals(listOf(PropinasTab.POR_PAGAR, PropinasTab.MIS_PROPINAS), PropinasTabs.available(listOf("o.ch")))
        assertEquals(
            listOf(PropinasTab.POR_PAGAR, PropinasTab.REPORTE, PropinasTab.MIS_PROPINAS),
            PropinasTabs.available(listOf("s.m")),
        )
        assertEquals(listOf(PropinasTab.MIS_PROPINAS), PropinasTabs.available(null))
    }

    @Test fun textos() {
        val row = PendingTipStaff(staffId = 3, name = "Ana", count = 2, total = 18.5, unbooked = 0)
        val msg = TipsText.payoutConfirm(row)
        assertTrue(msg.contains("S/ 18.50"))
        assertTrue(msg.contains("2 propinas"))
        assertTrue(msg.contains("TU caja"))
        assertEquals("1 propina", TipsText.count(1))
        assertEquals("Sin mozo asignado", TipsText.staffName(PendingTipStaff(0, "", 1, 1.0, 0)))
        assertEquals(30.0, TipsText.totalPending(listOf(row, row.copy(staffId = 4, total = 11.5))), 0.0)
        assertEquals("Efectivo", TipsText.methodLabel("cash"))
    }

    // ---- reporte con selector de rango ----

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val today = LocalDate.of(2026, 10, 9)

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    @Test fun reportePorRango() {
        val calls = CopyOnWriteArrayList<Pair<String, String>>()
        val p = RangedReportPresenter<TipsReport>(scope, { today }) { from, to ->
            calls += from to to
            AppResult.Success(TipsReport(total = 10.0, count = 1))
        }
        p.start()
        await { p.state.value.data != null }
        assertEquals("2026-10-03" to "2026-10-09", calls.single())
        p.selectPreset(ReportPreset.TODAY)
        await { calls.size == 2 && !p.state.value.loading }
        assertEquals("2026-10-09" to "2026-10-09", calls[1])
        p.selectPreset(ReportPreset.LAST_30)
        await { calls.size == 3 && !p.state.value.loading }
        assertEquals("2026-09-10" to "2026-10-09", calls[2])
    }

    @Test fun reporteValidaFechasPersonalizadas() {
        val calls = CopyOnWriteArrayList<Pair<String, String>>()
        val p = RangedReportPresenter<TipsReport>(scope, { today }) { from, to ->
            calls += from to to
            AppResult.Success(TipsReport())
        }
        p.selectPreset(ReportPreset.CUSTOM)
        // Futuro
        p.setFrom("2026-10-01")
        p.setTo("2026-10-20")
        p.applyCustom()
        assertNotNull(p.state.value.error)
        assertTrue(calls.isEmpty())
        // Más de 92 días
        p.setFrom("2026-01-01")
        p.setTo("2026-10-09")
        p.applyCustom()
        assertNotNull(p.state.value.error)
        // Válido
        p.setFrom("2026-10-01")
        p.setTo("2026-10-09")
        p.applyCustom()
        await { calls.size == 1 && !p.state.value.loading }
        assertEquals("2026-10-01" to "2026-10-09", calls.single())
        assertNull(p.state.value.error)
    }

    @Test fun reporteConErrorMuestraElMensaje() {
        val p = RangedReportPresenter<TipsReport>(scope, { today }) { _, _ -> AppResult.Error("Las propinas aún no están disponibles.") }
        p.start()
        await { p.state.value.error != null }
        assertEquals("Las propinas aún no están disponibles.", p.state.value.error)
        assertNull(p.state.value.data)
    }
}
