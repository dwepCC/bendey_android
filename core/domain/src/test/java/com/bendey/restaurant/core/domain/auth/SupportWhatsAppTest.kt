package com.bendey.restaurant.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupportWhatsAppTest {
    @Test fun completaElPrefijoDePeruYCodificaElMensaje() {
        assertEquals("https://wa.me/51914561918", supportWhatsAppUrl("914 561 918"))
        assertEquals("https://wa.me/51914561918?text=Hola%20Ecuador", supportWhatsAppUrl("+51 914561918", "Hola Ecuador"))
    }

    @Test fun sinNumeroNoHayEnlace() {
        assertNull(supportWhatsAppUrl(""))
        assertNull(supportWhatsAppUrl(null))
        assertNull(supportWhatsAppUrl("123"))
    }
}
