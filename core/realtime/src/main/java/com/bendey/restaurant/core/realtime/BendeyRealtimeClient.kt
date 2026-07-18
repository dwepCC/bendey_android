package com.bendey.restaurant.core.realtime

import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.math.min
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

private const val PING_INTERVAL_MS = 30_000L
private const val MAX_RECONNECT_DELAY_MS = 30_000L
private const val CLIENT_VERSION = "1.0.0"

/**
 * Cliente WS Bendey Realtime v1 (REALTIME_PROTOCOL.md).
 * Puerto de `bendeyRealtime.ts` (front_tenant_restaurant_tauri) — misma secuencia
 * auth -> ping 30s -> reconexión exponencial `min(30_000, 1_000 * 2^attempt)`.
 */
@Singleton
class BendeyRealtimeClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionProvider: NetworkSessionProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _authOk = MutableStateFlow<AuthOk?>(null)
    val authOk: StateFlow<AuthOk?> = _authOk.asStateFlow()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var closedByUser = false
    private var reconnectAttempt = 0

    fun connect() {
        closedByUser = false
        openSocket()
    }

    fun disconnect() {
        closedByUser = true
        clearPing()
        val ws = webSocket
        webSocket = null
        if (ws != null) {
            try {
                ws.close(1000, "client_disconnect")
            } catch (_: Exception) {
                /* ignore */
            }
        }
        _connected.value = false
        _authOk.value = null
    }

    private fun openSocket() {
        val token = sessionProvider.token()?.trim().orEmpty()
        val base = sessionProvider.tenantApiBaseUrl()?.trim()?.trimEnd('/').orEmpty()
        val slug = sessionProvider.tenantSlug()?.trim().orEmpty()
        if (token.isBlank() || base.isBlank()) return

        val wsUrl = buildRealtimeWsUrl(base, slug)
        val request = Request.Builder()
            .url(wsUrl)
            .get()
            .apply {
                if (slug.isNotBlank()) header("X-Tenant-Slug", slug)
            }
            .build()

        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    ws.send(
                        RealtimeProtocol.authFrame(
                            token = token,
                            app = "restaurant-android",
                            version = CLIENT_VERSION,
                        ),
                    )
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    when (val msg = RealtimeProtocol.parseWireMessage(text)) {
                        is RealtimeWireMessage.Authenticated -> {
                            reconnectAttempt = 0
                            _authOk.value = msg.authOk
                            _connected.value = true
                            startPing()
                        }
                        is RealtimeWireMessage.Failed -> {
                            _connected.value = false
                            disconnect()
                        }
                        is RealtimeWireMessage.Event -> {
                            scope.launch { _events.emit(msg.envelope.event) }
                        }
                        RealtimeWireMessage.Pong -> Unit
                        null -> Unit
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    handleClose()
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    handleClose()
                }
            },
        )
    }

    private fun handleClose() {
        clearPing()
        _connected.value = false
        _authOk.value = null
        if (!closedByUser) {
            val delayMs = min(MAX_RECONNECT_DELAY_MS, (1000L * 2.0.pow(reconnectAttempt)).toLong())
            reconnectAttempt++
            scope.launch {
                delay(delayMs)
                if (!closedByUser) openSocket()
            }
        }
    }

    private fun startPing() {
        clearPing()
        pingJob = scope.launch {
            while (isActive) {
                delay(PING_INTERVAL_MS)
                webSocket?.send(RealtimeProtocol.pingFrame())
            }
        }
    }

    private fun clearPing() {
        pingJob?.cancel()
        pingJob = null
    }

    companion object {
        fun buildRealtimeWsUrl(apiBase: String, tenantSlug: String): String {
            val http = apiBase.trim().trimEnd('/')
            val ws = when {
                http.startsWith("https://") -> "wss://" + http.removePrefix("https://")
                http.startsWith("http://") -> "ws://" + http.removePrefix("http://")
                else -> "ws://$http"
            }
            val url = "$ws/api/realtime/ws"
            return if (tenantSlug.isNotBlank() && http.contains("127.0.0.1")) {
                "$url?tenant_slug=$tenantSlug"
            } else {
                url
            }
        }
    }
}
