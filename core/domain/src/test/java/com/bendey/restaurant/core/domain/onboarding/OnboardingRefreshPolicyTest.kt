package com.bendey.restaurant.core.domain.onboarding

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRefreshPolicyTest {

    @Test
    fun `sin carga previa se pide`() {
        assertTrue(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = null, nowMs = 1_000))
    }

    @Test
    fun `dentro de 60 s se usa la cache`() {
        assertFalse(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = 10_000, nowMs = 69_999))
    }

    @Test
    fun `a los 60 s o mas se vuelve a pedir`() {
        assertTrue(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = 10_000, nowMs = 70_000))
        assertTrue(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = 10_000, nowMs = 500_000))
    }

    @Test
    fun `forzar siempre pide`() {
        assertTrue(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = 10_000, nowMs = 10_001, force = true))
    }

    @Test
    fun `si el reloj retrocede se pide`() {
        assertTrue(OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs = 50_000, nowMs = 10_000))
    }
}

class OnboardingSupportTest {

    @Test
    fun `mensaje de borrar ejemplos`() {
        kotlin.test.assertEquals(
            "Se borraron 8 y se desactivaron 2 porque ya tienen ventas.",
            SampleDataDeleteResult(8, 2).userMessage(),
        )
        kotlin.test.assertEquals("Se borraron 10 productos de ejemplo.", SampleDataDeleteResult(10, 0).userMessage())
        kotlin.test.assertEquals("Se borró 1 producto de ejemplo.", SampleDataDeleteResult(1, 0).userMessage())
        kotlin.test.assertEquals("Se desactivaron 3 porque ya tienen ventas.", SampleDataDeleteResult(0, 3).userMessage())
        kotlin.test.assertEquals("No había ejemplos que borrar.", SampleDataDeleteResult(0, 0).userMessage())
    }

    @Test
    fun `la pista de pestana caduca y es de un solo uso`() {
        val req = ConfigTabHint.Request(ConfigTabHint.OPERACION, requestedAtMs = 10_000)
        kotlin.test.assertEquals("OPERACION", ConfigTabHint.freshTab(req, nowMs = 10_500))
        kotlin.test.assertEquals(null, ConfigTabHint.freshTab(req, nowMs = 10_000 + ConfigTabHint.TTL_MS + 1))
        kotlin.test.assertEquals(null, ConfigTabHint.freshTab(null, nowMs = 10_000))

        ConfigTabHint.request(ConfigTabHint.BRANCHES, nowMs = 1)
        kotlin.test.assertEquals("BRANCHES", ConfigTabHint.pending.value?.tab)
        ConfigTabHint.consume()
        kotlin.test.assertEquals(null, ConfigTabHint.pending.value)
    }
}
