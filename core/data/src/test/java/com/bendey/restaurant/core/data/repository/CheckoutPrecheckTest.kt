package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.BankAccountBrief
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentLine
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.copy.CashCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** El cobro debe validarse ANTES de mandar la comanda a cocina; y al mozo no se le pide "abrir caja". */
class CheckoutPrecheckTest {

    private val cash = PaymentMethodOption(1, "Efectivo", "cash", "cash", null, true)
    private val yape = PaymentMethodOption(2, "Yape", "yape", "bank", 7, true)
    private val tarjetaSinCuenta = PaymentMethodOption(3, "Tarjeta", "card", "bank", null, true)
    private val methods = listOf(cash, yape, tarjetaSinCuenta)
    private val banks = listOf(BankAccountBrief(7, "yape", true))

    private fun pay(code: String) = listOf(CheckoutPaymentLine(code, 10.0))

    private fun check(canCash: Boolean, code: String, openCash: Boolean) =
        checkoutPaymentPrecheckError(canCash, methods, banks, pay(code), openCash)

    @Test
    fun mozoConEfectivoRecibeElMensajeDePermiso_noElDeAbrirCaja() {
        assertEquals(MSG_CASH_NOT_ALLOWED, check(canCash = false, code = "cash", openCash = false))
    }

    @Test
    fun mozoConMetodoNoEfectivoPasa_sinCaja() {
        assertNull(check(canCash = false, code = "yape", openCash = false))
    }

    @Test
    fun cajeroConEfectivoYSinCajaDebeAbrirla() {
        assertEquals(CashCopy.CHECKOUT_NEED_OPEN, check(true, "cash", openCash = false))
    }

    @Test
    fun cajeroConMetodoNoEfectivoPasaSinCaja_reglaUnificadaConTauri() {
        assertNull(check(true, "yape", openCash = false))
    }

    @Test
    fun mozoNuncaVeAbreTuCaja_nadaDelMensajeDeAbrir() {
        val msg = check(canCash = false, code = "cash", openCash = false)
        assertEquals(CashCopy.CHECKOUT_CASH_DISABLED_ROLE, msg)
        assertTrue(!msg!!.contains("Abre tu caja", ignoreCase = true))
    }

    @Test
    fun cajaRequeridaSoloConEfectivo() {
        assertTrue(requiresOpenCashSessionForCheckout(methods, pay("cash")))
        assertFalse(requiresOpenCashSessionForCheckout(methods, pay("yape")))
        assertTrue(requiresOpenCashSessionForCheckout(emptyList(), pay("cash")))
        assertFalse(requiresOpenCashSessionForCheckout(emptyList(), pay("yape")))
        assertTrue(requiresOpenCashSessionForCheckout(methods, pay("yape") + pay("cash")))
    }

    @Test
    fun sesionQueSeManda_cajeroLaMandaSiempreQueLaTenga_otrosSoloSiHayEfectivo() {
        assertEquals(5, cashSessionIdForCheckout(canOperateCash = true, requiresCash = false, openSessionId = 5))
        assertEquals(5, cashSessionIdForCheckout(true, true, 5))
        assertNull(cashSessionIdForCheckout(true, false, null))
        assertNull(cashSessionIdForCheckout(canOperateCash = false, requiresCash = false, openSessionId = 5))
        assertEquals(5, cashSessionIdForCheckout(false, true, 5))
    }

    @Test
    fun cajeroConCajaAbiertaPasa() {
        assertNull(check(true, "cash", openCash = true))
    }

    @Test
    fun metodoSinCuentaVinculadaSeRechaza() {
        assertEquals("El método \"Tarjeta\" no tiene una cuenta vinculada.", check(true, "card", openCash = true))
    }

    @Test
    fun metodoDesconocidoSeRechaza() {
        assertEquals(MSG_METHOD_NOT_CONFIGURED, check(true, "paypal", openCash = true))
    }

    @Test
    fun defaultParaQuienNoPuedeCobrarEfectivoEsElPrimerNoEfectivo() {
        assertEquals("yape", defaultPaymentMethodCode(methods, canOperateCash = false))
        assertEquals("cash", defaultPaymentMethodCode(methods, canOperateCash = true))
        assertEquals("cash", defaultPaymentMethodCode(listOf(cash), canOperateCash = false))
        assertEquals("cash", defaultPaymentMethodCode(emptyList(), canOperateCash = false))
    }
}
