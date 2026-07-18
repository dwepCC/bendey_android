package com.bendey.restaurant.core.realtime.connection

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

data class ConnectionSession(
    val isAuthenticated: Boolean,
    val restaurantPermissions: List<String>,
)

/** Criterio de conexión WS — independiente del sonido. Puerto de `connection/manager.ts` (Tauri). */
object RealtimeConnectionPolicy {
    fun shouldConnect(session: ConnectionSession): Boolean {
        if (!session.isAuthenticated) return false
        return RestaurantPermissions.canReceiveRealtimeEvents(session.restaurantPermissions)
    }
}
