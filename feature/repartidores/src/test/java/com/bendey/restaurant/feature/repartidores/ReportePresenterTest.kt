package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.delivery.DeliveryReport
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryReportSummary
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

/** D5: reporte de delivery (rangos, validación, vacío, error con catálogo). */
class ReportePresenterTest {
    private class FakeRepo : DeliveryReportRepository {
        val calls = CopyOnWriteArrayList<Pair<String, String>>()
        @Volatile var result: AppResult<DeliveryReport> = AppResult.Success(DeliveryReport(summary = DeliveryReportSummary(orders = 3)))
        override suspend fun report(from: String, to: String): AppResult<DeliveryReport> {
            calls += from to to
            return result
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo()
    private val today = LocalDate.of(2026, 10, 9)
    private fun presenter() = ReportePresenter(scope, repo) { today }

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    @Test fun arrancaConSieteDias() {
        val p = presenter()
        p.start()
        await { p.state.value.report != null }
        assertEquals("2026-10-03" to "2026-10-09", repo.calls.single())
        assertEquals(ReportPreset.LAST_7, p.state.value.preset)
        assertFalse(p.state.value.isEmpty)
    }

    @Test fun hoyY30DiasPidenSuRango() {
        val p = presenter()
        p.selectPreset(ReportPreset.TODAY)
        await { repo.calls.size == 1 && !p.state.value.loading }
        p.selectPreset(ReportPreset.LAST_30)
        await { repo.calls.size == 2 && !p.state.value.loading }
        assertEquals("2026-10-09" to "2026-10-09", repo.calls[0])
        assertEquals("2026-09-10" to "2026-10-09", repo.calls[1])
    }

    @Test fun elegirFechasNoConsultaHastaPedirlo() {
        val p = presenter()
        p.selectPreset(ReportPreset.CUSTOM)
        assertTrue(repo.calls.isEmpty())
        assertEquals("2026-10-09", p.state.value.toText)
        p.setFrom("2026-06-01")
        p.applyCustom()
        assertNotNull(p.state.value.error)
        assertTrue(repo.calls.isEmpty())
        p.setFrom("2026-10-01")
        p.applyCustom()
        await { repo.calls.size == 1 && !p.state.value.loading }
        assertNull(p.state.value.error)
        assertEquals("2026-10-01" to "2026-10-09", repo.calls[0])
    }

    @Test fun sinPedidosEsVacio() {
        repo.result = AppResult.Success(DeliveryReport())
        val p = presenter()
        p.start()
        await { p.state.value.report != null }
        assertTrue(p.state.value.isEmpty)
    }

    @Test fun errorDelServidorSeMuestraSinReporte() {
        repo.result = AppResult.Error("Revisa las fechas del reporte: usa un rango de hasta 92 días.")
        val p = presenter()
        p.start()
        await { p.state.value.error != null }
        assertNull(p.state.value.report)
        assertFalse(p.state.value.loading)
    }
}
