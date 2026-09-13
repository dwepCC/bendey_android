package com.bendey.restaurant.core.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * EL CHECKOUT DE ANDROID NUNCA SUPO DEL RC (2026-09-14): warmCheckoutMeta() se dispara al abrir
 * Pos/Mesa y deja checkoutMeta != null, así que el guard "if (checkoutMeta == null)" de
 * loadCheckoutMeta() nunca corría — ni el checkbox "por consumo" ni el RC se cargaban aunque la
 * sucursal los tuviera activados. Mismos casos que serviceChargePreview.test.ts (Tauri).
 */
class ServiceChargePreviewTest {

    @Test
    fun `reproduce el caso reportado S13 con IGV incluido y RC 5% da 0,55`() {
        val total = 13.0
        val rc = calcServiceChargePreview(
            total = total,
            discountAmount = 0.0,
            rate = 5.0,
            enabled = true,
            taxRatePercent = 18.0,
        )
        assertEquals(0.55, rc, 0.01)
        assertEquals(13.55, total + rc, 0.01)
    }

    @Test
    fun `RC deshabilitado no agrega nada`() {
        val rc = calcServiceChargePreview(100.0, 0.0, rate = 5.0, enabled = false, taxRatePercent = 18.0)
        assertEquals(0.0, rc, 0.0)
    }

    @Test
    fun `tasa en 0 no agrega nada aunque este habilitado`() {
        val rc = calcServiceChargePreview(100.0, 0.0, rate = 0.0, enabled = true, taxRatePercent = 18.0)
        assertEquals(0.0, rc, 0.0)
    }

    @Test
    fun `el descuento reduce la base del RC proporcionalmente`() {
        // Total 118 (100 + 18% IGV), descuento de 11.8 (10% del total). Subtotal neto esperado: 90.
        val rc = calcServiceChargePreview(118.0, discountAmount = 11.8, rate = 10.0, enabled = true, taxRatePercent = 18.0)
        assertEquals(9.0, rc, 0.01)
    }

    @Test
    fun `nunca da un RC negativo aunque el descuento supere el total`() {
        val rc = calcServiceChargePreview(118.0, discountAmount = 200.0, rate = 10.0, enabled = true, taxRatePercent = 18.0)
        assertEquals(0.0, rc, 0.0)
    }

    @Test
    fun `formatServiceChargeLabel muestra el porcentaje configurado`() {
        assertEquals("RC 5%", formatServiceChargeLabel(5.0))
        assertEquals("RC 2.5%", formatServiceChargeLabel(2.5))
    }

    @Test
    fun `formatServiceChargeLabel sin tasa cae al label generico`() {
        assertEquals("Recargo al Consumo", formatServiceChargeLabel(0.0))
    }

    // `checkoutPayableTotal` en PosViewModel/MesaViewModel (el que prellena el monto de pago y
    // valida el cobro) ignoraba el RC — solo `CheckoutDialog` lo sumaba para PINTAR el total. El
    // cajero veía "S/13.55" en el diálogo pero el campo de pago traía "S/13.00": tenía que sumar el
    // RC a mano o el backend rechazaba el cobro por insuficiente. Ver PosViewModel/MesaViewModel
    // `checkoutPayableTotal` y `calcPayableTotalWithServiceCharge`.
    @Test
    fun `calcPayableTotalWithServiceCharge suma el RC al total a cobrar, no solo a lo que se pinta`() {
        val payable = calcPayableTotalWithServiceCharge(
            rawTotal = 13.0,
            mode = CheckoutDiscountMode.PERCENT,
            value = 0.0,
            serviceChargeRate = 5.0,
            serviceChargeEnabled = true,
            taxRatePercent = 18.0,
        )
        assertEquals(13.55, payable, 0.01)
    }

    @Test
    fun `calcPayableTotalWithServiceCharge con RC apagado es igual al total sin descuento`() {
        val payable = calcPayableTotalWithServiceCharge(
            rawTotal = 100.0,
            mode = CheckoutDiscountMode.AMOUNT,
            value = 0.0,
            serviceChargeRate = 5.0,
            serviceChargeEnabled = false,
            taxRatePercent = 18.0,
        )
        assertEquals(100.0, payable, 0.0)
    }

    @Test
    fun `calcPayableTotalWithServiceCharge aplica el descuento antes del RC`() {
        // Total 118 (100 + 18% IGV), descuento 10% (11.8) -> subtotal neto 90, RC 10% = 9.
        // Total a cobrar: 118 - 11.8 + 9 = 115.2.
        val payable = calcPayableTotalWithServiceCharge(
            rawTotal = 118.0,
            mode = CheckoutDiscountMode.PERCENT,
            value = 10.0,
            serviceChargeRate = 10.0,
            serviceChargeEnabled = true,
            taxRatePercent = 18.0,
        )
        assertEquals(115.2, payable, 0.01)
    }
}
