package com.bendey.restaurant.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationRucLengthTest {

    @Test fun peruPide11DigitosYEcuador13() {
        assertEquals(11, registrationRucLength("PE"))
        assertEquals(13, registrationRucLength("EC"))
    }

    @Test fun elRegistroSinPaisSigueSiendoDePeru() {
        assertEquals("PE", RestaurantRegistrationInput(name = "", razonSocial = "", ruc = "", email = "", phone = "", password = "").country)
    }
}
