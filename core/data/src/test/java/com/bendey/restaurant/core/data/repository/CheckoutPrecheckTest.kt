package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.BankAccountBrief
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentLine
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("Abre tu caja para cobrar en efectivo", check(true, "cash", openCash = false))
    }

    @Test
    fun cajeroConCualquierMetodoYSinCajaDebeAbrirla_reglaVigenteDeAndroid() {
        assertEquals("Abre tu caja para cobrar", check(true, "yape", openCash = false))
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
