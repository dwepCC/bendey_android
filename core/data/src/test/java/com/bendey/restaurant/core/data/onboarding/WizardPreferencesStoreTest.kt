package com.bendey.restaurant.core.data.onboarding

import com.bendey.restaurant.core.domain.onboarding.wizard.ServiceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardPreferencesStoreTest {
    @Test
    fun modosDeAtencionSeGuardanYSeLeenIgual() {
        val modes = setOf(ServiceMode.TAKEAWAY, ServiceMode.DELIVERY)
        assertEquals(modes, WizardPreferencesStore.decodeModes(WizardPreferencesStore.encodeModes(modes)))
    }

    @Test
    fun sinValorGuardadoOIlegibleSeAsumeEnMesas() {
        assertEquals(setOf(ServiceMode.DINE_IN), WizardPreferencesStore.decodeModes(null))
        assertEquals(setOf(ServiceMode.DINE_IN), WizardPreferencesStore.decodeModes(""))
        assertEquals(setOf(ServiceMode.DINE_IN), WizardPreferencesStore.decodeModes("xx,yy"))
    }
}
