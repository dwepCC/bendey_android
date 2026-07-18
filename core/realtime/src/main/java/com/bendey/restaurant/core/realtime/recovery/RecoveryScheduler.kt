package com.bendey.restaurant.core.realtime.recovery

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val RECOVERY_DEBOUNCE_MS = 120L

private fun jobKey(req: RecoveryRequest): String {
    val s = req.scope
    return "${req.policy}:${s?.domain.orEmpty()}:${s?.slice.orEmpty()}:${s?.entity.orEmpty()}:${s?.entityId ?: ""}"
}

/**
 * Agrupa recovery HTTP en ráfagas cortas — evita N GET por ráfaga de eventos WS.
 * Puerto de `recovery/scheduler.ts` (Tauri) — debounce 120ms idéntico.
 */
@Singleton
class RecoveryScheduler @Inject constructor(
    private val realtimeRecovery: RealtimeRecovery,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val pending = LinkedHashMap<String, RecoveryRequest>()
    private var debounceJob: Job? = null

    fun schedule(req: RecoveryRequest) {
        scope.launch {
            mutex.withLock {
                pending[jobKey(req)] = req
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(RECOVERY_DEBOUNCE_MS)
                    val jobs = mutex.withLock {
                        val list = pending.values.toList()
                        pending.clear()
                        list
                    }
                    for (job in jobs) realtimeRecovery.run(job)
                }
            }
        }
    }

    fun resetForTests() {
        debounceJob?.cancel()
        debounceJob = null
        pending.clear()
    }
}
