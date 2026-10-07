package com.bendey.restaurant.core.realtime.delivery

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardCounts
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeObservability
import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainHandlerContext
import com.bendey.restaurant.core.realtime.domains.delivery.DeliveryDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** D1: el store del tablero (carga, error, coalescencia de eventos, "no suena en la carga inicial") y el dominio. */
class DeliveryBoardStoreTest {

    private fun card(id: Int) = DeliveryCard(
        sessionId = id, assignmentId = null, source = "staff", customerName = "Ana", customerPhone = "999000111",
        address = "Av. Sol", reference = "", itemsCount = 1, totalAmount = 10.0, orderStatus = "sent_to_kitchen", createdAt = null,
    )

    private fun board(vararg unassigned: Int) = DeliveryBoardData(
        counts = DeliveryBoardCounts(unassigned = unassigned.size),
        unassigned = unassigned.map { card(it) },
    )

    private class FakeRepo : DeliveryRepository {
        val calls = AtomicInteger(0)
        @Volatile var result: AppResult<DeliveryBoardData> = AppResult.Success(DeliveryBoardData())
        @Volatile var latencyMs: Long = 0

        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> {
            calls.incrementAndGet()
            if (latencyMs > 0) delay(latencyMs)
            return result
        }
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> = AppResult.Success(DeliverySettings())
        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> = AppResult.Success(DeliverySettings())
        override fun peekDeliverySettings(): DeliverySettings? = null
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<com.bendey.restaurant.core.domain.delivery.SessionPayment?> = AppResult.Success(null)
        override suspend fun collectAssignment(assignmentId: Int): AppResult<com.bendey.restaurant.core.domain.delivery.SessionPayment?> = AppResult.Success(null)
        override suspend fun listDrivers(): AppResult<List<DeliveryDriver>> = AppResult.Success(emptyList())
        override suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteDriver(id: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun listCompanies(): AppResult<List<DeliveryCompany>> = AppResult.Success(emptyList())
        override suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCompany(id: Int): AppResult<Unit> = AppResult.Success(Unit)
    }

    private suspend fun waitUntil(timeoutMs: Long = 5000, cond: () -> Boolean) =
        kotlinx.coroutines.withTimeout(timeoutMs) { while (!cond()) delay(10) }

    private fun store(repo: FakeRepo, debounceMs: Long = 40): DeliveryBoardStore =
        DeliveryBoardStore(repo, CoroutineScope(SupervisorJob() + Dispatchers.Default), debounceMs).also { it.setEnabled(true) }

    /** Recoge lo que emite `arrivals` mientras corre [block]. */
    private fun collectArrivals(s: DeliveryBoardStore, block: suspend () -> Unit): List<List<Int>> {
        val got = mutableListOf<List<Int>>()
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Unconfined)
            val job = scope.launch { s.arrivals.collect { synchronized(got) { got += it } } }
            block()
            delay(30)
            job.cancel()
        }
        return got
    }

    @Test fun sinPermisoNoConsulta() = runBlocking {
        val repo = FakeRepo()
        val s = DeliveryBoardStore(repo, CoroutineScope(SupervisorJob()), 40)
        s.refresh()
        assertEquals(0, repo.calls.get())
        assertFalse(s.state.value.loaded)
    }

    @Test fun cargaElTableroYCuentaLosPorAsignar() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1, 2, 3)) }
        val s = store(repo)
        s.refresh()
        assertTrue(s.state.value.loaded)
        assertFalse(s.state.value.error)
        assertEquals(3, s.state.value.unassignedCount)
        assertNotNull(s.state.value.fetchedAtMs)
        assertFalse(s.state.value.loading)
    }

    @Test fun laPrimeraCargaNoSuenaPorPedidosQueYaEstaban() {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1, 2)) }
        val s = store(repo).also { it.alertsEnabled = true }
        val got = collectArrivals(s) { s.refresh() }
        assertTrue(got.isEmpty(), "la carga inicial no debe avisar: $got")
    }

    @Test fun unPedidoNuevoDespuesDeLaCargaSiAvisaSoloUnaVez() {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1, 2)) }
        val s = store(repo).also { it.alertsEnabled = true }
        val got = collectArrivals(s) {
            s.refresh()
            repo.result = AppResult.Success(board(1, 2, 3))
            s.refresh()
            // Misma lista otra vez: no es nuevo.
            s.refresh()
        }
        assertEquals(listOf(listOf(3)), got)
    }

    @Test fun sinAlertasHabilitadasNoAvisaPeroElTableroSeActualiza() {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)) }
        val s = store(repo)
        val got = collectArrivals(s) {
            s.refresh()
            repo.result = AppResult.Success(board(1, 2))
            s.refresh()
        }
        assertTrue(got.isEmpty())
        assertEquals(2, s.state.value.unassignedCount)
    }

    @Test fun unPedidoQueSeFueYVuelveSiAvisa() {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)) }
        val s = store(repo).also { it.alertsEnabled = true }
        val got = collectArrivals(s) {
            s.refresh()
            repo.result = AppResult.Success(board())
            s.refresh()
            repo.result = AppResult.Success(board(1))
            s.refresh()
        }
        assertEquals(listOf(listOf(1)), got)
    }

    @Test fun resetOlvidaLoConocidoLaSiguienteCargaVuelveAserSilenciosa() {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)) }
        val s = store(repo).also { it.alertsEnabled = true }
        val got = collectArrivals(s) {
            s.refresh()
            s.reset()
            assertNull(s.state.value.board)
            repo.result = AppResult.Success(board(1, 2))
            s.refresh()
        }
        assertTrue(got.isEmpty(), "tras cambiar de sucursal la primera carga es silenciosa: $got")
    }

    @Test fun unErrorConservaLoQueSeVeYNoSeConfundeConVacio() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1, 2)) }
        val s = store(repo)
        s.refresh()
        repo.result = AppResult.Error("Sin conexión")
        s.refresh()
        assertTrue(s.state.value.error)
        assertEquals("Sin conexión", s.state.value.errorMessage)
        assertEquals(2, s.state.value.unassignedCount, "los datos anteriores se siguen mostrando")
        repo.result = AppResult.Success(board(1))
        s.refresh()
        assertFalse(s.state.value.error)
        assertEquals(1, s.state.value.unassignedCount)
    }

    @Test fun elPrimerFalloSinDatosQuedaComoErrorCargado() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Error("Sin conexión") }
        val s = store(repo)
        s.refresh()
        assertTrue(s.state.value.error)
        assertTrue(s.state.value.loaded)
        assertNull(s.state.value.board)
    }

    @Test fun unaRafagaDeEventosProduceUnaSolaConsulta() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)) }
        val s = store(repo, debounceMs = 60)
        repeat(8) { s.scheduleRefresh() }
        waitUntil { repo.calls.get() >= 1 }
        delay(200) // margen para detectar consultas de más
        assertEquals(1, repo.calls.get(), "ráfaga coalescida en una sola consulta")
        // Otra ráfaga después de la ventana: una consulta más.
        repeat(3) { s.scheduleRefresh() }
        waitUntil { repo.calls.get() >= 2 }
        delay(200)
        assertEquals(2, repo.calls.get())
    }

    @Test fun sinPermisoElEventoNoConsulta() = runBlocking {
        val repo = FakeRepo()
        val s = DeliveryBoardStore(repo, CoroutineScope(SupervisorJob() + Dispatchers.Default), 20)
        s.scheduleRefresh()
        delay(300)
        assertEquals(0, repo.calls.get())
    }

    @Test fun unRefrescoMientrasHayOtroEnCursoSeRepiteUnaSolaVez() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)); latencyMs = 120 }
        val s = store(repo)
        val first = launch(Dispatchers.Default) { s.refresh() }
        delay(30)
        // Tres llamadas mientras la primera sigue en vuelo: comparten UNA repetición.
        s.refresh(); s.refresh(); s.refresh()
        first.join()
        assertEquals(2, repo.calls.get())
    }

    // ---- dominio ----

    private fun event(type: String) = DomainEvent(
        v = 1, id = "e-$type", type = type, tenantId = 1, branchId = 1, occurredAt = "2026-10-05T22:00:00-05:00",
    )

    @Test fun elDominioReconoceLosTresEventosYRefrescaElTablero() = runBlocking {
        val repo = FakeRepo().apply { result = AppResult.Success(board(1)) }
        val s = store(repo, debounceMs = 30)
        val domain = DeliveryDomain(s)
        val handlers = mutableMapOf<String, DomainHandler>()
        domain.registerHandlers { type, h -> handlers[type] = h }
        assertEquals(
            setOf("delivery.board.updated", "delivery.assignment.updated", "delivery.status.updated"),
            handlers.keys,
        )
        assertTrue(domain.matchesEventType("delivery.board.updated"))
        assertFalse(domain.matchesEventType("restaurant.session.opened"))
        assertTrue(domain.getStores().isEmpty())

        var patched: Boolean? = null
        handlers.getValue("delivery.board.updated")(DomainHandlerContext(event("delivery.board.updated")) { patched = it })
        handlers.getValue("delivery.status.updated")(DomainHandlerContext(event("delivery.status.updated")) { })
        waitUntil { repo.calls.get() >= 1 }
        delay(200)
        assertEquals(false, patched)
        assertEquals(1, repo.calls.get(), "dos eventos seguidos = una consulta")
    }
}
