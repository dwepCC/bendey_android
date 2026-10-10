package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.delivery.ApprovedPayment
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.PayMethod
import com.bendey.restaurant.core.domain.delivery.PayMethodInput
import com.bendey.restaurant.core.domain.delivery.PaymentReview
import com.bendey.restaurant.core.domain.model.AppResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

/** D2a: bandeja Pagos por validar (validar con confirmacion, rechazar con motivo, refresco silencioso). */
class PagosPresenterTest {

    private fun review(id: Int, code: String = "A$id", doc: String = "") = PaymentReview(
        paymentId = id, sessionId = id + 100, orderCode = code, orderType = "delivery", customerName = "Ana", customerPhone = "999",
        amount = 25.5, methodCode = "yape", methodLabel = "Yape", proofUrl = "/storage/p$id.jpg", operationNumber = "123",
        docKind = doc, clientDocNumber = "", clientName = "", attempts = 1, expiresAtMs = null,
    )

    private class FakeRepo(var items: List<PaymentReview>) : ManualPaymentRepository {
        val approved = CopyOnWriteArrayList<Int>()
        val rejected = CopyOnWriteArrayList<Pair<Int, String>>()
        @Volatile var approveError: String? = null
        @Volatile var rejectError: String? = null
        @Volatile var loadError: String? = null

        override suspend fun payMethods(): AppResult<List<PayMethod>> = AppResult.Success(emptyList())
        override suspend fun createPayMethod(input: PayMethodInput): AppResult<PayMethod> = AppResult.Error("no usado")
        override suspend fun updatePayMethod(id: Int, input: PayMethodInput): AppResult<PayMethod> = AppResult.Error("no usado")
        override suspend fun deletePayMethod(id: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun uploadQr(id: Int, bytes: ByteArray, mimeType: String): AppResult<String> = AppResult.Success("")
        override suspend fun paymentsReview(): AppResult<List<PaymentReview>> =
            loadError?.let { AppResult.Error(it) } ?: AppResult.Success(items)

        override suspend fun approve(paymentId: Int): AppResult<ApprovedPayment> {
            approveError?.let { return AppResult.Error(it) }
            approved += paymentId
            items = items.filter { it.paymentId != paymentId }
            return AppResult.Success(ApprovedPayment("A$paymentId", paymentId + 100, "999", "NV01-5", null))
        }

        override suspend fun reject(paymentId: Int, reason: String): AppResult<Unit> {
            rejectError?.let { return AppResult.Error(it) }
            rejected += paymentId to reason
            items = items.filter { it.paymentId != paymentId }
            return AppResult.Success(Unit)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val printed = CopyOnWriteArrayList<SalePrintData?>()
    private val repo = FakeRepo(listOf(review(1), review(2, doc = "factura")))
    private val messages = CopyOnWriteArrayList<String>()

    private fun presenter(printNote: String? = null): PagosPresenter {
        val p = PagosPresenter(scope, repo) { data ->
            printed += data
            printNote
        }
        scope.launch { p.messages.collect { messages += it } }
        return p
    }

    @After fun tearDown() = scope.cancel()

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun loaded(p: PagosPresenter): PagosPresenter {
        p.refresh()
        await { !p.state.value.loading && p.state.value.items.isNotEmpty() }
        return p
    }

    @Test fun cargaLaBandejaYCuentaLosPendientes() {
        val p = loaded(presenter())
        assertEquals(2, p.state.value.pendingCount)
        assertTrue(p.state.value.items[1].wantsDocument)
        assertEquals("Factura", p.state.value.items[1].docLabel)
    }

    @Test fun validarPideConfirmacionYNoLlamaAlServidorHastaConfirmar() {
        val p = loaded(presenter())
        p.askApprove(p.state.value.items[0])
        assertNotNull(p.state.value.approve)
        assertTrue(repo.approved.isEmpty())
        p.dismissApprove()
        assertNull(p.state.value.approve)
        assertTrue(repo.approved.isEmpty())
    }

    @Test fun confirmarValidaImprimeYRefrescaLaLista() {
        val p = loaded(presenter(printNote = "Comprobante impreso."))
        p.askApprove(p.state.value.items[0])
        p.confirmApprove()
        await { repo.approved.isNotEmpty() && p.state.value.pendingCount == 1 && !p.state.value.approveBusy }
        assertEquals(listOf(1), repo.approved.toList())
        assertEquals(1, printed.size)
        assertNull(p.state.value.approve)
        await { messages.isNotEmpty() }
        assertTrue(messages[0].contains("#A1") && messages[0].contains("NV01-5") && messages[0].contains("Comprobante impreso."))
    }

    @Test fun siElPagoYaNoEstabaPorValidarSeRefrescaYSeMuestraElError() {
        val p = loaded(presenter())
        repo.approveError = "Este pago ya fue validado o rechazado. Actualiza la lista."
        repo.items = listOf(review(2))
        p.askApprove(p.state.value.items[0])
        p.confirmApprove()
        await { p.state.value.pendingCount == 1 && !p.state.value.approveBusy }
        assertNull(p.state.value.approve)
        assertTrue(printed.isEmpty())
    }

    @Test fun rechazarExigeMotivoDeAlMenosTresLetras() {
        val p = loaded(presenter())
        p.openReject(p.state.value.items[0])
        p.setRejectReason("ab")
        p.confirmReject()
        assertTrue(repo.rejected.isEmpty())
        assertNotNull(p.state.value.reject?.error)
    }

    @Test fun rechazarConMotivoLoEnviaRecortadoYQuitaElPagoDeLaLista() {
        val p = loaded(presenter())
        p.openReject(p.state.value.items[0])
        p.setRejectReason("  El monto no coincide  ")
        p.confirmReject()
        await { repo.rejected.isNotEmpty() && p.state.value.pendingCount == 1 }
        assertEquals(listOf(1 to "El monto no coincide"), repo.rejected.toList())
        assertNull(p.state.value.reject)
    }

    @Test fun unErrorAlRechazarQuedaEnElDialogo() {
        val p = loaded(presenter())
        repo.rejectError = "Indica por qué rechazas el pago (mínimo 3 caracteres)."
        p.openReject(p.state.value.items[0])
        p.setRejectReason("no llego")
        p.confirmReject()
        await { p.state.value.reject?.error != null }
        assertFalse(p.state.value.reject?.busy == true)
    }

    @Test fun laCapturaSeAbreYSeCierra() {
        val p = loaded(presenter())
        p.openProof(p.state.value.items[0])
        assertEquals(1, p.state.value.viewer?.paymentId)
        p.closeProof()
        assertNull(p.state.value.viewer)
    }

    @Test fun siOtroCajeroAtiendeElPagoAbiertoSeCierranSusDialogos() {
        val p = loaded(presenter())
        p.openProof(p.state.value.items[0])
        p.askApprove(p.state.value.items[0])
        repo.items = listOf(review(2))
        p.refresh()
        await { p.state.value.pendingCount == 1 }
        assertNull(p.state.value.viewer)
        assertNull(p.state.value.approve)
    }

    @Test fun sinPagosPendientesLaListaQuedaVacia() {
        repo.items = emptyList()
        val p = presenter()
        p.refresh()
        await { !p.state.value.loading }
        assertEquals(0, p.state.value.pendingCount)
        assertNull(p.state.value.loadError)
    }

    @Test fun unFalloAlCargarSeInformaSinPerderLoQueYaHabia() {
        val p = loaded(presenter())
        repo.loadError = "Sin conexión"
        p.refresh()
        await { p.state.value.loadError != null }
        assertEquals(2, p.state.value.items.size)
    }
}
