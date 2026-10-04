package com.bendey.restaurant.core.domain.onboarding

import com.bendey.restaurant.core.domain.model.AppResult

/**
 * Todo el acceso al estado de activación del restaurante. Un único punto: si el contrato del
 * backend cambia, solo se toca `OnboardingApi` y `OnboardingRepositoryImpl`.
 *
 * Los mensajes de [AppResult.Error] ya vienen en tuteo y son accionables.
 */
interface OnboardingRepository {
    suspend fun getState(): AppResult<OnboardingState>

    /** Aplica el cambio y devuelve el estado fresco (PATCH + GET: el backend puede responder 200 o 204). */
    suspend fun updatePreferences(update: OnboardingPreferencesUpdate): AppResult<OnboardingState>

    suspend fun loadSampleMenu(): AppResult<SampleMenuResult>

    suspend fun deleteSampleData(): AppResult<SampleDataDeleteResult>

    suspend fun requestSunatActivation(): AppResult<SunatRequestStatus>

    suspend fun getSunatStatus(): AppResult<SunatRequestStatus>
}
