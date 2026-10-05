package com.bendey.restaurant.core.domain.connectivity

/** Que sabemos del servidor: nunca se afirma "alcanzable" sin haberlo comprobado. */
enum class ReachabilityLevel { UNKNOWN, REACHABLE, DEGRADED, UNREACHABLE }

data class ReachabilityState(
    val consecutiveFailures: Int = 0,
    val probedOnce: Boolean = false,
)

/**
 * Decisor PURO del alcance real del backend (R10, indicador de conexion). Mismos numeros que el
 * `connectivityManager` de Tauri: sonda a `/api/health/live` cada 30 s, cada 10 s tras el primer
 * fallo, "Sin conexion" a los 3 fallos seguidos, y una respuesta de la API reciente cuenta como
 * prueba de vida aunque la sonda falle.
 */
object BackendReachability {
    const val PROBE_INTERVAL_MS = 30_000L
    const val RECOVERY_INTERVAL_MS = 10_000L
    const val PROBE_TIMEOUT_MS = 5_000L
    const val FAILURES_BEFORE_OFFLINE = 3
    const val API_SUCCESS_GRACE_MS = 45_000L

    fun onProbe(
        state: ReachabilityState,
        ok: Boolean,
        nowMs: Long,
        lastApiSuccessMs: Long,
    ): ReachabilityState {
        val recentApi = lastApiSuccessMs > 0 && nowMs - lastApiSuccessMs < API_SUCCESS_GRACE_MS
        return if (ok || recentApi) {
            ReachabilityState(consecutiveFailures = 0, probedOnce = true)
        } else {
            ReachabilityState(consecutiveFailures = state.consecutiveFailures + 1, probedOnce = true)
        }
    }

    fun level(state: ReachabilityState): ReachabilityLevel = when {
        !state.probedOnce -> ReachabilityLevel.UNKNOWN
        state.consecutiveFailures >= FAILURES_BEFORE_OFFLINE -> ReachabilityLevel.UNREACHABLE
        state.consecutiveFailures > 0 -> ReachabilityLevel.DEGRADED
        else -> ReachabilityLevel.REACHABLE
    }

    fun nextDelayMs(state: ReachabilityState): Long =
        if (state.consecutiveFailures > 0) RECOVERY_INTERVAL_MS else PROBE_INTERVAL_MS
}
