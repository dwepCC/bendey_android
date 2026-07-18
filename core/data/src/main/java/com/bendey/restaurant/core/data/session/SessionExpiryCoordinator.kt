package com.bendey.restaurant.core.data.session

import com.bendey.restaurant.core.data.cache.OperationalDataCache
import com.bendey.restaurant.core.network.session.SessionExpiryReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manejo global de expiración de sesión. Cuando la capa de red detecta un 401 en un request
 * autenticado, cierra la sesión (limpia token + datos) y emite un evento para que la UI muestre el
 * aviso. Al limpiar la sesión, [SessionManager.isAuthenticatedFlow] pasa a false y el NavHost
 * redirige automáticamente al login.
 */
@Singleton
class SessionExpiryCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val operationalDataCache: OperationalDataCache,
) : SessionExpiryReporter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emite el mensaje a mostrar cuando la sesión expira. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Evita ejecutar el cierre varias veces ante una ráfaga de 401 concurrentes. */
    private val handling = AtomicBoolean(false)

    override fun reportSessionExpired() {
        // Sin token = ya no hay sesión (o cierre en curso): nada que hacer.
        if (sessionManager.token().isNullOrBlank()) return
        if (!handling.compareAndSet(false, true)) return
        scope.launch {
            runCatching {
                operationalDataCache.clearAll()
                sessionManager.clearUserSession()
            }
            _events.emit(SESSION_EXPIRED_MESSAGE)
            handling.set(false)
        }
    }

    companion object {
        const val SESSION_EXPIRED_MESSAGE = "Tu sesión ha expirado. Inicia sesión nuevamente."
    }
}
