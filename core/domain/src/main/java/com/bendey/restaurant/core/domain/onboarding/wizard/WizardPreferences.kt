package com.bendey.restaurant.core.domain.onboarding.wizard

import kotlinx.coroutines.flow.Flow

/**
 * Estado LOCAL del wizard (por equipo y por restaurante): "wizard cerrado", modos de atención y
 * paso actual. NO usa `dismissed` del servidor (eso oculta el checklist).
 */
interface WizardPreferences {
    val state: Flow<WizardLocalState>

    suspend fun setClosed(closed: Boolean)
    suspend fun setServiceModes(modes: Set<ServiceMode>)
    suspend fun setStep(step: WizardStep)
}
