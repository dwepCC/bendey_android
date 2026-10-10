package com.bendey.restaurant.core.domain.rcshare

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

/** Reparto del recargo al consumo: período, división al céntimo, validación contra lo pendiente y la hoja. */
class RcShareTest {
    private val today = LocalDate.of(2026, 10, 9)

    @Test fun periodoPorDefectoEsElMesHastaHoy() {
        val r = RcShareRules.defaultPeriod(today)
        assertEquals("2026-10-01", r.fromIso)
        assertEquals("2026-10-09", r.toIso)
        val prev = RcShareRules.previousMonth(today)
        assertEquals("2026-09-01", prev.fromIso)
        assertEquals("2026-09-30", prev.toIso)
        assertEquals(92, RcShareRules.MAX_DAYS)
    }

    @Test fun periodoMaximo92DiasYSinFuturo() {
        assertNull(RcShareRules.validatePeriod(today.minusDays(91), today, today))
        assertNotNull(RcShareRules.validatePeriod(today.minusDays(92), today, today))
        assertNotNull(RcShareRules.validatePeriod(today, today.plusDays(1), today))
        assertNotNull(RcShareRules.validatePeriod(today, today.minusDays(1), today))
    }

    private fun lines(vararg amounts: String) = amounts.mapIndexed { i, a -> RcShareDraftLine(i + 1, a) }

    @Test fun divideEnPartesIgualesAlCentimo() {
        // 100.00 entre 3 = 33.34 + 33.33 + 33.33 (cuadra al céntimo)
        val out = RcShareRules.splitRemainder(100.0, lines("", "", ""))
        assertEquals(listOf("33.34", "33.33", "33.33"), out.map { it.amount })
        assertEquals(10_000L, RcShareRules.totalCents(out))
        // Lo que ya se escribió se respeta; solo se reparte lo que falta entre los vacíos.
        val partial = RcShareRules.splitRemainder(100.0, lines("40", "", ""))
        assertEquals(listOf("40", "30.00", "30.00"), partial.map { it.amount })
        assertEquals(10_000L, RcShareRules.totalCents(partial))
        // 0.01 entre 2: un céntimo para el primero.
        val tiny = RcShareRules.splitRemainder(0.01, lines("", ""))
        assertEquals(listOf("0.01", ""), tiny.map { it.amount })
    }

    @Test fun divisionSinNadaQueRepartir() {
        // Ya se pasó o completó: no cambia nada.
        val same = lines("60", "50", "")
        assertEquals(same, RcShareRules.splitRemainder(100.0, same))
        val full = lines("50", "50", "")
        assertEquals(full, RcShareRules.splitRemainder(100.0, full))
        // Sin vacíos: tampoco.
        val none = lines("10", "20")
        assertEquals(none, RcShareRules.splitRemainder(100.0, none))
    }

    @Test fun validaContraLoPendiente() {
        assertNotNull(RcShareRules.validate(100.0, lines("", "")))
        assertNull(RcShareRules.validate(100.0, lines("60", "40")))
        assertNull(RcShareRules.validate(100.0, lines("60", "")))
        assertNotNull(RcShareRules.validate(100.0, lines("60", "40.01")))
        assertNotNull(RcShareRules.validate(100.0, lines("-5", "10")))
        assertNotNull(RcShareRules.validate(100.0, listOf(RcShareDraftLine(1, "10"), RcShareDraftLine(1, "10"))))
        // 0.1 + 0.2 no debe pasarse por coma flotante.
        assertNull(RcShareRules.validate(0.3, lines("0.1", "0.2")))
    }

    @Test fun sobranteEnVivo() {
        assertEquals(4000L, RcShareRules.leftoverCents(100.0, lines("60", "")))
        assertEquals(-100L, RcShareRules.leftoverCents(100.0, lines("60", "41")))
        assertEquals(100.0, RcShareRules.total(lines("60", "40")), 0.0)
    }

    @Test fun armaElEnvioSoloConLasLineasConMonto() {
        val input = RcShareRules.toInput("2026-10-01", "2026-10-09", lines("60,5", "", "39.5"), mapOf(1 to "Ana", 3 to "Luis"), "  cierre  ", true)
        assertEquals(listOf(RcLine(1, "Ana", 60.5), RcLine(3, "Luis", 39.5)), input.lines)
        assertEquals("cierre", input.notes)
        assertTrue(input.payFromCash)
    }

    @Test fun avisoSoloEnMesTerminadoConPendiente() {
        val old = RcSummary(from = "2026-09-01", to = "2026-09-30", pending = 12.0)
        assertTrue(RcShareRules.isOverdue(old, today))
        assertFalse(RcShareRules.isOverdue(old.copy(pending = 0.0), today))
        // Mes en curso: todavía no.
        assertFalse(RcShareRules.isOverdue(RcSummary(from = "2026-10-01", to = "2026-10-09", pending = 50.0), today))
    }

    @Test fun permisos() {
        assertTrue(canSeeRcShare(listOf("c.v")))
        assertTrue(canSeeRcShare(listOf("s.m")))
        assertFalse(canSeeRcShare(listOf("o.ch")))
        assertTrue(canDistributeRc(listOf("s.m")))
        assertFalse(canDistributeRc(listOf("c.v")))
    }

    // ---- presentador ----

    private class FakeRepo : RcShareRepository {
        val summaries = CopyOnWriteArrayList<Pair<String, String>>()
        val sent = CopyOnWriteArrayList<RcShareInput>()
        @Volatile var pending = 100.0
        @Volatile var distributeResult: AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun summary(from: String, to: String): AppResult<RcSummary> {
            summaries += from to to
            return AppResult.Success(RcSummary(from = from, to = to, collected = 100.0, pending = pending))
        }

        override suspend fun distribute(input: RcShareInput): AppResult<Unit> {
            sent += input
            return distributeResult
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo()
    private val staff = listOf(RcStaff(1, "Ana"), RcStaff(2, "Luis"), RcStaff(3, "Rosa"))
    private fun presenter() = RcSharePresenter(scope, repo, { AppResult.Success(staff) }) { today }

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    @Test fun arrancaConElMesActual() {
        val p = presenter()
        p.start(canDistribute = true)
        await { p.state.value.summary != null }
        assertEquals("2026-10-01" to "2026-10-09", repo.summaries.single())
        p.selectPreset(RcPeriodPreset.PREV_MONTH)
        await { repo.summaries.size == 2 && !p.state.value.loading }
        assertEquals("2026-09-01" to "2026-09-30", repo.summaries[1])
    }

    @Test fun repartirEnPartesIgualesYEnviar() {
        val p = presenter()
        p.start(true)
        await { p.state.value.summary != null }
        p.openDistribute()
        await { p.state.value.draft.size == 3 }
        p.splitRemainder()
        assertEquals(10_000L, RcShareRules.totalCents(p.state.value.draft))
        assertTrue(p.state.value.canSubmit)
        p.setNotes("cierre de octubre")
        p.setPayFromCash(true)
        p.submit()
        await { repo.sent.size == 1 && !p.state.value.submitting }
        val sent = repo.sent.single()
        assertEquals("2026-10-01", sent.from)
        assertEquals("2026-10-09", sent.to)
        assertEquals(100.0, sent.lines.sumOf { it.amount }, 0.0001)
        assertEquals("Ana", sent.lines.first().name)
        assertTrue(sent.payFromCash)
        assertFalse(p.state.value.sheetOpen)
    }

    @Test fun noEnviaSiSePasaDeLoPendiente() {
        val p = presenter()
        p.start(true)
        await { p.state.value.summary != null }
        p.openDistribute()
        await { p.state.value.draft.size == 3 }
        p.setAmount(1, "70")
        p.setAmount(2, "40")
        assertTrue(p.state.value.overLimit)
        assertFalse(p.state.value.canSubmit)
        p.submit()
        assertTrue(repo.sent.isEmpty())
        assertNotNull(p.state.value.submitError)
    }

    @Test fun soloElAdministradorReparte() {
        val p = presenter()
        p.start(canDistribute = false)
        await { p.state.value.summary != null }
        p.openDistribute()
        assertFalse(p.state.value.sheetOpen)
    }

    @Test fun errorDelServidorSeMuestraEnLaHoja() {
        repo.distributeResult = AppResult.Error("El reparto supera lo que queda por repartir de ese período.", code = "RC_SHARE_EXCEEDS_PENDING")
        val p = presenter()
        p.start(true)
        await { p.state.value.summary != null }
        p.openDistribute()
        await { p.state.value.draft.size == 3 }
        p.setAmount(1, "10")
        p.submit()
        await { p.state.value.submitError != null && !p.state.value.submitting }
        assertTrue(p.state.value.sheetOpen)
        // Con EXCEEDS_PENDING vuelve a leer lo pendiente.
        await { repo.summaries.size >= 2 }
    }
}
