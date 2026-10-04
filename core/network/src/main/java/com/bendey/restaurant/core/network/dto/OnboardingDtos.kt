package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /api/restaurant/onboarding`. Todo con default: el contrato aún lo cierra el backend. */
@Serializable
data class OnboardingStateDto(
    val dismissed: Boolean = false,
    @SerialName("business_subtype") val businessSubtype: String? = null,
    @SerialName("sample_data_loaded") val sampleDataLoaded: Boolean = false,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("first_sale_seen") val firstSaleSeen: Boolean = false,
    val progress: OnboardingProgressDto = OnboardingProgressDto(),
    val steps: List<OnboardingStepDto> = emptyList(),
)

@Serializable
data class OnboardingProgressDto(
    @SerialName("sell_ready") val sellReady: Boolean = false,
    @SerialName("operate_done") val operateDone: Int = 0,
    @SerialName("operate_total") val operateTotal: Int = 0,
    val percent: Int = 0,
)

@Serializable
data class OnboardingStepDto(
    val key: String = "",
    val tier: String? = null,
    val done: Boolean = false,
    val skipped: Boolean = false,
    @SerialName("local_only") val localOnly: Boolean = false,
    val count: Int? = null,
)

/** `PATCH /api/restaurant/onboarding`: solo viaja lo que no es null (`explicitNulls = false`). */
@Serializable
data class OnboardingPatchDto(
    val dismissed: Boolean? = null,
    val skip: List<String>? = null,
    val unskip: List<String>? = null,
    @SerialName("business_subtype") val businessSubtype: String? = null,
)

@Serializable
data class SampleMenuResponseDto(val created: Int = 0)

@Serializable
data class SampleDataDeleteResponseDto(
    val deleted: Int = 0,
    val deactivated: Int = 0,
)

/** `POST /api/company/sunat/request` y `GET /api/company/sunat/status`. */
@Serializable
data class SunatRequestStatusDto(
    val status: String = "none",
    @SerialName("requested_at") val requestedAt: String? = null,
)

/** `POST /api/telemetry/events`. */
@Serializable
data class TelemetryEventDto(
    val key: String,
    @SerialName("device_kind") val deviceKind: String,
)
