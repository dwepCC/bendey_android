package com.bendey.restaurant.core.domain.sales

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipoDeAtencionTest {

    @Test
    fun `usa las mismas palabras que el POS`() {
        assertEquals("Mesa", etiquetaDeAtencion("dine_in"))
        assertEquals("Para llevar", etiquetaDeAtencion("takeaway"))
        assertEquals("Delivery", etiquetaDeAtencion("delivery"))
        assertEquals("Directa", etiquetaDeAtencion("quick_sale"))
    }

    // Una venta registrada desde el modulo de ventas no paso por comanda: para el dueno es una venta
    // directa, no un dato faltante.
    @Test
    fun `la venta que no salio del POS es directa`() {
        assertEquals("Directa", etiquetaDeAtencion(null))
        assertEquals("Directa", etiquetaDeAtencion(""))
        assertEquals("Directa", etiquetaDeAtencion("   "))
    }

    // Peor que un nombre raro es una fila que miente diciendo «Directa» cuando fue otra cosa.
    @Test
    fun `no disfraza un tipo desconocido`() {
        assertEquals("catering", etiquetaDeAtencion("catering"))
    }

    @Test
    fun `la mesa acompana al tipo cuando la hay`() {
        assertEquals("Mesa · Mesa 7", atencionConMesa("dine_in", "Mesa 7"))
        assertEquals("Delivery", atencionConMesa("delivery", null))
        assertEquals("Delivery", atencionConMesa("delivery", "   "))
    }
}

class VentasTabTest {

    // «Todas» son las VENTAS: la nota de credito no es una venta sino su anulacion, y mezclarla
    // haria que el total del listado no cuadre con lo que entro a caja.
    @Test
    fun `todas trae las ventas y deja fuera la nota de credito`() {
        assertEquals("00,01,03", VentasTab.TODAS.sunatCodes)
        assertEquals("", VentasTab.CREDITOS.sunatCodes)
    }

    @Test
    fun `cada filtro pide los codigos que le tocan`() {
        assertEquals("00", VentasTab.NOTAS.sunatCodes)
        assertEquals("03", VentasTab.BOLETAS.sunatCodes)
        assertEquals("01", VentasTab.FACTURAS.sunatCodes)
        assertEquals("01,03", VentasTab.FACTURACION.sunatCodes)
    }

    // Solo el filtro de notas de venta se queda sin comprobantes electronicos; el resto puede
    // traerlos, y con ellos el estado de SUNAT.
    @Test
    fun `solo las notas de venta quedan fuera de lo electronico`() {
        assertFalse(VentasTab.NOTAS.incluyeElectronicos())
        assertTrue(VentasTab.TODAS.incluyeElectronicos())
        assertTrue(VentasTab.BOLETAS.incluyeElectronicos())
        assertTrue(VentasTab.FACTURAS.incluyeElectronicos())
        assertTrue(VentasTab.FACTURACION.incluyeElectronicos())
        assertTrue(VentasTab.CREDITOS.incluyeElectronicos())
    }
}
