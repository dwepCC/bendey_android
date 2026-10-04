package com.bendey.restaurant.core.domain.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TABLA DE CASOS COMPARTIDA con Tauri (`firstSaleNext.test.ts`): mismos ids (C01…C18), mismas
 * entradas, mismas salidas. C02 (steps no es lista) no existe en Kotlin: el tipo lo impide.
 */
class FirstSaleNextTest {
    private val all = listOf("qr_menu", "sunat", "team", "auth_pin")

    private fun mk(
        pending: List<String>,
        skipped: List<String> = emptyList(),
        absent: List<String> = emptyList(),
    ) = OnboardingState(
        firstSaleSeen = true,
        steps = all.filter { it !in absent }.map {
            OnboardingStep(it, OnboardingTier.RECOMMENDED, done = it !in pending, skipped = it in skipped, localOnly = false, count = null)
        },
    )

    private fun rec(p: NextActionId, vararg s: NextActionId) = NextRecommendation(p, s.toList())

    private class Case(
        val id: String,
        val state: OnboardingState?,
        val printer: Boolean,
        val counter: Boolean,
        val expected: NextRecommendation?,
    )

    private val P = NextActionId.PRINTER
    private val T = NextActionId.TABLE_SALE
    private val Q = NextActionId.QR_MENU
    private val S = NextActionId.SUNAT
    private val E = NextActionId.TEAM
    private val A = NextActionId.AUTH_PIN

    private val cases = listOf(
        Case("C01 API caida (state null)", null, false, false, null),
        Case("C03 todo hecho", mk(emptyList()), true, false, null),
        Case("C04 sin impresora: impresora primero y el resto detras", mk(all), false, false, rec(P, Q, A)),
        Case("C05 con impresora: QR primero", mk(all), true, false, rec(Q, S, A)),
        Case("C06 con impresora, QR hecho: SUNAT primero", mk(listOf("sunat", "team", "auth_pin")), true, false, rec(S, E, A)),
        Case("C07 solo equipo", mk(listOf("team")), true, false, rec(E)),
        Case("C08 solo PIN: principal si nada mas aplica", mk(listOf("auth_pin")), true, false, rec(A)),
        Case("C09 sin impresora y solo PIN: PIN como secundaria", mk(listOf("auth_pin")), false, false, rec(P, A)),
        Case("C10 sin impresora, nada pendiente en servidor", mk(emptyList()), false, false, rec(P)),
        Case("C11 sin impresora, QR SUNAT y equipo (sin PIN): dos secundarias", mk(listOf("qr_menu", "sunat", "team")), false, false, rec(P, Q, S)),
        Case("C12 sin impresora, 4 candidatas: el PIN desplaza a la ultima secundaria", mk(listOf("sunat", "team", "auth_pin")), false, false, rec(P, S, A)),
        Case("C13 paso omitido no cuenta", mk(listOf("qr_menu", "sunat"), skipped = listOf("qr_menu")), true, false, rec(S)),
        Case("C14 paso ausente del servidor no cuenta", mk(listOf("qr_menu", "sunat"), absent = listOf("qr_menu")), true, false, rec(S)),
        Case("C15 solo mostrador con impresora: venta con mesa en lugar de QR", mk(all), true, true, rec(T, S, A)),
        Case("C16 solo mostrador sin impresora", mk(all), false, true, rec(P, T, A)),
        Case("C17 solo mostrador, todo lo demas hecho: igual propone mesa", mk(emptyList()), true, true, rec(T)),
        Case("C18 solo mostrador: QR pendiente nunca aparece", mk(listOf("qr_menu")), true, true, rec(T)),
    )

    @Test
    fun tablaCompartida() {
        for (c in cases) {
            assertEquals(c.expected, recommendNext(c.state, c.printer, c.counter), c.id)
        }
    }

    @Test
    fun bandaSoloConFlagExplicitoYReciboAbierto() {
        assertTrue(shouldShowFirstSaleBand(true, true))
        assertFalse(shouldShowFirstSaleBand(true, false))
        assertFalse(shouldShowFirstSaleBand(false, true))
        assertFalse(shouldShowFirstSaleBand(null, true)) // reimpresion desde Ventas
    }

    @Test
    fun momentoEsUnicoPorCobroYSeCierra() {
        FirstSaleMoment.finish()
        FirstSaleMoment.onReceiptShown("B001-1")
        assertEquals("B001-1", FirstSaleMoment.state.value.token)
        assertFalse(FirstSaleMoment.state.value.receiptClosed)
        FirstSaleMoment.onReceiptClosed()
        assertTrue(FirstSaleMoment.state.value.receiptClosed)
        FirstSaleMoment.finish()
        assertNull(FirstSaleMoment.state.value.token)
        // Recomponer el mismo recibo no reabre el momento.
        FirstSaleMoment.onReceiptShown("B001-1")
        assertNull(FirstSaleMoment.state.value.token)
        // Cerrar sin momento en curso no hace nada.
        FirstSaleMoment.onReceiptClosed()
        assertFalse(FirstSaleMoment.state.value.receiptClosed)
    }
}
