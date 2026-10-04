package com.bendey.restaurant.core.data.onboarding

import android.util.Log
import com.bendey.restaurant.core.network.api.OnboardingApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.TelemetryEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "OnboardingTelemetry"

/** `android.util.Log` no existe en los tests JVM: si falla el log, no pasa nada. */
private fun logToAndroid(message: String, error: Throwable) {
    try {
        Log.w(TAG, message, error)
    } catch (_: Throwable) {
    }
}

/**
 * Eventos de activación hacia `POST /api/telemetry/events`. Fire-and-forget: corre en su propio
 * scope, NUNCA bloquea a quien lo llama y un fallo solo va al log (jamás se muestra al usuario).
 */
@Singleton
class OnboardingTelemetry internal constructor(
    private val apiSource: () -> OnboardingApi,
    private val scope: CoroutineScope,
    private val log: (String, Throwable) -> Unit,
) {
    @Inject
    constructor(tenantRetrofitProvider: TenantRetrofitProvider) : this(
        apiSource = { tenantRetrofitProvider.create() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        log = ::logToAndroid,
    )

    /** La primera vez que este equipo queda con una impresora lista. */
    fun reportPrinterConfigured() = report(EVENT_PRINTER_CONFIGURED)

    private fun report(key: String) {
        scope.launch {
            try {
                apiSource().postTelemetryEvent(TelemetryEventDto(key = key, deviceKind = DEVICE_KIND_ANDROID))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("No se pudo reportar el evento $key", e)
            }
        }
    }

    companion object {
        const val EVENT_PRINTER_CONFIGURED = "printer_configured"
        const val DEVICE_KIND_ANDROID = "android"
    }
}
