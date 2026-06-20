package com.bendey.restaurant.core.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Polling silencioso mientras la pantalla está visible. */
class AutoRefreshHandle {
    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        intervalMs: Long = PollingConfig.COCINA_MESAS_MS,
        onTick: suspend () -> Unit,
    ) {
        stop()
        job = scope.launch {
            delay(intervalMs)
            while (isActive) {
                onTick()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
