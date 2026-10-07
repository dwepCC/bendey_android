package com.bendey.restaurant.feature.pos

import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D2.0: como ve el POS la linea "Servicio de delivery". El servidor es quien la agrega y cobra; aqui solo se
 * comprueba la VISTA PREVIA (pedido nuevo sin sesion), que el total de sesion no la cuente dos veces y que la
 * linea no cuente como plato de cocina.
 */
class PosDeliveryFeeTest {
    private val feeOn = DeliverySettings(feeEnabled = true, deliveryFee = 5.0)

    private fun line(price: Double = 20.0) = PosCartLine(
        product = PosProduct(
            id = 1, code = "P1", name = "Lomo", salePrice = price, categoryId = null, imageUrl = null,
            igvAffectationType = "10", priceIncludesIgv = true,
        ),
        quantity = 1,
    )

    private fun comanda(id: Int, name: String, code: String?, price: Double, cancelled: Boolean = false, billed: Int? = null) =
        SessionComandaSummary(
            id = id, productName = name, quantity = 1.0, unitPrice = price, status = ComandaStatus.ENTREGADA, notes = null,
            cancelledAt = if (cancelled) "2026-10-06T10:00:00-05:00" else null, billedSaleId = billed, productCode = code,
        )

    private fun feeLine(cancelled: Boolean = false, billed: Int? = null) =
        comanda(9, "Servicio de delivery", "DELIVERY", 5.0, cancelled, billed)

    @Test fun pedidoDeliveryNuevoMuestraLaTarifaEnVistaPrevia() {
        val s = PosUiState(orderType = PosOrderType.DELIVERY, cart = listOf(line()), deliverySettings = feeOn)
        assertEquals(5.0, s.deliveryFeePreview, 0.0)
        assertEquals(5.0, s.deliveryFeeShown, 0.0)
        assertEquals(25.0, s.checkoutRawTotal, 0.0)
    }

    @Test fun sinTarifaEncendidaNadaCambia() {
        val s = PosUiState(orderType = PosOrderType.DELIVERY, cart = listOf(line()), deliverySettings = feeOn.copy(feeEnabled = false))
        assertEquals(0.0, s.deliveryFeePreview, 0.0)
        assertEquals(20.0, s.checkoutRawTotal, 0.0)
        assertEquals(0.0, PosUiState(orderType = PosOrderType.DELIVERY, cart = listOf(line())).deliveryFeePreview, 0.0)
    }

    @Test fun soloAplicaAPedidosDeliveryConProductos() {
        assertEquals(0.0, PosUiState(orderType = PosOrderType.TAKEAWAY, cart = listOf(line()), deliverySettings = feeOn).deliveryFeePreview, 0.0)
        assertEquals(0.0, PosUiState(orderType = PosOrderType.QUICK_SALE, cart = listOf(line()), deliverySettings = feeOn).deliveryFeePreview, 0.0)
        val vacio = PosUiState(orderType = PosOrderType.DELIVERY, deliverySettings = feeOn)
        assertEquals("carrito vacio: no hay nada que cobrar", 0.0, vacio.checkoutRawTotal, 0.0)
        assertFalse(vacio.canCheckout)
    }

    @Test fun conSesionElTotalDelServidorYaTraeLaTarifaYNoSeSumaOtraVez() {
        val s = PosUiState(
            orderType = PosOrderType.DELIVERY, activeSessionId = 41, sessionTotal = 55.0, deliverySettings = feeOn,
            sessionOrders = listOf(SessionOrderSummary(1, 1, listOf(comanda(1, "Lomo", "P1", 50.0), feeLine()))),
        )
        assertEquals(0.0, s.deliveryFeePreview, 0.0)
        assertEquals(5.0, s.deliveryFeeInSession, 0.0)
        assertEquals(5.0, s.deliveryFeeShown, 0.0)
        assertEquals(55.0, s.checkoutRawTotal, 0.0)
    }

    @Test fun lasLineasAnuladasOCobradasNoCuentan() {
        val anulada = PosUiState(sessionOrders = listOf(SessionOrderSummary(1, 1, listOf(feeLine(cancelled = true)))))
        val cobrada = PosUiState(sessionOrders = listOf(SessionOrderSummary(1, 1, listOf(feeLine(billed = 7)))))
        assertEquals(0.0, anulada.deliveryFeeInSession, 0.0)
        assertEquals(0.0, cobrada.deliveryFeeInSession, 0.0)
    }

    @Test fun laLineaDeTarifaNoEsUnPlatoDeCocina() {
        val s = PosUiState(
            sessionOrders = listOf(
                SessionOrderSummary(1, 1, listOf(comanda(1, "Lomo", "P1", 50.0), feeLine())),
                SessionOrderSummary(2, 2, listOf(feeLine())),
            ),
        )
        assertEquals(1, s.kitchenOrders.size)
        assertEquals(listOf("Lomo"), s.kitchenOrders.single().comandas.map { it.productName })
        assertTrue(s.hasSentComandas)
        assertFalse(PosUiState(sessionOrders = listOf(SessionOrderSummary(2, 2, listOf(feeLine())))).hasSentComandas)
    }

    @Test fun conSoloLaTarifaEnLaSesionSePuedeVaciarElCarrito() {
        val s = PosUiState(cart = listOf(line()), sessionOrders = listOf(SessionOrderSummary(2, 2, listOf(feeLine()))))
        assertTrue(s.canClearCart)
    }
}
