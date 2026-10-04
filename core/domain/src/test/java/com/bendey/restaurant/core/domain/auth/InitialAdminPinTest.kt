package com.bendey.restaurant.core.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InitialAdminPinTest {

    @Test
    fun aceptaSoloPinNumericoDe4a6Digitos() {
        assertEquals("4093", normalizeInitialPin("4093"))
        assertEquals("0481", normalizeInitialPin(" 0481 "))
        assertNull(normalizeInitialPin("123"))
        assertNull(normalizeInitialPin("1234567"))
        assertNull(normalizeInitialPin("12a4"))
        assertNull(normalizeInitialPin(null))
    }

    @Test
    fun elHolderViveEnMemoriaYSeLimpia() {
        InitialAdminPinHolder.clear()
        assertNull(InitialAdminPinHolder.peek())
        InitialAdminPinHolder.stash("4093")
        assertEquals("4093", InitialAdminPinHolder.peek())
        InitialAdminPinHolder.clear()
        assertNull(InitialAdminPinHolder.peek())
    }

    @Test
    fun unValorInvalidoNoSeGuarda() {
        InitialAdminPinHolder.stash("abc")
        assertNull(InitialAdminPinHolder.peek())
    }

    @Test
    fun sinPinNoHayAvisoYConPinNoContieneElPinHistorico() {
        assertNull(initialPinNotice(null))
        val text = initialPinNotice("4093")
        assertTrue(text!!.contains("Cocina y Delivery"))
        assertTrue(text.contains("no se vuelve a mostrar"))
        assertFalse(text.contains("7410"))
    }
}
