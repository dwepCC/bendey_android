package com.bendey.restaurant.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PinCircleCountTest {
    @Test
    fun sinTeclearDibujaCuatro() = assertEquals(4, pinCircleCount(0))

    @Test
    fun hastaCuatroDigitosSigueEnCuatro() = assertEquals(4, pinCircleCount(4))

    @Test
    fun crecePorEncimaDeCuatro() {
        assertEquals(5, pinCircleCount(5))
        assertEquals(6, pinCircleCount(6))
    }

    @Test
    fun nuncaPasaDelMaximo() = assertEquals(6, pinCircleCount(9))
}
