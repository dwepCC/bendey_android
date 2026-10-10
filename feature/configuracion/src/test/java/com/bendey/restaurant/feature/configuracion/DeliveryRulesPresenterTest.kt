package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.domain.model.AppResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D3: Ajustes > Operación > Pedido mínimo y tiempo de entrega. */
class DeliveryRulesPresenterTest {
    private class FakeRepo : DeliveryRepository {
        @Volatile var cached: DeliverySettings? = null
        @Volatile var server = DeliverySettings(feeEnabled = true, deliveryFee = 5.0, minOrderAmount = 20.0, etaMinutes = 45)
        @Volatile var updateError: String? = null
        val updates = CopyOnWriteArrayList<DeliverySettingsUpdate>()

        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> =
            AppResult.Success(server).also { cached = server }

        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> {
            updates += update
            updateError?.let { return AppResult.Error(it) }
            server = server.copy(minOrderAmount = update.minOrderAmount ?: server.minOrderAmount, etaMinutes = update.etaMinutes ?: server.etaMinutes)
            cached = server
            return AppResult.Success(server)
        }

        override fun peekDeliverySettings(): DeliverySettings? = cached
        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun collectAssignment(assignmentId: Int): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> = AppResult.Success(DeliveryBoardData())
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> = AppResult.Success(Unit)
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
    private fun presenter() = DeliveryRulesPresenter(scope, repo)

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun admin(p: DeliveryRulesPresenter) {
        p.setPermissions(listOf("s.m"))
        p.load()
        await { p.state.value.settings != null }
    }

    @Test fun cargaLosValoresDelServidor() {
        val p = presenter()
        admin(p)
        assertEquals("20.00", p.state.value.minOrderText)
        assertEquals("45", p.state.value.etaText)
        assertTrue(p.state.value.summary.contains("S/ 20.00"))
    }

    @Test fun sinSMNoGuarda() {
        val p = presenter()
        p.setPermissions(listOf("o.ch"))
        p.load()
        await { p.state.value.settings != null }
        p.setEtaText("30")
        p.save()
        assertTrue(repo.updates.isEmpty())
    }

    @Test fun guardaParcialSoloLoQueCambio() {
        val p = presenter()
        admin(p)
        p.setEtaText("30")
        p.save()
        await { repo.updates.size == 1 && !p.state.value.saving }
        assertNull(repo.updates[0].minOrderAmount)
        assertEquals(30, repo.updates[0].etaMinutes)
        assertEquals(30, p.state.value.settings?.etaMinutes)
    }

    @Test fun validaLocalAntesDeEnviar() {
        val p = presenter()
        admin(p)
        p.setEtaText("999")
        p.save()
        assertNotNull(p.state.value.error)
        p.setEtaText("30")
        p.setMinOrderText("10000")
        p.save()
        assertNotNull(p.state.value.error)
        assertTrue(repo.updates.isEmpty())
    }

    @Test fun errorDelServidorSeMuestra() {
        repo.updateError = "Las reglas de delivery aún no están disponibles para este restaurante; intenta de nuevo en unos minutos."
        val p = presenter()
        admin(p)
        p.setMinOrderText("5")
        p.save()
        await { p.state.value.error != null }
        assertEquals(repo.updateError, p.state.value.error)
        assertEquals(20.0, p.state.value.settings?.minOrderAmount ?: 0.0, 0.0)
    }
}
