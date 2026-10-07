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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** D2b: Ajustes > Operacion > Pago contra entrega (solo s.m, confirmacion al encender, 503 = queda apagado). */
class CodSettingsPresenterTest {

    private class FakeRepo : DeliveryRepository {
        @Volatile var cached: DeliverySettings? = null
        @Volatile var server: DeliverySettings = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)
        @Volatile var updateError: String? = null
        val updates = CopyOnWriteArrayList<DeliverySettingsUpdate>()

        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> =
            AppResult.Success(server).also { cached = server }

        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> {
            updates += update
            updateError?.let { return AppResult.Error(it) }
            server = server.copy(codEnabled = update.codEnabled ?: server.codEnabled)
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
    private fun presenter() = CodSettingsPresenter(scope, repo)

    @After fun tearDown() = scope.cancel()

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun admin(p: CodSettingsPresenter) {
        p.setPermissions(listOf("s.m"))
        p.load()
        await { p.state.value.settings != null }
    }

    @Test fun sinSMEsSoloLecturaYElInterruptorNoHaceNada() {
        val p = presenter()
        p.setPermissions(listOf("o.ch", "d.v", "d.u"))
        p.load()
        await { p.state.value.settings != null }
        assertFalse(p.state.value.canEdit)
        p.toggle(true)
        assertFalse(p.state.value.confirmOpen)
        p.confirmEnable()
        assertTrue(repo.updates.isEmpty())
        assertFalse(p.state.value.enabled)
    }

    @Test fun empiezaApagadoYElResumenLoDice() {
        val p = presenter(); admin(p)
        assertFalse(p.state.value.enabled)
        assertEquals("Apagado", p.state.value.summary)
    }

    @Test fun encenderPideConfirmacionYNoGuardaTodavia() {
        val p = presenter(); admin(p)
        p.toggle(true)
        assertTrue(p.state.value.confirmOpen)
        assertTrue(repo.updates.isEmpty())
        p.dismissConfirm()
        assertFalse(p.state.value.confirmOpen)
        assertTrue(repo.updates.isEmpty())
        assertFalse(p.state.value.enabled)
    }

    @Test fun confirmarGuardaUnPutParcialSoloConCodEnabled() {
        val p = presenter(); admin(p)
        p.toggle(true)
        p.confirmEnable()
        await { p.state.value.enabled && !p.state.value.saving }
        assertEquals(listOf(DeliverySettingsUpdate(codEnabled = true)), repo.updates.toList())
        assertNull("no pisa la tarifa", repo.updates.single().deliveryFee)
        assertNull(repo.updates.single().feeEnabled)
        assertFalse(p.state.value.confirmOpen)
        assertEquals("Encendido", p.state.value.summary)
        assertTrue("la tarifa del servidor sigue intacta", repo.server.feeEnabled && repo.server.deliveryFee == 5.0)
    }

    @Test fun apagarGuardaDirectoSinConfirmacion() {
        repo.server = repo.server.copy(codEnabled = true)
        val p = presenter(); admin(p)
        assertTrue(p.state.value.enabled)
        p.toggle(false)
        await { !p.state.value.enabled && !p.state.value.saving }
        assertFalse(p.state.value.confirmOpen)
        assertEquals(listOf(DeliverySettingsUpdate(codEnabled = false)), repo.updates.toList())
    }

    @Test fun siElServidorRechazaConUn503ElMensajeSeMuestraYQuedaApagado() {
        repo.updateError = "El pago contra entrega no está disponible en este restaurante."
        val p = presenter(); admin(p)
        p.toggle(true)
        p.confirmEnable()
        await { p.state.value.error != null }
        assertEquals("El pago contra entrega no está disponible en este restaurante.", p.state.value.error)
        assertFalse("sigue apagado", p.state.value.enabled)
        assertFalse(p.state.value.saving)
    }

    @Test fun elErrorSeLimpiaAlVolverAIntentar() {
        repo.updateError = "No disponible."
        val p = presenter(); admin(p)
        p.toggle(true); p.confirmEnable()
        await { p.state.value.error != null }
        p.toggle(true)
        assertNull(p.state.value.error)
        assertTrue(p.state.value.confirmOpen)
    }

    @Test fun tocarElMismoValorNoHaceNada() {
        val p = presenter(); admin(p)
        p.toggle(false)
        assertTrue(repo.updates.isEmpty())
        assertFalse(p.state.value.confirmOpen)
    }
}
