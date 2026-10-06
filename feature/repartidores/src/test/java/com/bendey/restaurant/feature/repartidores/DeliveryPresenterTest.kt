package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DELIVERY_REASON_OTHER
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliverySection
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** D1: el reductor de acciones de la vista Delivery (permiso d.u, carga, errores, doble envío, motivos, recarga). */
class DeliveryPresenterTest {

    private class FakeRepo : DeliveryRepository {
        val boardCalls = AtomicInteger(0)
        val actions = CopyOnWriteArrayList<String>()
        @Volatile var actionResult: AppResult<Unit> = AppResult.Success(Unit)
        @Volatile var latencyMs: Long = 0

        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> {
            boardCalls.incrementAndGet()
            return AppResult.Success(DeliveryBoardData())
        }

        private suspend fun act(name: String): AppResult<Unit> {
            actions += name
            if (latencyMs > 0) delay(latencyMs)
            return actionResult
        }

        override suspend fun assignDriver(sessionId: Int, driverId: Int) = act("assign:$sessionId:$driverId")
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String) = act("cancel:$sessionId:$reason")
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?) =
            act("status:$assignmentId:$status:${failedReason.orEmpty()}")

        override suspend fun listDrivers(): AppResult<List<DeliveryDriver>> = AppResult.Success(emptyList())
        override suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteDriver(id: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun listCompanies(): AppResult<List<DeliveryCompany>> = AppResult.Success(emptyList())
        override suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCompany(id: Int): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo()
    private val store = DeliveryBoardStore(repo).also { it.setEnabled(true) }
    private val presenter = DeliveryPresenter(scope, store, repo)

    @After fun tearDown() = scope.cancel()

    private fun card(assignmentId: Int? = 7) = DeliveryCard(
        sessionId = 41, assignmentId = assignmentId, source = "staff", customerName = "Ana", customerPhone = "999000111",
        address = "Av. Sol", reference = "", itemsCount = 1, totalAmount = 10.0, orderStatus = "ready", createdAt = null,
    )

    private fun operator() = presenter.setPermissions(listOf("d.v", "d.u"))

    private fun await(timeoutMs: Long = 3000, cond: () -> Boolean) = runBlocking {
        withTimeout(timeoutMs) { while (!cond()) delay(10) }
    }

    /** Espera a que termine la acción en curso Y a que se recargue el tablero. */
    private fun settle() = await { !presenter.state.value.busy && repo.boardCalls.get() >= 1 }

    // ---- permisos ----

    @Test fun sinPermisoDeOperarNoAbreDialogosNiHaceNada() {
        presenter.setPermissions(listOf("d.v"))
        assertFalse(presenter.state.value.canAssign)
        presenter.openAssign(card()); presenter.openCancel(card()); presenter.openDelivered(card()); presenter.openFailed(card())
        assertNull(presenter.state.value.dialog)
        presenter.assign(3); presenter.confirmCancel(); presenter.confirmDelivered(); presenter.confirmFailed()
        assertTrue(repo.actions.isEmpty())
    }

    @Test fun elRepartidorSoloMiraAunqueTengaDU() {
        presenter.setPermissions(listOf("d.v", "d.u"), "driver")
        assertFalse(presenter.state.value.canAssign)
        presenter.openCancel(card())
        assertNull(presenter.state.value.dialog)
        presenter.setPermissions(listOf("d.v", "d.u"), "cashier")
        assertTrue(presenter.state.value.canAssign)
    }

    @Test fun conPermisoDeOperarSeAbrenLosDialogos() {
        operator()
        assertTrue(presenter.state.value.canAssign)
        presenter.openAssign(card())
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Assign)
        presenter.dismissDialog()
        assertNull(presenter.state.value.dialog)
    }

    @Test fun sinAsignacionNoSePuedeMarcarEntregadoNiFallido() {
        operator()
        presenter.openDelivered(card(assignmentId = null))
        assertNull(presenter.state.value.dialog)
        presenter.openFailed(card(assignmentId = null))
        assertNull(presenter.state.value.dialog)
    }

    // ---- asignar ----

    @Test fun asignarConExitoCierraAvisaYRecargaElTablero() {
        operator()
        presenter.openAssign(card())
        val msgs = CopyOnWriteArrayList<String>()
        val job = collect(presenter.messages, msgs)
        presenter.assign(3)
        settle()
        await { msgs.isNotEmpty() }
        assertEquals(listOf("assign:41:3"), repo.actions.toList())
        assertNull(presenter.state.value.dialog)
        assertEquals("Repartidor asignado.", msgs.first())
        assertNull(presenter.state.value.dialogError)
        assertTrue("recarga el tablero", repo.boardCalls.get() >= 1)
        job.cancel()
    }

    @Test fun unErrorSeMuestraDentroDelDialogoYEnElSnackbarYRecarga() {
        operator()
        repo.actionResult = AppResult.Error("Ese repartidor está marcado como no disponible. Elige a otro o espera a que se conecte.")
        presenter.openAssign(card())
        val msgs = CopyOnWriteArrayList<String>()
        val job = collect(presenter.messages, msgs)
        presenter.assign(3)
        settle()
        await { msgs.isNotEmpty() }
        assertTrue("el diálogo sigue abierto para elegir a otro", presenter.state.value.dialog is DeliveryDialog.Assign)
        assertEquals(msgs.first(), presenter.state.value.dialogError)
        assertTrue(msgs.first().contains("no disponible"))
        assertTrue(repo.boardCalls.get() >= 1)
        assertFalse(presenter.state.value.busy)
        job.cancel()
    }

    @Test fun mientrasCorreUnaAccionNoSeAceptaOtra() {
        operator()
        repo.latencyMs = 200
        presenter.openAssign(card())
        presenter.assign(3)
        assertTrue(presenter.state.value.busy)
        presenter.assign(4)
        presenter.openCancel(card())
        presenter.dismissDialog()
        assertTrue("no se cierra a medio camino", presenter.state.value.dialog is DeliveryDialog.Assign)
        settle()
        assertEquals(listOf("assign:41:3"), repo.actions.toList())
    }

    // ---- cancelar ----

    @Test fun cancelarExigeMotivoYNoLlamaAlServidorSinEl() {
        operator()
        presenter.openCancel(card())
        presenter.confirmCancel()
        assertTrue(presenter.state.value.reasonShowError)
        assertTrue(repo.actions.isEmpty())
        presenter.chooseReason(DELIVERY_REASON_OTHER)
        presenter.setReasonText("ab")
        presenter.confirmCancel()
        assertTrue(presenter.state.value.reasonShowError)
        assertTrue(repo.actions.isEmpty())
    }

    @Test fun cancelarConMotivoRapido() {
        operator()
        presenter.openCancel(card())
        presenter.chooseReason("No hay stock")
        presenter.confirmCancel()
        settle()
        assertEquals(listOf("cancel:41:No hay stock"), repo.actions.toList())
        assertNull(presenter.state.value.dialog)
    }

    @Test fun cancelarConOtroUsaElTextoEscritoRecortado() {
        operator()
        presenter.openCancel(card())
        presenter.chooseReason(DELIVERY_REASON_OTHER)
        presenter.setReasonText("  se quemó la comida  ")
        presenter.confirmCancel()
        settle()
        assertEquals(listOf("cancel:41:se quemó la comida"), repo.actions.toList())
    }

    @Test fun abrirOtroDialogoLimpiaElMotivoAnterior() {
        operator()
        presenter.openCancel(card())
        presenter.chooseReason("No contesta")
        presenter.dismissDialog()
        presenter.openCancel(card())
        assertNull(presenter.state.value.reasonChoice)
        assertEquals("", presenter.state.value.reasonText)
    }

    // ---- entregado / fallido ----

    @Test fun marcarEntregadoUsaLaAsignacionNoLaSesion() {
        operator()
        presenter.openDelivered(card(assignmentId = 7))
        presenter.confirmDelivered()
        settle()
        assertEquals(listOf("status:7:delivered:"), repo.actions.toList())
    }

    @Test fun marcarFallidoExigeMotivoYLoEnvia() {
        operator()
        presenter.openFailed(card(assignmentId = 7))
        presenter.confirmFailed()
        assertTrue(presenter.state.value.reasonShowError)
        assertTrue(repo.actions.isEmpty())
        presenter.chooseReason("El cliente no contesta")
        presenter.confirmFailed()
        settle()
        assertEquals(listOf("status:7:failed:El cliente no contesta"), repo.actions.toList())
    }

    // ---- navegación de la vista ----

    @Test fun laSeccionYLaPestanaDeRepartidoresSeExcluyen() {
        presenter.selectSection(DeliverySection.INCIDENTS)
        assertEquals(DeliverySection.INCIDENTS, presenter.state.value.selected)
        presenter.selectDrivers()
        assertTrue(presenter.state.value.showDrivers)
        presenter.selectSection(DeliverySection.ASSIGNED)
        assertFalse(presenter.state.value.showDrivers)
    }

    @Test fun elTextoDelMotivoSeRecortaA255() {
        presenter.setReasonText("a".repeat(400))
        assertEquals(255, presenter.state.value.reasonText.length)
        assertNotNull(presenter.state.value)
    }

    private fun <T> collect(flow: SharedFlow<T>, into: MutableList<T>): Job =
        CoroutineScope(Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) { flow.collect { into += it } }
}
