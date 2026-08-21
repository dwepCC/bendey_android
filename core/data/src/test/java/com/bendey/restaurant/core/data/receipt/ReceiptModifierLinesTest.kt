package com.bendey.restaurant.core.data.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EL COMPROBANTE TIENE QUE DECIR QUÉ SE VENDIÓ.
 *
 * Datos reales de la boleta B001-00000474: dos líneas distintas impresas las dos como «6 Alitas».
 * Mismos casos que productModifiers.receipt.test.ts — los dos lados imprimen los mismos tickets.
 */
class ReceiptModifierLinesTest {

    private val money: (Double) -> String = { "S/ %.2f".format(it) }

    private val dueto =
        """[{"group_id":0,"group_name":"Presentacion","type":"variant","group_type":"variant","option_id":9,"option_name":"Duo 6 Nuggets + 6 Alitas + Papa","extra_price":26,"snapshot":true}]"""
    private val acevichadas =
        """[{"group_id":0,"group_name":"Presentacion","type":"variant","group_type":"variant","option_id":6,"option_name":"6 Alitas Acevichadas con Papas","extra_price":16,"snapshot":true}]"""

    @Test
    fun `distingue las dos lineas que la boleta mostraba iguales`() {
        assertEquals(listOf("Duo 6 Nuggets + 6 Alitas + Papa"), ReceiptModifierLines.of(dueto, money))
        assertEquals(listOf("6 Alitas Acevichadas con Papas"), ReceiptModifierLines.of(acevichadas, money))
    }

    @Test
    fun `no repite el precio de la presentacion`() {
        assertFalse(ReceiptModifierLines.of(dueto, money).first().contains("26"))
    }

    @Test
    fun `conserva el precio de los extras`() {
        val extras = """[{"group_name":"Salsas","type":"modifier","option_name":"Salsa BBQ","extra_price":2}]"""
        assertTrue(ReceiptModifierLines.of(extras, money).first().contains("2.00"))
    }

    @Test
    fun `acepta el alias legacy de retail`() {
        assertEquals(listOf("+ Talla M"), ReceiptModifierLines.of("""[{"name":"Talla M","extra_price":0}]""", money))
    }

    @Test
    fun `no inventa lineas cuando no hay nada elegido`() {
        assertEquals(emptyList<String>(), ReceiptModifierLines.of(null, money))
        assertEquals(emptyList<String>(), ReceiptModifierLines.of("", money))
        assertEquals(emptyList<String>(), ReceiptModifierLines.of("{no es json", money))
    }
}
