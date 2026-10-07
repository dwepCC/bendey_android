package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.copy.ForbiddenTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2b: lógica pura del efectivo contra entrega (etiquetas, vuelto, cobrar, forzar, formulario del POS). */
class DeliveryPaymentTest {

    private fun cod(
        status: String = "pending_collection",
        expected: Double? = 83.0,
        tendered: Double? = 100.0,
        change: Double? = 17.0,
        insufficient: Boolean = false,
        collectedBy: PaymentCollector? = null,
    ) = SessionPayment(
        mode = "cash_on_delivery", status = status, expectedAmount = expected, tenderedAmount = tendered,
        changeAmount = change, tenderedInsufficient = insufficient, collectedAt = null, collectedBy = collectedBy,
    )

    private fun card(payment: SessionPayment?, assignmentStatus: String? = "on_the_way", assignmentId: Int? = 17) = DeliveryCard(
        sessionId = 41, assignmentId = assignmentId, source = "marketplace", customerName = "Ana", customerPhone = "999",
        address = "Av. Sol", reference = "", itemsCount = 2, totalAmount = 83.0, orderStatus = "ready", createdAt = null,
        assignmentStatus = assignmentStatus, payment = payment,
    )

    // ---- línea de cobro de la tarjeta ----

    @Test fun lineaDeCobroConVuelto() {
        assertEquals("Cobrar S/ 83.00 · Paga con S/ 100.00 · Vuelto S/ 17.00", deliveryPaymentLine(cod()))
    }

    @Test fun lineaDeCobroPagaJusto() {
        assertEquals("Cobrar S/ 83.00 · Paga justo", deliveryPaymentLine(cod(tendered = null, change = null)))
    }

    @Test fun elVueltoSeCalculaSiElServidorNoLoMando() {
        assertEquals("Cobrar S/ 83.00 · Paga con S/ 100.00 · Vuelto S/ 17.00", deliveryPaymentLine(cod(change = null)))
        assertEquals(17.0, paymentChange(cod(change = null))!!, 0.0001)
    }

    @Test fun conElTotalMasAltoQueLoQuePagaNoHayVueltoYSeAvisa() {
        val p = cod(expected = 120.0, tendered = 100.0, change = null, insufficient = true)
        assertEquals("Cobrar S/ 120.00 · Paga con S/ 100.00", deliveryPaymentLine(p))
        assertNull(paymentChange(p))
        assertEquals("El total subió: revisa con cuánto paga", deliveryPaymentWarning(p))
    }

    @Test fun sinAvisoSiElMontoAlcanza() {
        assertEquals("", deliveryPaymentWarning(cod()))
        assertEquals("", deliveryPaymentWarning(null))
    }

    @Test fun cobradoYSinPagoNoMuestranLineaDeCobro() {
        assertEquals("", deliveryPaymentLine(null))
        assertEquals("", deliveryPaymentLine(cod(status = "collected")))
        assertEquals("", deliveryPaymentLine(cod(status = "cancelled")))
        assertEquals("", deliveryPaymentLine(SessionPayment(mode = "manual", status = "pending_collection")))
    }

    @Test fun lineaDeCobradoConHoraYQuien() {
        val p = cod(status = "collected", collectedBy = PaymentCollector(3, "Luis", "driver"))
        assertEquals("14:32 · Luis", deliveryCollectedLine(p, "14:32"))
        assertEquals("Luis", deliveryCollectedLine(p, null))
        assertEquals("", deliveryCollectedLine(cod(), "14:32"))
        assertEquals("", deliveryCollectedLine(null, "14:32"))
    }

    // ---- estado ----

    @Test fun estadosDelPago() {
        assertTrue(cod().isPendingCollection)
        assertTrue(cod(status = "collected").isCollected)
        assertFalse(cod(status = "collected").isPendingCollection)
        assertTrue(cod().isCashOnDelivery)
        assertFalse(SessionPayment(mode = "manual_qr", status = "collected").isCollected)
        assertTrue(cod().isCodPending())
        assertFalse(null.isCodPending())
        assertTrue(cod(status = "collected").isCodCollected())
    }

    // ---- Marcar cobrado ----

    @Test fun marcarCobradoSoloConCobroPendienteYAsignacionRecogidaOEnCamino() {
        assertTrue(canCollectDelivery(cod(), 17, "picked_up"))
        assertTrue(canCollectDelivery(cod(), 17, "on_the_way"))
        assertFalse(canCollectDelivery(cod(), 17, "assigned"))
        assertFalse(canCollectDelivery(cod(), 17, "accepted"))
        assertFalse(canCollectDelivery(cod(), 17, "delivered"))
        assertFalse(canCollectDelivery(cod(), 17, null))
        assertFalse(canCollectDelivery(cod(), null, "on_the_way"))
        assertFalse(canCollectDelivery(cod(status = "collected"), 17, "on_the_way"))
        assertFalse(canCollectDelivery(null, 17, "on_the_way"))
    }

    @Test fun laTarjetaEnCaminoOfreceCobrarAntesQueEntregar() {
        val actions = deliveryCardActions(card(cod()), DeliverySection.IN_TRANSIT, canAssign = true)
        assertEquals(
            listOf(
                DeliveryAction.CALL, DeliveryAction.COLLECT, DeliveryAction.DELIVERED, DeliveryAction.FAILED,
                DeliveryAction.REASSIGN, DeliveryAction.CANCEL,
            ),
            actions,
        )
        assertEquals("Marcar cobrado", deliveryActionLabel(DeliveryAction.COLLECT))
    }

    @Test fun sinPagoOYaCobradoNoHayBotonCobrar() {
        assertFalse(DeliveryAction.COLLECT in deliveryCardActions(card(null), DeliverySection.IN_TRANSIT, true))
        assertFalse(DeliveryAction.COLLECT in deliveryCardActions(card(cod(status = "collected")), DeliverySection.IN_TRANSIT, true))
        // Solo lectura: nunca.
        assertFalse(DeliveryAction.COLLECT in deliveryCardActions(card(cod()), DeliverySection.IN_TRANSIT, false))
    }

    // ---- forzar entrega ----

    @Test fun forzarEntregaSoloCajaOAdministracion() {
        assertTrue(canForceDelivery(listOf("o.ch")))
        assertTrue(canForceDelivery(listOf("s.m")))
        assertFalse(canForceDelivery(listOf("d.u")))
        assertFalse(canForceDelivery(listOf("o.c")))
        assertFalse(canForceDelivery(null))
    }

    @Test fun elMotivoParaForzarVaDe3A255() {
        assertEquals("Escribe el motivo (mínimo 3 letras).", forceReasonError("ab"))
        assertEquals("Escribe el motivo (mínimo 3 letras).", forceReasonError("   "))
        assertNull(forceReasonError("abc"))
        assertNull(forceReasonError("a".repeat(255)))
        assertEquals("Escribe el motivo (mínimo 3 letras).", forceReasonError("a".repeat(256)))
    }

    // ---- POS: "El cliente paga con (S/)" ----

    @Test fun parseaElMontoConComaOPunto() {
        assertEquals(100.0, parseCashTendered("100")!!, 0.0)
        assertEquals(100.5, parseCashTendered("100,5")!!, 0.0)
        assertEquals(20.25, parseCashTendered(" 20.25 ")!!, 0.0)
        assertNull(parseCashTendered(""))
        assertNull(parseCashTendered("0"))
        assertNull(parseCashTendered("abc"))
        assertNull(parseCashTendered("10.123"))
    }

    @Test fun vacioEsPagaJusto() {
        assertEquals(CashTenderedCheck.Exact, checkCashTendered("", 83.0))
        assertEquals(CashTenderedCheck.Exact, checkCashTendered("   ", 83.0))
        assertEquals("", cashTenderedMessage(CashTenderedCheck.Exact))
        assertFalse(CashTenderedCheck.Exact.blocksSaving)
    }

    @Test fun montoQueCubreDaVueltoEstimado() {
        val c = checkCashTendered("100", 83.0)
        assertEquals(CashTenderedCheck.Ok(100.0, 17.0), c)
        assertEquals("Vuelto estimado S/ 17.00", cashChangeEstimate(c))
        assertEquals("", cashTenderedMessage(c))
        assertFalse(c.blocksSaving)
    }

    @Test fun montoIgualAlTotalNoDaVuelto() {
        val c = checkCashTendered("83", 83.0)
        assertEquals(CashTenderedCheck.Ok(83.0, 0.0), c)
        assertEquals("", cashChangeEstimate(c))
    }

    @Test fun montoMenorAlTotalAvisaYBloquea() {
        val c = checkCashTendered("50", 83.0)
        assertEquals(CashTenderedCheck.TooLow(50.0, 83.0), c)
        assertEquals("Debe cubrir el total S/ 83.00", cashTenderedMessage(c))
        assertEquals("", cashChangeEstimate(c))
        assertTrue(c.blocksSaving)
    }

    @Test fun montoIlegibleBloquea() {
        val c = checkCashTendered("1.2.3", 83.0)
        assertEquals(CashTenderedCheck.Invalid, c)
        assertTrue(c.blocksSaving)
        assertTrue(cashTenderedMessage(c).isNotEmpty())
    }

    @Test fun sinTotalConocidoNoSeCompara() {
        assertEquals(CashTenderedCheck.Ok(10.0, 0.0), checkCashTendered("10", 0.0))
    }

    @Test fun unMontoYaGuardadoQueDejoDeCubrirSoloAvisaSinBloquear() {
        val saved = cod(tendered = 50.0, expected = 40.0, change = 10.0)
        val c = checkCashTenderedAgainstSaved("50", 83.0, saved)
        assertEquals(CashTenderedCheck.Stale(50.0, 83.0), c)
        assertFalse(c.blocksSaving)
        assertEquals("El total subió: revisa con cuánto paga", cashTenderedMessage(c))
        // Un monto NUEVO que no cubre sigue bloqueando.
        assertTrue(checkCashTenderedAgainstSaved("60", 83.0, saved).blocksSaving)
        // Sin pago guardado también bloquea.
        assertTrue(checkCashTenderedAgainstSaved("50", 83.0, null).blocksSaving)
    }

    // ---- POS: qué se manda al servidor ----

    @Test fun elFormularioSoloExisteEnDeliveryConContraEntregaEncendido() {
        val on = DeliverySettings(codEnabled = true)
        assertTrue(showsPosPaymentForm(true, on, null))
        assertFalse(showsPosPaymentForm(false, on, null))
        assertFalse(showsPosPaymentForm(true, DeliverySettings(), null))
        assertFalse(showsPosPaymentForm(true, null, null))
        // Apagaron la función pero el pedido ya la tiene: se sigue viendo para poder quitarla.
        assertTrue(showsPosPaymentForm(true, DeliverySettings(), cod()))
    }

    @Test fun seMandaElPagoSoloSiCambio() {
        assertEquals(PaymentSync.SetCod(100.0), paymentSyncNeeded(true, true, 100.0, null))
        assertEquals(PaymentSync.SetCod(null), paymentSyncNeeded(true, true, null, null))
        // El servidor ya tiene lo mismo: no se vuelve a llamar.
        assertNull(paymentSyncNeeded(true, true, 100.0, cod(tendered = 100.0)))
        // Cambió el monto: se vuelve a llamar.
        assertEquals(PaymentSync.SetCod(120.0), paymentSyncNeeded(true, true, 120.0, cod(tendered = 100.0)))
        assertEquals(PaymentSync.SetCod(null), paymentSyncNeeded(true, true, null, cod(tendered = 100.0)))
    }

    @Test fun quitarElPagoSoloSiElServidorLoTienePendiente() {
        assertEquals(PaymentSync.Clear, paymentSyncNeeded(true, false, null, cod()))
        assertNull(paymentSyncNeeded(true, false, null, null))
        assertNull("ya cobrado no se toca", paymentSyncNeeded(true, false, null, cod(status = "collected")))
    }

    @Test fun conElFormularioOcultoNoSeMandaNada() {
        assertNull(paymentSyncNeeded(false, true, 100.0, null))
        assertNull(paymentSyncNeeded(false, false, null, cod()))
    }

    // ---- textos ----

    @Test fun losTextosNuevosNoTienenUstedNiJerga() {
        val keys = DeliveryCopy.entries.keys.dropWhile { it != "chip.cod" }
        assertTrue(keys.isNotEmpty())
        for (k in keys) assertTrue("$k: ${DeliveryCopy.entries[k]}", ForbiddenTerms.find(DeliveryCopy.entries[k]!!).isEmpty())
    }

    @Test fun lasEtiquetasDeLaSpecSonLasExactas() {
        assertEquals("Contra entrega", DeliveryCopy.text("chip.cod"))
        assertEquals("Cobrado", DeliveryCopy.text("chip.collected"))
        assertEquals("Aún no está cobrado", DeliveryCopy.text("force.title"))
        assertEquals("Pago contra entrega", DeliveryCopy.text("cod.title"))
        assertEquals("Permitir pago en efectivo contra entrega", DeliveryCopy.text("cod.switch"))
        assertEquals(
            "Tus clientes del marketplace podrán elegir pagar en efectivo al recibir. El repartidor cobra y tú lo ves en Entregas. " +
                "El registro del efectivo en caja se hará en una próxima actualización; el comprobante lo sigues emitiendo tú.",
            DeliveryCopy.text("cod.help"),
        )
        assertEquals("Pago", DeliveryCopy.text("pos.payment_label"))
        assertEquals("Sin definir", DeliveryCopy.text("pos.payment_none"))
        assertEquals("Efectivo contra entrega", DeliveryCopy.text("pos.payment_cod"))
        assertEquals("El cliente paga con (S/)", DeliveryCopy.text("pos.tendered_label"))
    }
}
