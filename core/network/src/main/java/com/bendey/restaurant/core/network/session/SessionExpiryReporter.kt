package com.bendey.restaurant.core.network.session

/**
 * Puente entre la capa de red y la sesión: la implementación (en core/data) debe cerrar la sesión
 * del usuario y notificar a la UI cuando un request autenticado recibe 401 (token expirado/inválido).
 *
 * Se invoca desde un interceptor OkHttp, así que la implementación NO debe bloquear el hilo de red
 * (lanzar el cierre en su propio scope) y debe ser idempotente frente a ráfagas de 401 concurrentes.
 */
interface SessionExpiryReporter {
    fun reportSessionExpired()
}
