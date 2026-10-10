package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.ApprovedPayment
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.PayMethod
import com.bendey.restaurant.core.domain.delivery.PayMethodInput
import com.bendey.restaurant.core.domain.delivery.PaymentReview
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2a: Ajustes > Operacion > Pago con captura (solo s.m, PUT parcial, codigos de Caja y Bancos, QR despues de guardar). */
class ManualPaymentSettingsPresenterTest {

    private class FakeDelivery : DeliveryRepository {
        @Volatile var server: DeliverySettings = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)
        @Volatile var updateError: String? = null
        val updates = CopyOnWriteArrayList<DeliverySettingsUpdate>()

        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> = AppResult.Success(server)
        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> {
            updates += update
            updateError?.let { return AppResult.Error(it) }
            server = server.copy(
                manualPaymentEnabled = update.manualPaymentEnabled ?: server.manualPaymentEnabled,
                paymentReviewMinutes = update.paymentReviewMinutes ?: server.paymentReviewMinutes,
            )
            return AppResult.Success(server)
        }

        override fun peekDeliverySettings(): DeliverySettings? = null
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

    private class FakeManual : ManualPaymentRepository {
        val methods = CopyOnWriteArrayList<PayMethod>()
        val uploads = CopyOnWriteArrayList<Pair<Int, Int>>()
        @Volatile var saveError: String? = null
        @Volatile var qrError: String? = null
        private var nextId = 1

        override suspend fun payMethods(): AppResult<List<PayMethod>> = AppResult.Success(methods.toList())
        override suspend fun createPayMethod(input: PayMethodInput): AppResult<PayMethod> {
            saveError?.let { return AppResult.Error(it) }
            val m = PayMethod(nextId++, input.methodCode, input.label, input.holder, input.accountNumber, input.cci, input.instructions, "", input.active)
            methods += m
            return AppResult.Success(m)
        }

        override suspend fun updatePayMethod(id: Int, input: PayMethodInput): AppResult<PayMethod> {
            saveError?.let { return AppResult.Error(it) }
            val old = methods.first { it.id == id }
            val m = old.copy(label = input.label, holder = input.holder, accountNumber = input.accountNumber, active = input.active)
            methods[methods.indexOf(old)] = m
            return AppResult.Success(m)
        }

        override suspend fun deletePayMethod(id: Int): AppResult<Unit> {
            methods.removeIf { it.id == id }
            return AppResult.Success(Unit)
        }

        override suspend fun uploadQr(id: Int, bytes: ByteArray, mimeType: String): AppResult<String> {
            uploads += id to bytes.size
            qrError?.let { return AppResult.Error(it) }
            return AppResult.Success("/storage/qr.png")
        }

        override suspend fun paymentsReview(): AppResult<List<PaymentReview>> = AppResult.Success(emptyList())
        override suspend fun approve(paymentId: Int): AppResult<ApprovedPayment> = AppResult.Error("no usado")
        override suspend fun reject(paymentId: Int, reason: String): AppResult<Unit> = AppResult.Error("no usado")
    }

    private val cashbank = listOf(
        CashPaymentMethod(1, "Efectivo", "cash", "cash", null, true),
        CashPaymentMethod(2, "Yape", "yape", "bank", 9, true),
        CashPaymentMethod(3, "Plin", "plin", "bank", 9, true),
        CashPaymentMethod(4, "Tarjeta vieja", "tarjeta", "bank", 9, false),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val delivery = FakeDelivery()
    private val manual = FakeManual()
    private fun presenter() = ManualPaymentSettingsPresenter(scope, delivery, manual) { AppResult.Success(cashbank) }

    @After fun tearDown() = scope.cancel()

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun admin(p: ManualPaymentSettingsPresenter): ManualPaymentSettingsPresenter {
        p.setPermissions(listOf("s.m"))
        p.load()
        await { p.state.value.settings != null && !p.state.value.loading }
        return p
    }

    @Test fun empiezaApagadoYSoloOfreceMediosQueNoSonEfectivoNiInactivos() {
        val p = admin(presenter())
        assertFalse(p.state.value.enabled)
        assertEquals("Apagado", p.state.value.summary)
        assertEquals(listOf("yape", "plin"), p.state.value.cashbankOptions.map { it.code })
    }

    @Test fun sinSMNadaSeGuardaNiSeAbreElFormulario() {
        val p = presenter()
        p.setPermissions(listOf("o.ch"))
        p.load()
        await { p.state.value.settings != null && !p.state.value.loading }
        p.toggle(true)
        p.openCreate()
        assertTrue(delivery.updates.isEmpty())
        assertNull(p.state.value.form)
        assertFalse(p.state.value.canEdit)
    }

    @Test fun elInterruptorEnviaUnPutParcialConSoloManualPaymentEnabled() {
        val p = admin(presenter())
        p.toggle(true)
        await { p.state.value.enabled && !p.state.value.saving }
        assertEquals(listOf(DeliverySettingsUpdate(manualPaymentEnabled = true)), delivery.updates.toList())
        assertTrue(delivery.server.feeEnabled && delivery.server.deliveryFee == 5.0)
    }

    @Test fun unRechazoDelServidorSeMuestraYElInterruptorSigueApagado() {
        delivery.updateError = "El pago con captura no está disponible en este restaurante."
        val p = admin(presenter())
        p.toggle(true)
        await { !p.state.value.saving && p.state.value.error != null }
        assertFalse(p.state.value.enabled)
        assertEquals("El pago con captura no está disponible en este restaurante.", p.state.value.error)
    }

    @Test fun losMinutosFueraDeRangoNoSeEnvian() {
        val p = admin(presenter())
        p.setMinutesText("4")
        p.saveMinutes()
        assertTrue(delivery.updates.isEmpty())
        assertNotNull(p.state.value.error)
        p.setMinutesText("121")
        p.saveMinutes()
        assertTrue(delivery.updates.isEmpty())
    }

    @Test fun losMinutosValidosEnvianSoloPaymentReviewMinutes() {
        val p = admin(presenter())
        p.setMinutesText("30")
        p.saveMinutes()
        await { p.state.value.settings?.paymentReviewMinutes == 30 && !p.state.value.saving }
        assertEquals(listOf(DeliverySettingsUpdate(paymentReviewMinutes = 30)), delivery.updates.toList())
        assertNull(p.state.value.error)
    }

    @Test fun crearUnMedioSinNombreNoLlegaAlServidor() {
        val p = admin(presenter())
        p.openCreate()
        assertEquals("yape", p.state.value.form?.methodCode)
        p.saveForm()
        assertTrue(manual.methods.isEmpty())
        assertNotNull(p.state.value.form?.error)
    }

    @Test fun crearUnMedioConQrLoGuardaYDespuesSubeLaImagen() {
        val p = admin(presenter())
        p.openCreate()
        p.updateForm { it.copy(label = "Yape del local", holder = "Ana", accountNumber = "999888777") }
        p.pickQr(ByteArray(10), "image/png")
        p.saveForm()
        await { p.state.value.form == null }
        assertEquals(1, manual.methods.size)
        assertEquals(listOf(manual.methods[0].id to 10), manual.uploads.toList())
        assertEquals(1, p.state.value.methods.size)
    }

    @Test fun siElQrFallaElMedioQuedaGuardadoYElFormularioAvisa() {
        manual.qrError = "imagen muy pesada"
        val p = admin(presenter())
        p.openCreate()
        p.updateForm { it.copy(label = "Yape") }
        p.pickQr(ByteArray(3), "image/jpeg")
        p.saveForm()
        await { p.state.value.form?.error != null }
        assertEquals(1, manual.methods.size)
        assertTrue(p.state.value.form?.error?.contains("imagen muy pesada") == true)
        assertEquals(manual.methods[0].id, p.state.value.form?.editingId)
    }

    @Test fun alCrearOtroNoSeOfreceElCodigoYaUsado() {
        val p = admin(presenter())
        p.openCreate()
        p.updateForm { it.copy(label = "Yape") }
        p.saveForm()
        await { p.state.value.methods.size == 1 }
        p.openCreate()
        assertEquals("plin", p.state.value.form?.methodCode)
        assertEquals(listOf("plin"), p.state.value.codeOptionsFor(null).map { it.code })
    }

    @Test fun elErrorDelServidorAlGuardarQuedaEnElFormulario() {
        manual.saveError = "Ya ofreces ese medio de pago."
        val p = admin(presenter())
        p.openCreate()
        p.updateForm { it.copy(label = "Yape") }
        p.saveForm()
        await { p.state.value.form?.error != null }
        assertEquals("Ya ofreces ese medio de pago.", p.state.value.form?.error)
        assertFalse(p.state.value.form?.saving == true)
    }

    @Test fun activarYBorrarActualizanLaLista() {
        val p = admin(presenter())
        p.openCreate()
        p.updateForm { it.copy(label = "Yape") }
        p.saveForm()
        await { p.state.value.methods.size == 1 }
        p.toggleActive(p.state.value.methods[0], false)
        await { p.state.value.methods.firstOrNull()?.active == false && !p.state.value.saving }
        p.askDelete(p.state.value.methods[0])
        assertNotNull(p.state.value.deleteTarget)
        p.confirmDelete()
        await { p.state.value.methods.isEmpty() && !p.state.value.saving }
        assertNull(p.state.value.deleteTarget)
    }
}
