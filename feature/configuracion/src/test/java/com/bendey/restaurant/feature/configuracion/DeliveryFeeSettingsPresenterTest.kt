package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeCopy
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
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

/** D2.0: Ajustes > Operacion > Tarifa de delivery (monto 0 a 999.99, no se enciende con 0, solo lectura sin s.m). */
class DeliveryFeeSettingsPresenterTest {

    private class FakeRepo : DeliveryRepository {
        @Volatile var cached: DeliverySettings? = null
        @Volatile var server: DeliverySettings = DeliverySettings()
        @Volatile var updateResult: AppResult<DeliverySettings>? = null
        @Volatile var getFails = false
        val updates = CopyOnWriteArrayList<DeliverySettingsUpdate>()

        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> =
            if (getFails) AppResult.Error("sin red") else AppResult.Success(server).also { cached = server }

        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> {
            updates += update
            return updateResult ?: run {
                server = server.copy(
                    feeEnabled = update.feeEnabled ?: server.feeEnabled,
                    deliveryFee = update.deliveryFee ?: server.deliveryFee,
                    feeIgvAffectation = update.feeIgvAffectation ?: server.feeIgvAffectation,
                    codEnabled = update.codEnabled ?: server.codEnabled,
                )
                cached = server
                AppResult.Success(server)
            }
        }

        override fun peekDeliverySettings(): DeliverySettings? = cached
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> = AppResult.Success(DeliveryBoardData())
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun collectAssignment(assignmentId: Int): AppResult<SessionPayment?> = AppResult.Success(null)
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
    private fun presenter() = DeliveryFeeSettingsPresenter(scope, repo)

    @After fun tearDown() = scope.cancel()

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun admin(p: DeliveryFeeSettingsPresenter) = p.setPermissions(listOf("s.m"))

    // ---- solo lectura ----

    @Test fun sinSMEsSoloLecturaYNoAbreNiGuarda() {
        val p = presenter()
        p.setPermissions(listOf("o.ch", "d.v"))
        assertFalse(p.state.value.canEdit)
        p.openDialog()
        assertFalse(p.state.value.dialogOpen)
        p.setAmountText("5"); p.setEnabled(true); p.save()
        assertTrue(repo.updates.isEmpty())
        assertEquals("", p.state.value.amountText)
    }

    @Test fun conSMPuedeAbrirElDialogo() {
        val p = presenter(); admin(p)
        p.openDialog()
        assertTrue(p.state.value.dialogOpen)
    }

    // ---- resumen y carga ----

    @Test fun elResumenDiceApagadaOElMontoYLaAfectacion() {
        val p = presenter()
        assertEquals(DeliveryFeeCopy.OFF, p.state.value.summary)
        repo.server = DeliverySettings(feeEnabled = true, deliveryFee = 5.0, feeIgvAffectation = "20")
        p.load()
        await { p.state.value.settings != null }
        assertEquals("S/ 5.00 por pedido · Exonerado", p.state.value.summary)
    }

    @Test fun unaCargaFallidaDejaApagadaSinInventarNada() {
        repo.getFails = true
        val p = presenter()
        p.load()
        await { !p.state.value.loading }
        assertNull(p.state.value.settings)
        assertEquals(DeliveryFeeCopy.OFF, p.state.value.summary)
    }

    @Test fun usaLaCacheCompartidaAlCrearse() {
        repo.cached = DeliverySettings(feeEnabled = true, deliveryFee = 4.0)
        assertEquals("S/ 4.00 por pedido · Gravado con IGV", presenter().state.value.summary)
    }

    // ---- validacion ----

    @Test fun noSeEnciendeConMontoCero() {
        val p = presenter(); admin(p); p.openDialog()
        p.setEnabled(true)
        assertFalse(p.state.value.enabled)
        assertEquals(DeliveryFeeCopy.AMOUNT_REQUIRED_TO_ENABLE, p.state.value.error)
        p.setAmountText("0")
        p.setEnabled(true)
        assertFalse(p.state.value.enabled)
        p.setAmountText("5")
        p.setEnabled(true)
        assertTrue(p.state.value.enabled)
        assertNull(p.state.value.error)
    }

    @Test fun elCampoSoloAceptaDosDecimalesYTresEnteros() {
        val p = presenter(); admin(p)
        p.setAmountText("12a3,456")
        assertEquals("123.45", p.state.value.amountText)
        p.setAmountText("12345")
        assertEquals("123", p.state.value.amountText)
    }

    @Test fun afectacionDesconocidaNoLlamaAlServidor() {
        val p = presenter(); admin(p); p.openDialog()
        p.setAmountText("5")
        p.setAffectation("99")
        p.save()
        assertTrue(repo.updates.isEmpty())
        assertEquals(DeliveryFeeCopy.INVALID_AMOUNT, p.state.value.error)
    }

    @Test fun elMaximoPermitidoSeGuarda() {
        val p = presenter(); admin(p); p.openDialog()
        p.setAmountText("999.99")
        p.save()
        await { !p.state.value.saving && !p.state.value.dialogOpen }
        assertEquals(999.99, repo.updates.single().deliveryFee!!, 0.0)
    }

    // ---- guardar ----

    @Test fun guardarMandaLosTresCamposYCierraConLaRespuesta() {
        val p = presenter(); admin(p); p.openDialog()
        p.setAmountText("6,5")
        p.setEnabled(true)
        p.setAffectation("30")
        p.save()
        await { !p.state.value.saving && !p.state.value.dialogOpen }
        assertEquals(listOf(DeliverySettingsUpdate(true, 6.5, "30")), repo.updates.toList())
        assertEquals("S/ 6.50 por pedido · Inafecto", p.state.value.summary)
        assertNull(p.state.value.error)
    }

    @Test fun unErrorDelServidorSeMuestraDentroYElDialogoSigueAbierto() {
        repo.updateResult = AppResult.Error("Revisa la tarifa: usa un monto entre S/ 0 y S/ 999.99.")
        val p = presenter(); admin(p); p.openDialog()
        p.setAmountText("5"); p.setEnabled(true)
        p.save()
        await { !p.state.value.saving && p.state.value.error != null }
        assertTrue(p.state.value.dialogOpen)
        assertEquals("Revisa la tarifa: usa un monto entre S/ 0 y S/ 999.99.", p.state.value.error)
    }

    @Test fun elDialogoSeAbreConLoQueYaHayGuardado() {
        repo.cached = DeliverySettings(feeEnabled = true, deliveryFee = 5.0, feeIgvAffectation = "20")
        val p = presenter(); admin(p); p.openDialog()
        assertTrue(p.state.value.enabled)
        assertEquals("5.00", p.state.value.amountText)
        assertEquals("20", p.state.value.affectation)
    }
}
