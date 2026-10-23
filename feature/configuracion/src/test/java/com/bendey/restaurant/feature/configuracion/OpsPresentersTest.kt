package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.ops.BusinessHours
import com.bendey.restaurant.core.domain.ops.BusinessHoursStatus
import com.bendey.restaurant.core.domain.ops.BusinessInterval
import com.bendey.restaurant.core.domain.ops.OpsControls
import com.bendey.restaurant.core.domain.ops.OpsRepository
import com.bendey.restaurant.core.domain.ops.ReopenResult
import com.bendey.restaurant.core.domain.ops.SlotDraft
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

/** Control de descuentos y horario de atención de Ajustes (lógica pura, sin Android). */
class OpsPresentersTest {
    private class FakeRepo : OpsRepository {
        @Volatile var controls = OpsControls(discountControl = false, canDiscount = true)
        @Volatile var hours = BusinessHours(listOf(BusinessInterval(1, "11:00", "22:00")), BusinessHoursStatus(true, true, ""))
        @Volatile var saveError: String? = null
        val toggles = CopyOnWriteArrayList<Boolean>()
        val saves = CopyOnWriteArrayList<List<BusinessInterval>>()

        override fun peekOpsControls(): OpsControls? = controls
        override suspend fun getOpsControls(forceRefresh: Boolean): AppResult<OpsControls> = AppResult.Success(controls)
        override suspend fun setDiscountControl(enabled: Boolean): AppResult<OpsControls> {
            toggles += enabled
            controls = controls.copy(discountControl = enabled)
            return AppResult.Success(controls)
        }

        override suspend fun setShowPosStock(enabled: Boolean): AppResult<OpsControls> {
            controls = controls.copy(showPosStock = enabled)
            return AppResult.Success(controls)
        }

        override suspend fun getBusinessHours(): AppResult<BusinessHours> = AppResult.Success(hours)
        override suspend fun saveBusinessHours(intervals: List<BusinessInterval>): AppResult<BusinessHours> {
            saves += intervals
            saveError?.let { return AppResult.Error(it) }
            hours = BusinessHours(intervals, BusinessHoursStatus(intervals.isNotEmpty(), true, ""))
            return AppResult.Success(hours)
        }

        override suspend fun reopenSession(saleId: Int, pin: String, reason: String): AppResult<ReopenResult> =
            AppResult.Success(ReopenResult(1, 0, true))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo()

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    // ---- Control de descuentos ----

    @Test fun elInterruptorEstaApagadoPorDefectoYSoloAdminLoCambia() {
        val p = DiscountControlPresenter(scope, repo)
        p.load()
        await { p.state.value.controls != null }
        assertFalse(p.state.value.enabled)

        p.setPermissions(listOf("o.ch"))
        p.toggle(true)
        assertTrue(repo.toggles.isEmpty())

        p.setPermissions(listOf("s.m"))
        p.toggle(true)
        await { p.state.value.enabled }
        assertEquals(listOf(true), repo.toggles.toList())
    }

    @Test fun noReenviaSiNoCambia() {
        val p = DiscountControlPresenter(scope, repo)
        p.setPermissions(listOf("s.m"))
        p.load()
        await { p.state.value.controls != null }
        p.toggle(false)
        assertTrue(repo.toggles.isEmpty())
    }

    // ---- Horario ----

    private fun hoursAdmin(): BusinessHoursPresenter {
        val p = BusinessHoursPresenter(scope, repo)
        p.setPermissions(listOf("s.m"))
        p.load()
        await { p.state.value.saved != null }
        return p
    }

    @Test fun cargaElHorarioGuardado() {
        val p = hoursAdmin()
        assertTrue(p.state.value.week[1]!!.open)
        assertFalse(p.state.value.week[2]!!.open)
        assertFalse(p.state.value.dirty)
        assertFalse(p.state.value.canSave)
    }

    @Test fun guardaReemplazandoTodoElHorario() {
        val p = hoursAdmin()
        p.setDayOpen(2, true)
        assertTrue(p.state.value.canSave)
        p.save()
        await { p.state.value.savedNotice }
        assertEquals(
            listOf(BusinessInterval(1, "11:00", "22:00"), BusinessInterval(2, "11:00", "22:00")),
            repo.saves.single(),
        )
        assertFalse(p.state.value.dirty)
    }

    @Test fun noGuardaUnTramoInvalido() {
        val p = hoursAdmin()
        p.updateSlot(1, 0, SlotDraft("11:00", "11:00"))
        assertNotNull(p.state.value.validation)
        assertFalse(p.state.value.canSave)
        p.save()
        assertTrue(repo.saves.isEmpty())
        assertNotNull(p.state.value.error)
    }

    @Test fun apagarTodosLosDiasEnviaListaVacia() {
        val p = hoursAdmin()
        p.setDayOpen(1, false)
        p.save()
        await { p.state.value.savedNotice }
        assertTrue(repo.saves.single().isEmpty())
        assertFalse(p.state.value.status.configured)
    }

    @Test fun sinPermisoEsSoloLectura() {
        val p = BusinessHoursPresenter(scope, repo)
        p.setPermissions(listOf("o.ch"))
        p.load()
        await { p.state.value.saved != null }
        p.setDayOpen(2, true)
        assertFalse(p.state.value.week[2]!!.open)
        p.save()
        assertTrue(repo.saves.isEmpty())
    }

    @Test fun errorDelServidorSeMuestraYConservaElBorrador() {
        repo.saveError = "Revisa el horario"
        val p = hoursAdmin()
        p.setDayOpen(3, true)
        p.save()
        await { p.state.value.error != null }
        assertEquals("Revisa el horario", p.state.value.error)
        assertTrue(p.state.value.week[3]!!.open)
        assertNull(p.state.value.saved?.intervals?.firstOrNull { it.weekday == 3 })
    }
}
