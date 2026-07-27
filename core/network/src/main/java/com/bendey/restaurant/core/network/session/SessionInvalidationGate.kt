package com.bendey.restaurant.core.network.session

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compuerta en memoria (sincrónica) para cortar en seco las peticiones autenticadas cuando el token
 * ya expiró/inválido.
 *
 * Problema que resuelve: al abrir la app con el token caducado, varias fuentes disparan peticiones a
 * la vez (precarga, permisos, polling del dashboard, recovery del realtime). Todas reciben 401. El
 * cierre de sesión que redirige al login es asíncrono (DataStore), así que durante esa ventana la app
 * sigue golpeando el servidor → el backend responde 429 ("demasiadas peticiones") y la vista queda en
 * bucle sin redirigir.
 *
 * En cuanto la capa de red ve el PRIMER 401 en un request autenticado, marca esta compuerta. El
 * [com.bendey.restaurant.core.network.interceptor.AuthInterceptor] entonces responde 401 localmente
 * (sin tocar la red) a las siguientes peticiones con Authorization, hasta que se inicia una sesión
 * nueva (login → `reset()`). Así se detiene la ráfaga al instante y el redirect al login gana.
 */
@Singleton
class SessionInvalidationGate @Inject constructor() {
    private val invalidated = AtomicBoolean(false)

    fun isInvalidated(): Boolean = invalidated.get()

    /** Marca la sesión como inválida. Devuelve true si fue el primero en marcarla. */
    fun markInvalidated(): Boolean = invalidated.compareAndSet(false, true)

    /** Se llama al iniciar una sesión nueva (login) para volver a permitir peticiones. */
    fun reset() = invalidated.set(false)
}
