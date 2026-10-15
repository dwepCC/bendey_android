package com.bendey.restaurant.core.data.session

/**
 * Cuándo se puede guardar una sesión ACTUALIZADA (permisos, tipo de empleado…) sin que sea un login.
 *
 * EL BUCLE QUE EVITA (ráfagas de 500–700 peticiones por minuto, cortadas por el servidor con 429):
 * con el token vencido, el refresco de permisos recibía 401, la capa de red cerraba la sesión y el
 * refresco —que traga el error y devuelve la sesión tal cual— la volvía a guardar con
 * `applyUserSession`, que además reabre la compuerta de red. La app quedaba "con sesión" otra vez con el
 * mismo token muerto, reconectaba el tiempo real, registraba el push, recargaba toda la configuración,
 * recibía 401 y empezaba de nuevo, una vez por segundo, sin llegar nunca al login.
 *
 * Una actualización solo se escribe si la sesión sigue VIVA y es LA MISMA que se refrescó: la compuerta
 * no está cerrada por un 401 y tanto la sesión guardada como el token vigente son los del refresco. Si
 * entre medio se cerró la sesión (o hubo otro login), no se toca nada.
 */
internal object SessionRefreshPolicy {
    fun puedeGuardarActualizacion(
        compuertaCerrada: Boolean,
        tokenDeLaSesionGuardada: String?,
        tokenVigente: String?,
        tokenRefrescado: String,
    ): Boolean {
        if (compuertaCerrada) return false
        if (tokenRefrescado.isBlank()) return false
        return tokenDeLaSesionGuardada == tokenRefrescado && tokenVigente == tokenRefrescado
    }
}
