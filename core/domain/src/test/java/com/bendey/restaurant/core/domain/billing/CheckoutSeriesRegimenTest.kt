package com.bendey.restaurant.core.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * UN NRUS NO EMITE FACTURA, Y NO PUEDE ENTERARSE AL FINAL.
 *
 * La regla existe en el backend desde siempre, pero recien actua al reservar el correlativo: para
 * entonces el mozo ya eligio el tipo, cargo al cliente y cobro. Al tenant 10457957781 le paso peor:
 * alguien le habia borrado la serie de boleta, asi que lo UNICO que el checkout le ofrecia era
 * justamente lo que no podia emitir, y el dueno concluyo que el sistema estaba al reves.
 */
class CheckoutSeriesRegimenTest {

    private fun serie(
        id: Int,
        docType: String = "FACTURA",
        series: String = "F001",
        sunatCode: String = "01",
        regimeBlocked: Boolean = false,
    ) = DocumentSeries(
        id = id,
        branchId = 1,
        docType = docType,
        series = series,
        category = "venta",
        sunatCode = sunatCode,
        active = true,
        regimeBlocked = regimeBlocked,
    )

    @Test
    fun `deja fuera la serie que el regimen del tenant prohibe`() {
        val lista = listOf(
            serie(1, regimeBlocked = true),
            serie(2, docType = "BOLETA", series = "B001", sunatCode = "03"),
            serie(3, docType = "NOTA DE VENTA", series = "NV001", sunatCode = "00"),
        )

        assertEquals(listOf(2, 3), filterRestaurantCheckoutSeries(lista).map { it.id })
    }

    @Test
    fun `sigue ofreciendo la factura cuando el regimen no la bloquea`() {
        val lista = listOf(serie(1), serie(2, docType = "BOLETA", series = "B001", sunatCode = "03"))

        assertEquals(listOf(1, 2), filterRestaurantCheckoutSeries(lista).map { it.id })
    }
}
