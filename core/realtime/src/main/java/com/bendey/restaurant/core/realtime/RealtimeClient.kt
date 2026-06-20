package com.bendey.restaurant.core.realtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class RealtimeTransport {
    SSE,
    WEB_SOCKET,
    SIGNALR,
}

sealed class RealtimeEndpoint {
    abstract val transport: RealtimeTransport

    data class Sse(
        val url: String,
        val accessToken: String,
    ) : RealtimeEndpoint() {
        override val transport = RealtimeTransport.SSE
    }

    data class WebSocket(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
    ) : RealtimeEndpoint() {
        override val transport = RealtimeTransport.WEB_SOCKET
    }

    data class SignalR(
        val hubUrl: String,
        val accessToken: String? = null,
    ) : RealtimeEndpoint() {
        override val transport = RealtimeTransport.SIGNALR
    }
}

data class RealtimeEvent(
    val name: String,
    val payload: String,
    val receivedAtEpochMs: Long = System.currentTimeMillis(),
)

interface RealtimeClient {
    val events: Flow<RealtimeEvent>
    val isConnected: Flow<Boolean>
    suspend fun connect(endpoint: RealtimeEndpoint)
    suspend fun disconnect()
}

/** Stub hasta integración WebSocket/SSE en fases posteriores. */
class StubRealtimeClient : RealtimeClient {
    override val events: Flow<RealtimeEvent> = emptyFlow()
    override val isConnected: Flow<Boolean> = emptyFlow()

    override suspend fun connect(endpoint: RealtimeEndpoint) = Unit

    override suspend fun disconnect() = Unit
}
