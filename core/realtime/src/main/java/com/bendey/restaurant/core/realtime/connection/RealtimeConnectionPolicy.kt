package com.bendey.restaurant.core.realtime.connection

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

data class ConnectionSession(
    val isAuthenticated: Boolean,
    val restaurantPermissions: List<String>,
    /**
     * Si la aplicación está en pantalla. Ver [AppForeground] para por qué importa.
     *
     * Sin valor por defecto A PROPÓSITO: un `= true` dejaría compilar a quien agregue otra vía de
     * conexión sin pensar en el segundo plano, y el problema volvería sin que nadie lo note.
     */
    val isForeground: Boolean,
)

/** Criterio de conexión WS — independiente del sonido. Puerto de `connection/manager.ts` (Tauri). */
object RealtimeConnectionPolicy {
    fun shouldConnect(session: ConnectionSession): Boolean {
        if (!session.isAuthenticated) return false
        // En segundo plano la conexión se cierra a propósito en vez de dejar que el servidor la mate a
        // los 90 s y el cliente reconecte en un bucle que no sirve a nadie.
        if (!session.isForeground) return false
        return RestaurantPermissions.canReceiveRealtimeEvents(session.restaurantPermissions)
    }
}
