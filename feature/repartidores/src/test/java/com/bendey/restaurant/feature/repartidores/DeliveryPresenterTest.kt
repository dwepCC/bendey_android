package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DELIVERY_REASON_OTHER
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeCopy
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.DeliverySection
import com.bendey.restaurant.core.domain.delivery.SessionPayment
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

        /** Cada asignar se anota con su tarifa: `assign:41:3` sin tarifa, `assign:41:3:fee=7.5` con ella. */
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?) =
            act("assign:$sessionId:$driverId" + (deliveryFee?.let { ":fee=$it" } ?: ""))

        @Volatile var settings: DeliverySettings? = null
        /** Lo que devuelve el GET cuando la caché está vacía (peek = null). */
        @Volatile var settingsOnFetch: DeliverySettings = DeliverySettings()
        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> =
            AppResult.Success(settings ?: settingsOnFetch)
        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> = AppResult.Success(DeliverySettings())
        override fun peekDeliverySettings(): DeliverySettings? = settings
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String) = act("cancel:$sessionId:$reason")
        /** D2b: respuestas de `updateAssignmentStatus` en orden (la primera se consume); vacío = `actionResult`. */
        val statusResults = CopyOnWriteArrayList<AppResult<Unit>>()
        @Volatile var collectResult: AppResult<SessionPayment?> = AppResult.Success(null)

        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> {
            val name = "status:$assignmentId:$status:${failedReason.orEmpty()}" + (forceReason?.let { ":force=$it" } ?: "")
            val queued = if (statusResults.isNotEmpty()) statusResults.removeAt(0) else null
            val r = act(name)
            return queued ?: r
        }

        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<SessionPayment?> =
            AppResult.Success(null)

        override suspend fun collectAssignment(assignmentId: Int): AppResult<SessionPayment?> {
            actions += "collect:$assignmentId"
            return collectResult
        }

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

    // ---- tarifa de delivery (D2.0) ----

    private fun cardWithFee(fee: Double?) = card().copy(deliveryFee = fee)
    private fun cashier() = presenter.setPermissions(listOf("d.v", "d.u", "o.ch"))
    private val feeOn = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)

    @Test fun sinTarifaEncendidaNiEnElPedidoLaHojaNoMuestraElCampo() {
        cashier()
        presenter.openAssign(card())
        assertFalse(presenter.state.value.showFeeField)
    }

    @Test fun conLaTarifaEncendidaPrellenaConLaDeAjustes() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(card())
        assertTrue(presenter.state.value.showFeeField)
        assertEquals("5.00", presenter.state.value.feeText)
        assertTrue(presenter.state.value.canEditFee)
    }

    @Test fun laTarifaDelPedidoGanaSobreLaDeAjustes() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(cardWithFee(7.5))
        assertEquals("7.50", presenter.state.value.feeText)
    }

    @Test fun siElPedidoYaTieneTarifaElCampoSeMuestraAunqueLosAjustesEstenApagados() {
        repo.settings = DeliverySettings(feeEnabled = false, deliveryFee = 5.0)
        cashier()
        presenter.openAssign(cardWithFee(7.5))
        assertTrue(presenter.state.value.showFeeField)
    }

    @Test fun sinAjustesEnCacheLosLeeYPrellenaSiNoSeTocoElCampo() {
        cashier()
        repo.settingsOnFetch = feeOn
        presenter.openAssign(card())
        await { presenter.state.value.settings != null }
        assertEquals("5.00", presenter.state.value.feeText)
        assertTrue(presenter.state.value.showFeeField)
    }

    @Test fun sinOChNiSMElCampoSaleDeshabilitadoYNoSeEnviaTarifa() {
        repo.settings = feeOn
        operator()   // solo d.v y d.u
        presenter.openAssign(card())
        assertTrue(presenter.state.value.showFeeField)
        assertFalse(presenter.state.value.canEditFee)
        presenter.setFeeText("9")
        assertEquals("5.00", presenter.state.value.feeText)
        presenter.assign(3)
        settle()
        assertEquals(listOf("assign:41:3"), repo.actions.toList())
    }

    @Test fun elRepartidorNoEditaLaTarifaAunqueTengaOCh() {
        presenter.setPermissions(listOf("d.v", "d.u", "o.ch"), "driver")
        assertFalse(presenter.state.value.canEditFee)
    }

    @Test fun conSMTambienSePuedeEditar() {
        presenter.setPermissions(listOf("d.v", "d.u", "s.m"))
        assertTrue(presenter.state.value.canEditFee)
    }

    @Test fun asignarSinTocarLaTarifaNoLaEnvia() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(cardWithFee(7.5))
        presenter.assign(3)
        settle()
        assertEquals(listOf("assign:41:3"), repo.actions.toList())
    }

    @Test fun asignarCambiandoLaTarifaLaEnviaConElAsignar() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(cardWithFee(7.5))
        presenter.setFeeText("9,25")
        presenter.assign(3)
        settle()
        assertEquals(listOf("assign:41:3:fee=9.25"), repo.actions.toList())
        assertNull(presenter.state.value.dialog)
    }

    @Test fun vaciarElCampoEnviaCeroParaQuitarLaTarifa() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(cardWithFee(7.5))
        presenter.setFeeText("")
        presenter.assign(3)
        settle()
        assertEquals(listOf("assign:41:3:fee=0.0"), repo.actions.toList())
    }

    @Test fun unMontoIlegibleNoLlamaAlServidorYSeAvisaDentroDeLaHoja() {
        repo.settings = feeOn
        cashier()
        presenter.openAssign(card())
        presenter.setFeeText(".")
        presenter.assign(3)
        assertTrue(repo.actions.isEmpty())
        assertEquals(DeliveryFeeCopy.INVALID_AMOUNT, presenter.state.value.dialogError)
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Assign)
    }

    @Test fun unErrorDeTarifaDelServidorSeMuestraDentroYLaHojaSigueAbierta() {
        repo.settings = feeOn
        cashier()
        repo.actionResult = AppResult.Error("No tienes permiso para cambiar la tarifa de delivery.")
        presenter.openAssign(cardWithFee(7.5))
        presenter.setFeeText("9")
        presenter.assign(3)
        settle()
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Assign)
        assertEquals("No tienes permiso para cambiar la tarifa de delivery.", presenter.state.value.dialogError)
        assertEquals("el valor tecleado se conserva", "9", presenter.state.value.feeText)
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

    // ---- efectivo contra entrega (D2b) ----

    private fun codPayment(status: String = "pending_collection") = SessionPayment(
        mode = "cash_on_delivery", status = status, expectedAmount = 83.0, tenderedAmount = 100.0, changeAmount = 17.0,
    )

    private fun codCard(assignmentStatus: String = "on_the_way", payment: SessionPayment? = codPayment()) =
        card().copy(assignmentStatus = assignmentStatus, payment = payment)

    private fun admin() = presenter.setPermissions(listOf("d.v", "d.u", "s.m"))

    @Test fun marcarCobradoAbreElDialogoSoloConCobroPendienteYAsignacionRecogida() {
        operator()
        presenter.openCollect(codCard("assigned"))
        assertNull("aún no recogió", presenter.state.value.dialog)
        presenter.openCollect(codCard(payment = null))
        assertNull("sin pago contra entrega", presenter.state.value.dialog)
        presenter.openCollect(codCard(payment = codPayment("collected")))
        assertNull("ya cobrado", presenter.state.value.dialog)
        presenter.openCollect(codCard("picked_up"))
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Collect)
    }

    @Test fun sinPermisoDeOperarNoSePuedeCobrar() {
        presenter.setPermissions(listOf("d.v"))
        presenter.openCollect(codCard())
        assertNull(presenter.state.value.dialog)
        presenter.confirmCollect()
        assertTrue(repo.actions.isEmpty())
    }

    @Test fun cobrarLlamaAlServidorAvisaYRecarga() {
        operator()
        presenter.openCollect(codCard())
        val msgs = CopyOnWriteArrayList<String>()
        val job = collect(presenter.messages, msgs)
        presenter.confirmCollect()
        settle()
        await { msgs.isNotEmpty() }
        assertEquals(listOf("collect:7"), repo.actions.toList())
        assertNull(presenter.state.value.dialog)
        assertEquals("Pedido marcado como cobrado.", msgs.first())
        assertTrue(repo.boardCalls.get() >= 1)
        job.cancel()
    }

    @Test fun unErrorAlCobrarSeQuedaDentroDelDialogo() {
        operator()
        repo.collectResult = AppResult.Error("Solo puedes cobrar cuando ya recogiste el pedido.", code = "COLLECT_STATUS_INVALID")
        presenter.openCollect(codCard())
        presenter.confirmCollect()
        settle()
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Collect)
        assertEquals("Solo puedes cobrar cuando ya recogiste el pedido.", presenter.state.value.dialogError)
        assertFalse(presenter.state.value.busy)
    }

    @Test fun conCobroPendienteYPermisoDeCajaMarcarEntregadoVaDirectoAlMotivo() {
        cashier()
        presenter.openDelivered(codCard())
        assertTrue(presenter.state.value.dialog is DeliveryDialog.ForceDelivered)
        presenter.dismissDialog()
        admin()
        presenter.openDelivered(codCard())
        assertTrue(presenter.state.value.dialog is DeliveryDialog.ForceDelivered)
    }

    @Test fun sinPermisoParaForzarSeAbreElDialogoNormal() {
        operator()
        assertFalse(presenter.state.value.canForce)
        presenter.openDelivered(codCard())
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Delivered)
    }

    @Test fun forzarExigeMotivoDe3A255YLoEnviaComoForceReason() {
        cashier()
        presenter.openDelivered(codCard())
        presenter.confirmForceDelivered()
        assertTrue(presenter.state.value.reasonShowError)
        assertTrue(repo.actions.isEmpty())
        presenter.setReasonText("ab")
        presenter.confirmForceDelivered()
        assertTrue(repo.actions.isEmpty())
        presenter.setReasonText("  El cliente pagó en caja  ")
        presenter.confirmForceDelivered()
        settle()
        await { presenter.state.value.dialog == null }
        assertEquals(listOf("status:7:delivered::force=El cliente pagó en caja"), repo.actions.toList())
    }

    @Test fun elServidorPideCobroYConPermisoSeReintentaConMotivo() {
        cashier()
        // La tarjeta no traía el pago (p. ej. tablero viejo): el servidor responde COLLECTION_REQUIRED.
        repo.statusResults += AppResult.Error("Primero marca «Cobrado» antes de marcar la entrega.", code = "COLLECTION_REQUIRED")
        presenter.openDelivered(codCard(payment = null))
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Delivered)
        presenter.confirmDelivered()
        await { presenter.state.value.dialog is DeliveryDialog.ForceDelivered }
        assertNull("el aviso no se queda pegado", presenter.state.value.dialogError)
        presenter.setReasonText("Pagó por Yape")
        presenter.confirmForceDelivered()
        await { presenter.state.value.dialog == null && !presenter.state.value.busy }
        assertEquals(
            listOf("status:7:delivered:", "status:7:delivered::force=Pagó por Yape"),
            repo.actions.toList(),
        )
    }

    @Test fun sinPermisoParaForzarSoloSeMuestraElMensajeDelCatalogo() {
        operator()
        repo.statusResults += AppResult.Error("Primero marca «Cobrado» antes de marcar la entrega.", code = "COLLECTION_REQUIRED")
        presenter.openDelivered(codCard(payment = null))
        presenter.confirmDelivered()
        settle()
        assertTrue("sigue el diálogo normal", presenter.state.value.dialog is DeliveryDialog.Delivered)
        assertEquals("Primero marca «Cobrado» antes de marcar la entrega.", presenter.state.value.dialogError)
    }

    @Test fun elRepartidorNuncaFuerza() {
        presenter.setPermissions(listOf("d.v", "d.u", "o.ch", "s.m"), "driver")
        assertFalse(presenter.state.value.canForce)
    }

    @Test fun cancelarConPagoYaCobradoMuestraElErrorDentroSinCerrar() {
        operator()
        repo.actionResult = AppResult.Error("Este pedido ya no admite cambios en el pago.", code = "PAYMENT_NOT_EDITABLE")
        presenter.openCancel(codCard(payment = codPayment("collected")))
        presenter.chooseReason("El cliente canceló")
        presenter.confirmCancel()
        settle()
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Cancel)
        assertEquals("Este pedido ya no admite cambios en el pago.", presenter.state.value.dialogError)
    }

    @Test fun conContraEntregaApagadoTodoSigueComoHoy() {
        operator()
        // Sin `payment` en la tarjeta: "Marcar entregado" abre el diálogo normal y entrega sin tocar nada de cobro.
        presenter.openDelivered(card().copy(assignmentStatus = "on_the_way"))
        assertTrue(presenter.state.value.dialog is DeliveryDialog.Delivered)
        presenter.confirmDelivered()
        settle()
        await { presenter.state.value.dialog == null }
        assertEquals(listOf("status:7:delivered:"), repo.actions.toList())
        presenter.openCollect(card().copy(assignmentStatus = "on_the_way"))
        assertNull(presenter.state.value.dialog)
    }

    private fun <T> collect(flow: SharedFlow<T>, into: MutableList<T>): Job =
        CoroutineScope(Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) { flow.collect { into += it } }
}
