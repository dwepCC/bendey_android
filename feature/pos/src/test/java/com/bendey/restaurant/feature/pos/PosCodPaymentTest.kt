package com.bendey.restaurant.feature.pos

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.CashTenderedCheck
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * D2b: el selector "Pago" del POS (solo con `cod_enabled`), el vuelto estimado, el aviso de "no cubre el total" y
 * que el pago se guarda con `PUT /sessions/:id/payment` DESPUÉS de crear la sesión (y solo si hace falta).
 */
class PosCodPaymentTest {
    private val codOn = DeliverySettings(codEnabled = true)

    private fun line(price: Double = 83.0) = PosCartLine(
        product = PosProduct(
            id = 1, code = "P1", name = "Lomo", salePrice = price, categoryId = null, imageUrl = null,
            igvAffectationType = "10", priceIncludesIgv = true,
        ),
        quantity = 1,
    )

    private fun state(
        cod: Boolean = true,
        tendered: String = "",
        settings: DeliverySettings? = codOn,
        type: PosOrderType = PosOrderType.DELIVERY,
        saved: SessionPayment? = null,
        cart: List<PosCartLine> = listOf(line()),
    ) = PosUiState(
        orderType = type, cart = cart, deliverySettings = settings, sessionPayment = saved,
        orderDetails = PosOrderDetails(paymentCod = cod, cashTendered = tendered),
    )

    private fun savedCod(tendered: Double? = 100.0, expected: Double = 83.0) = SessionPayment(
        mode = "cash_on_delivery", status = "pending_collection", expectedAmount = expected, tenderedAmount = tendered,
    )

    // ---- selector ----

    @Test fun elSelectorSoloApareceEnDeliveryConContraEntregaEncendido() {
        assertTrue(state().showPaymentForm)
        assertFalse(state(settings = DeliverySettings()).showPaymentForm)
        assertFalse(state(settings = null).showPaymentForm)
        assertFalse(state(type = PosOrderType.TAKEAWAY).showPaymentForm)
        assertFalse(state(type = PosOrderType.QUICK_SALE).showPaymentForm)
    }

    @Test fun conContraEntregaApagadoNadaCambiaYNadaBloquea() {
        val s = state(settings = DeliverySettings(), cod = true, tendered = "5")
        assertFalse(s.showPaymentForm)
        assertNull("sin formulario no hay aviso que bloquee", s.paymentBlockingMessage)
    }

    // ---- vuelto y aviso ----

    @Test fun conMontoQueCubreHayVueltoYNoHayAviso() {
        val s = state(tendered = "100")
        assertEquals(CashTenderedCheck.Ok(100.0, 17.0), s.cashTenderedCheck)
        assertNull(s.paymentBlockingMessage)
    }

    @Test fun campoVacioEsPagaJusto() {
        val s = state(tendered = "")
        assertEquals(CashTenderedCheck.Exact, s.cashTenderedCheck)
        assertNull(s.paymentBlockingMessage)
    }

    @Test fun montoMenorAlTotalAvisaDebeCubrirYBloquea() {
        val s = state(tendered = "50")
        assertEquals("Debe cubrir el total S/ 83.00", s.paymentBlockingMessage)
    }

    @Test fun sinContraEntregaElegidoUnMontoSueltoNoBloquea() {
        assertNull(state(cod = false, tendered = "5").paymentBlockingMessage)
    }

    @Test fun elTotalIncluyeLaTarifaDeLaVistaPrevia() {
        // 83 + tarifa 5 = 88: pagar con 85 ya no alcanza.
        val s = state(tendered = "85", settings = codOn.copy(feeEnabled = true, deliveryFee = 5.0))
        assertEquals("Debe cubrir el total S/ 88.00", s.paymentBlockingMessage)
    }

    @Test fun siElTotalSubioDespuesDeGuardarSoloSeAvisaYNoSeBloquea() {
        val s = state(tendered = "100", saved = savedCod(100.0, 83.0), cart = listOf(line(150.0)))
        assertTrue(s.cashTenderedCheck is CashTenderedCheck.Stale)
        assertNull(s.paymentBlockingMessage)
    }

    // ---- llamada tras crear la sesión ----

    private class FakeRepo : DeliveryRepository {
        data class Call(val sessionId: Int, val mode: String, val tendered: Double?)

        val calls = CopyOnWriteArrayList<Call>()
        @Volatile var result: AppResult<SessionPayment?> = AppResult.Success(null)

        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<SessionPayment?> {
            calls += Call(sessionId, mode, cashTendered)
            return result
        }

        override suspend fun collectAssignment(assignmentId: Int): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun listDrivers(): AppResult<List<DeliveryDriver>> = AppResult.Success(emptyList())
        override suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteDriver(id: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> = AppResult.Success(DeliveryBoardData())
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> = AppResult.Success(DeliverySettings())
        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> = AppResult.Success(DeliverySettings())
        override fun peekDeliverySettings(): DeliverySettings? = null
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun listCompanies(): AppResult<List<DeliveryCompany>> = AppResult.Success(emptyList())
        override suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCompany(id: Int): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val repo = FakeRepo()
    private val syncer = SessionPaymentSyncer(repo)

    private fun sync(s: PosUiState, sessionId: Int = 41, current: SessionPayment? = s.sessionPayment) = runBlocking {
        syncer.sync(
            sessionId = sessionId,
            isDelivery = s.orderType == PosOrderType.DELIVERY,
            settings = s.deliverySettings,
            current = current,
            codSelected = s.orderDetails.paymentCod,
            tenderedCheck = s.cashTenderedCheck,
        )
    }

    @Test fun conContraEntregaElegidoSeGuardaConElIdDeLaSesionYElMonto() {
        val r = sync(state(tendered = "100"), sessionId = 41)
        assertEquals(listOf(FakeRepo.Call(41, "cash_on_delivery", 100.0)), repo.calls.toList())
        assertTrue(r is AppResult.Success)
    }

    @Test fun campoVacioSeGuardaSinMontoPagaJusto() {
        sync(state(tendered = ""))
        assertEquals(listOf(FakeRepo.Call(41, "cash_on_delivery", null)), repo.calls.toList())
    }

    @Test fun siElServidorYaTieneLoMismoNoSeVuelveALlamar() {
        assertNull(sync(state(tendered = "100", saved = savedCod(100.0))))
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun siCambiaElMontoSeVuelveALlamar() {
        sync(state(tendered = "120", saved = savedCod(100.0)))
        assertEquals(listOf(FakeRepo.Call(41, "cash_on_delivery", 120.0)), repo.calls.toList())
    }

    @Test fun volverASinDefinirQuitaElPagoPendiente() {
        sync(state(cod = false, saved = savedCod()))
        assertEquals(listOf(FakeRepo.Call(41, "none", null)), repo.calls.toList())
    }

    @Test fun conContraEntregaApagadoNoSeHaceNingunaLlamada() {
        assertNull(sync(state(settings = DeliverySettings(), tendered = "100")))
        assertNull(sync(state(settings = null)))
        assertNull(sync(state(type = PosOrderType.TAKEAWAY)))
        assertNull(sync(state(cod = false)))
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun unErrorDelServidorSeDevuelveTraducido() {
        repo.result = AppResult.Error("El pago contra entrega no está disponible en este restaurante.", code = "PAYMENT_MODE_NOT_AVAILABLE")
        val r = sync(state(tendered = "100"))
        assertEquals("El pago contra entrega no está disponible en este restaurante.", (r as AppResult.Error).message)
    }
}
