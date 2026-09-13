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
}
