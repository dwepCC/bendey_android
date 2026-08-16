package com.bendey.restaurant.platform.printing.escpos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NADA SALE DE ACÁ QUE LA TICKETERA NO SEPA IMPRIMIR.
 *
 * El texto viaja en UTF-8 y la impresora lo lee en su página de códigos, así que un carácter que se
 * escape no sale «raro»: sale como dos o tres símbolos de basura. Un tenant real tenía «ALITAS JACK
 * DANIEL´S» y en la comanda salía «DANIEL┤S».
 *
 * Estos casos son los mismos que normalizeTextForTicketPrint.test.ts: las dos implementaciones
 * imprimen los mismos tickets y ya divergieron una vez con el «•».
 */
class EscPosTextUtilsTest {

    @Test
    fun `quita las tildes y deja el nombre legible`() {
        assertEquals("MENU", EscPosTextUtils.normalizeForPrint("MENÚ"))
        assertEquals("JAMON SERRANO", EscPosTextUtils.normalizeForPrint("JAMÓN SERRANO"))
    }

    @Test
    fun `escribe la enie como la lee un cocinero`() {
        assertEquals("TEQUENiOS", EscPosTextUtils.normalizeForPrint("TEQUEÑOS"))
        assertEquals("PINA", EscPosTextUtils.normalizeForPrint("PIÑA"))
    }

    @Test
    fun `convierte el acento suelto en apostrofo`() {
        assertEquals("ALITAS JACK DANIEL'S", EscPosTextUtils.normalizeForPrint("ALITAS JACK DANIEL´S"))
    }

    @Test
    fun `no deja pasar ningun caracter fuera del ASCII imprimible`() {
        val salida = EscPosTextUtils.normalizeForPrint("CAFÉ ☕ 100% ARÁBICA ® — «premium» 中文")
        assertTrue("quedo basura: $salida", salida.all { it == '\n' || it == '\r' || it == '\t' || it.code in 0x20..0x7e })
        assertTrue(salida.contains("CAFE"))
        assertTrue(salida.contains("ARABICA"))
    }

    @Test
    fun `conserva los saltos de linea que son el formato del ticket`() {
        assertEquals("UNO\nDOS\tTRES", EscPosTextUtils.normalizeForPrint("UNO\nDOS\tTRES"))
    }

    @Test
    fun `deja intacto lo que ya era ASCII`() {
        assertEquals("CALDO DE POLLO", EscPosTextUtils.normalizeForPrint("CALDO DE POLLO"))
        assertEquals("", EscPosTextUtils.normalizeForPrint(""))
    }
}
