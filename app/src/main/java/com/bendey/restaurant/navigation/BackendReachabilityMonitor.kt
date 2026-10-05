package com.bendey.restaurant.navigation

import com.bendey.restaurant.core.domain.connectivity.BackendReachability
import com.bendey.restaurant.core.domain.connectivity.ReachabilityLevel
import com.bendey.restaurant.core.domain.connectivity.ReachabilityState
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.client.BackendHealthProbe
import com.bendey.restaurant.core.network.interceptor.ApiActivitySignal
import com.bendey.restaurant.core.realtime.connection.AppForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alcance REAL del backend para el indicador de conexion (R10). Sonda `GET /api/health/live` con la app
 * en pantalla y sesion iniciada: cada 30 s, cada 10 s tras un fallo, "sin conexion" a los 3 fallos
 * seguidos (decision en `BackendReachability`, igual que Tauri). Una respuesta reciente de la API cuenta
 * como prueba de vida. Al recuperar la red del dispositivo o volver a primer plano sonda de inmediato.
 */
@Singleton
class BackendReachabilityMonitor @Inject constructor(
    private val healthProbe: BackendHealthProbe,
    private val apiActivity: ApiActivitySignal,
    private val appForeground: AppForeground,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val sessionStore: UserSessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private var state = ReachabilityState()
    private val _level = MutableStateFlow(ReachabilityLevel.UNKNOWN)
    val level: StateFlow<ReachabilityLevel> = _level.asStateFlow()

    /** Idempotente; se llama al crear el header. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                appForeground.enPantalla,
                networkStatusMonitor.hasNetwork,
                sessionStore.isAuthenticatedFlow.map { it },
            ) { fg, net, auth -> Triple(fg, net, auth) }
                .distinctUntilChanged()
                .collectLatest { (fg, net, auth) ->
                    if (!auth) {
                        state = ReachabilityState()
                        _level.value = ReachabilityLevel.UNKNOWN
                        return@collectLatest
                    }
                    if (!fg || !net) return@collectLatest
                    while (true) {
                        val ok = probe()
                        state = BackendReachability.onProbe(state, ok, System.currentTimeMillis(), apiActivity.lastSuccessMs)
                        _level.value = BackendReachability.level(state)
                        delay(BackendReachability.nextDelayMs(state))
                    }
                }
        }
    }

    private suspend fun probe(): Boolean =
        withTimeoutOrNull(BackendReachability.PROBE_TIMEOUT_MS) { healthProbe.isReachable() } ?: false
}
