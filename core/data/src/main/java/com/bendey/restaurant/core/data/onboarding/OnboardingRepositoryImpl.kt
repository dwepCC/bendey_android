package com.bendey.restaurant.core.data.onboarding

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingPreferencesUpdate
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.OnboardingServerProgress
import com.bendey.restaurant.core.domain.onboarding.OnboardingState
import com.bendey.restaurant.core.domain.onboarding.OnboardingStep
import com.bendey.restaurant.core.domain.onboarding.OnboardingTier
import com.bendey.restaurant.core.domain.onboarding.OnboardingUnavailableException
import com.bendey.restaurant.core.domain.onboarding.SampleDataDeleteResult
import com.bendey.restaurant.core.domain.onboarding.SampleMenuResult
import com.bendey.restaurant.core.domain.onboarding.SunatRequestState
import com.bendey.restaurant.core.domain.onboarding.SunatRequestStatus
import com.bendey.restaurant.core.network.api.OnboardingApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.OnboardingPatchDto
import com.bendey.restaurant.core.network.dto.OnboardingStateDto
import com.bendey.restaurant.core.network.dto.SunatRequestStatusDto
import com.bendey.restaurant.core.network.error.ModuleLockedException
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import com.bendey.restaurant.core.network.error.SubscriptionBlockedException
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Único consumidor de [OnboardingApi]. Convierte DTOs a dominio y errores a mensajes accionables
 * en tuteo (el mapeo global de errores llega en R5; estos ya son claros).
 */
@Singleton
class OnboardingRepositoryImpl internal constructor(
    private val apiSource: () -> OnboardingApi,
) : OnboardingRepository {

    @Inject
    constructor(tenantRetrofitProvider: TenantRetrofitProvider) : this({ tenantRetrofitProvider.create() })

    private val api: OnboardingApi get() = apiSource()

    override suspend fun getState(): AppResult<OnboardingState> {
        val result = call(MSG_LOAD) { api.getState().toDomain() }
        // 403 (no eres administrador) y 404 (backend sin la ruta): no es algo que reintentar.
        val cause = (result as? AppResult.Error)?.cause
        val status = cause?.let(NetworkErrorMapper::httpStatus)
        return if (status == 403 || status == 404) {
            AppResult.Error(MSG_UNAVAILABLE, OnboardingUnavailableException(MSG_UNAVAILABLE, cause))
        } else {
            result
        }
    }

    override suspend fun updatePreferences(update: OnboardingPreferencesUpdate): AppResult<OnboardingState> {
        val patched = call(MSG_SAVE) {
            api.patch(
                OnboardingPatchDto(
                    dismissed = update.dismissed,
                    skip = update.skip,
                    unskip = update.unskip,
                    businessSubtype = update.businessSubtype,
                ),
            )
        }
        if (patched is AppResult.Error) return patched
        // 200 con estado o 204 sin cuerpo: en ambos casos se relee, así hay una sola fuente de verdad.
        return call(MSG_LOAD) { api.getState().toDomain() }
    }

    override suspend fun loadSampleMenu(): AppResult<SampleMenuResult> {
        return try {
            AppResult.Success(SampleMenuResult(created = api.loadSampleMenu().created))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (NetworkErrorMapper.esConflicto(e)) {
                AppResult.Error(MSG_SAMPLE_NOT_EMPTY, e)
            } else {
                failure(e, MSG_SAMPLE_LOAD)
            }
        }
    }

    override suspend fun deleteSampleData(): AppResult<SampleDataDeleteResult> = call(MSG_SAMPLE_DELETE) {
        val r = api.deleteSampleData()
        SampleDataDeleteResult(deleted = r.deleted, deactivated = r.deactivated)
    }

    override suspend fun requestSunatActivation(): AppResult<SunatRequestStatus> = call(MSG_SUNAT_REQUEST) {
        api.requestSunat().toDomain()
    }

    override suspend fun getSunatStatus(): AppResult<SunatRequestStatus> = call(MSG_SUNAT_STATUS) {
        api.getSunatStatus().toDomain()
    }

    private inline fun <T> call(fallback: String, block: () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failure(e, fallback)
    }

    private fun failure(e: Exception, fallback: String): AppResult.Error {
        val mapped = NetworkErrorMapper.map(e)
        val message = when {
            e is IOException -> MSG_CONNECTION
            e is SerializationException -> MSG_UNEXPECTED
            mapped is SubscriptionBlockedException || mapped is ModuleLockedException -> mapped.message ?: fallback
            NetworkErrorMapper.httpStatus(e) == 403 -> MSG_ADMIN_ONLY
            else -> fallback
        }
        return AppResult.Error(message, e)
    }

    companion object {
        const val MSG_CONNECTION = "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."
        const val MSG_UNEXPECTED = "Recibimos una respuesta inesperada del servidor. Inténtalo de nuevo."
        const val MSG_ADMIN_ONLY = "Esta opción es solo para el administrador del restaurante."
        const val MSG_UNAVAILABLE = "Los primeros pasos no están disponibles para esta sesión."
        const val MSG_LOAD = "No pudimos cargar tus primeros pasos. Inténtalo de nuevo."
        const val MSG_SAVE = "No pudimos guardar el cambio. Inténtalo de nuevo."
        const val MSG_SAMPLE_LOAD = "No pudimos cargar la carta de ejemplo. Inténtalo de nuevo."
        const val MSG_SAMPLE_NOT_EMPTY = "Ya tienes productos; la carta de ejemplo solo se carga en una carta vacía."
        const val MSG_SAMPLE_DELETE = "No pudimos borrar los ejemplos. Inténtalo de nuevo."
        const val MSG_SUNAT_REQUEST = "No pudimos enviar tu solicitud. Inténtalo de nuevo en unos minutos."
        const val MSG_SUNAT_STATUS = "No pudimos consultar el estado de tu solicitud de SUNAT."
    }
}

internal fun OnboardingStateDto.toDomain() = OnboardingState(
    dismissed = dismissed,
    businessSubtype = businessSubtype.orEmpty(),
    sampleDataLoaded = sampleDataLoaded,
    startedAt = startedAt,
    completedAt = completedAt,
    firstSaleSeen = firstSaleSeen,
    progress = OnboardingServerProgress(
        sellReady = progress.sellReady,
        operateDone = progress.operateDone,
        operateTotal = progress.operateTotal,
        percent = progress.percent,
    ),
    steps = steps.map {
        OnboardingStep(
            key = it.key,
            tier = OnboardingTier.fromApi(it.tier),
            done = it.done,
            skipped = it.skipped,
            localOnly = it.localOnly,
            count = it.count,
        )
    },
)

internal fun SunatRequestStatusDto.toDomain() = SunatRequestStatus(
    state = SunatRequestState.fromApi(status),
    requestedAt = requestedAt,
)
