package com.bendey.restaurant.core.domain.onboarding.wizard

import com.bendey.restaurant.core.domain.onboarding.OnboardingState
import com.bendey.restaurant.core.domain.onboarding.OnboardingStep
import com.bendey.restaurant.core.domain.onboarding.OnboardingTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WizardEligibilityTest {

    private fun step(key: String, done: Boolean) =
        OnboardingStep(key, OnboardingTier.REQUIRED, done, skipped = false, localOnly = false, count = null)

    private fun state(
        menuDone: Boolean = false,
        saleDone: Boolean = false,
        dismissed: Boolean = false,
        completedAt: String? = null,
        firstSaleSeen: Boolean = false,
        subtype: String = "",
        withMenuStep: Boolean = true,
    ) = OnboardingState(
        dismissed = dismissed,
        businessSubtype = subtype,
        completedAt = completedAt,
        firstSaleSeen = firstSaleSeen,
        steps = listOfNotNull(
            if (withMenuStep) step("menu", menuDone) else null,
            step("first_sale", saleDone),
        ),
    )

    private val open = WizardLocalState()

    @Test
    fun tenantNuevoVeElWizard() {
        assertTrue(shouldAutoShowWizard(state(), open))
    }

    @Test
    fun quienYaTieneCartaNoLoVe() {
        assertFalse(shouldAutoShowWizard(state(menuDone = true), open))
    }

    @Test
    fun quienYaTieneVentasNoLoVe() {
        assertFalse(shouldAutoShowWizard(state(saleDone = true), open))
        assertFalse(shouldAutoShowWizard(state(firstSaleSeen = true), open))
    }

    @Test
    fun checklistOcultoNoLoVe() {
        assertFalse(shouldAutoShowWizard(state(dismissed = true), open))
    }

    @Test
    fun cerradoEnEsteEquipoNoLoVe() {
        assertFalse(shouldAutoShowWizard(state(), WizardLocalState(closed = true)))
    }

    @Test
    fun completadoEnElServidorNoLoVe() {
        assertFalse(shouldAutoShowWizard(state(completedAt = "2026-10-04T10:00:00Z"), open))
    }

    @Test
    fun sinElPasoMenuDelServidorNoSeMuestra() {
        assertFalse(shouldAutoShowWizard(state(withMenuStep = false), open))
    }

    @Test
    fun progresoSeDerivaDeLosDatosDelServidor() {
        assertEquals(0, wizardProgressDone(state(), open))
        assertEquals(1, wizardProgressDone(state(subtype = "bar"), open))
        assertEquals(1, wizardProgressDone(state(), WizardLocalState(step = WizardStep.MENU)))
        assertEquals(2, wizardProgressDone(state(menuDone = true, subtype = "bar"), open))
        assertEquals(3, wizardProgressDone(state(menuDone = true, saleDone = true, subtype = "bar"), open))
        assertEquals(0, wizardProgressDone(null, open))
    }

    @Test
    fun sinEnMesasLaPrimeraVentaEsEnMostrador() {
        assertTrue(WizardLocalState().usesTables)
        assertFalse(WizardLocalState(serviceModes = setOf(ServiceMode.TAKEAWAY)).usesTables)
    }
}
