package com.bendey.restaurant.core.realtime

object PollingConfig {
    /** Intervalo de auto-refresh cocina / mesas (paridad web sin WebSocket). */
    const val COCINA_MESAS_MS = 15_000L
}
