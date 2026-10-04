package com.bendey.restaurant.core.domain.onboarding.wizard

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WizardCoachTest {

    @AfterTest
    fun reset() = WizardCoach.close()

    @Test
    fun conMesasLosCincoPasosVanDeCajaAMesaACocinaYCobro() {
        val ids = coachSteps(usesTables = true).map { it.id }
        assertEquals(
            listOf(CoachStepId.CASH, CoachStepId.TABLE, CoachStepId.ADD_DISH, CoachStepId.KITCHEN, CoachStepId.CHARGE),
            ids,
        )
        assertEquals(CoachTarget.MESAS, coachSteps(true)[1].target)
    }

    @Test
    fun sinMesasLaVentaEsEnMostrador() {
        val steps = coachSteps(usesTables = false)
        assertEquals(CoachStepId.COUNTER, steps[1].id)
        assertTrue(steps.none { it.target == CoachTarget.MESAS })
        assertEquals(CoachTarget.POS, steps[2].target)
    }

    @Test
    fun laGuiaAvanzaYSeCierraAlTerminar() {
        WizardCoach.start(usesTables = true)
        assertTrue(WizardCoach.state.value.active)
        repeat(4) { WizardCoach.next() }
        assertTrue(WizardCoach.state.value.isLast)
        assertEquals(CoachStepId.CHARGE, WizardCoach.state.value.current?.id)
        WizardCoach.next()
        assertFalse(WizardCoach.state.value.active)
    }

    @Test
    fun sePuedeDescartarEnCualquierMomento() {
        WizardCoach.start(usesTables = true)
        WizardCoach.next()
        WizardCoach.close()
        assertFalse(WizardCoach.state.value.active)
        assertEquals(null, WizardCoach.state.value.current)
    }
}
