package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.OnboardingPatchDto
import com.bendey.restaurant.core.network.dto.OnboardingStateDto
import com.bendey.restaurant.core.network.dto.SampleDataDeleteResponseDto
import com.bendey.restaurant.core.network.dto.SampleMenuResponseDto
import com.bendey.restaurant.core.network.dto.SunatRequestStatusDto
import com.bendey.restaurant.core.network.dto.TelemetryEventDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * Activación del restaurante (R3). Único punto de contacto con el backend de onboarding: si el
 * contrato final cambia en algún detalle, se ajusta aquí y en `OnboardingRepositoryImpl`.
 */
interface OnboardingApi {
    @GET("/api/restaurant/onboarding")
    suspend fun getState(): OnboardingStateDto

    /**
     * El backend responde 200 con el estado o 204 sin cuerpo; devolver Unit sirve a los dos y el
     * repositorio refresca con [getState].
     */
    @PATCH("/api/restaurant/onboarding")
    suspend fun patch(@Body body: OnboardingPatchDto)

    /** 409 si el catálogo no está vacío. */
    @POST("/api/restaurant/onboarding/sample-menu")
    suspend fun loadSampleMenu(): SampleMenuResponseDto

    @DELETE("/api/restaurant/onboarding/sample-data")
    suspend fun deleteSampleData(): SampleDataDeleteResponseDto

    /** Idempotente. */
    @POST("/api/company/sunat/request")
    suspend fun requestSunat(): SunatRequestStatusDto

    @GET("/api/company/sunat/status")
    suspend fun getSunatStatus(): SunatRequestStatusDto

    /** Fire-and-forget: 204 sin cuerpo. */
    @POST("/api/telemetry/events")
    suspend fun postTelemetryEvent(@Body body: TelemetryEventDto)
}
