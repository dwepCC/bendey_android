package com.bendey.restaurant.core.domain.sales

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EL BOTON Y LA API TIENEN QUE DECIR LO MISMO.
 *
 * `refundable` lo decide el backend cruzando caja y banco; esta regla solo agrega que la venta este
 * fuera de circulacion. Si se relajara, el cajero veria una opcion que siempre rebota; si se
 * endureciera de mas, no podria entregar plata que si tiene que salir.
 */
class SaleRefundRulesTest {

    private fun venta(
        status: String,
        billingStatus: String?,
        refundable: Boolean,
        refundedAmount: Double = 0.0,
    ) = SaleSummary(
        id = 1,
        docType = "BOLETA",
        number = "B001-00000001",
        issueDate = "2026-08-15",
        contactName = null,
        total = 500.0,
        currency = "PEN",
        status = status,
        billingStatus = billingStatus,
        paymentMethod = "cash",
        refundable = refundable,
        refundableAmount = if (refundable) 500.0 else 0.0,
        refundedAmount = refundedAmount,
    )

    @Test
    fun `ofrece la devolucion de una venta anulada con cobro`() {
        assertTrue(venta("cancelled", "accepted", refundable = true).canRegisterRefund())
    }

    @Test
    fun `ofrece la devolucion de una venta dada de baja ante SUNAT`() {
        assertTrue(venta("paid", "voided", refundable = true).canRegisterRefund())
    }

    @Test
    fun `no la ofrece si el backend dice que no queda nada por devolver`() {
        assertFalse(venta("cancelled", "accepted", refundable = false).canRegisterRefund())
    }

    @Test
    fun `no la ofrece sobre una venta vigente porque eso seria un descuadre`() {
        assertFalse(venta("paid", "accepted", refundable = true).canRegisterRefund())
    }

    @Test
    fun `distingue una venta ya devuelta de uno que solo fue anulada`() {
        assertTrue(venta("cancelled", "accepted", refundable = false, refundedAmount = 500.0).isRefunded())
        assertFalse(venta("cancelled", "accepted", refundable = true).isRefunded())
    }
}
