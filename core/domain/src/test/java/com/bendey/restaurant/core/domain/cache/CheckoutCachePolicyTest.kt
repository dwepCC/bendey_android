package com.bendey.restaurant.core.domain.cache

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckoutCachePolicyTest {
    @Test fun primeraLecturaSoloFijaLaBase() = assertFalse(CheckoutCachePolicy.catalogVersionChanged(null, "a"))

    @Test fun mismaVersionNoInvalida() = assertFalse(CheckoutCachePolicy.catalogVersionChanged("a", "a"))

    @Test fun otraVersionInvalida() = assertTrue(CheckoutCachePolicy.catalogVersionChanged("a", "b"))

    @Test fun primerPlanoTrasPocoTiempoNoInvalida() =
        assertFalse(CheckoutCachePolicy.shouldInvalidateOnForeground(0, 9 * 60_000L))

    @Test fun primerPlanoTrasMuchoTiempoInvalida() =
        assertTrue(CheckoutCachePolicy.shouldInvalidateOnForeground(0, 10 * 60_000L))

    @Test fun sinSegundoPlanoPrevioNoInvalida() =
        assertFalse(CheckoutCachePolicy.shouldInvalidateOnForeground(null, 99_999_999L))
}
