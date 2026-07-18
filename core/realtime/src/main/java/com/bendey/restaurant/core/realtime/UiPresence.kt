package com.bendey.restaurant.core.realtime

data class DashboardRange(val from: String, val to: String)

/** Indica qué vistas están montadas para recovery acotado. Puerto de `uiPresence.ts` (Tauri). */
object UiPresence {
    @Volatile var kitchen: Boolean = false
    @Volatile var posOrders: Boolean = false
    @Volatile var caja: Boolean = false
    @Volatile var dashboard: Boolean = false
    @Volatile var dashboardRange: DashboardRange? = null

    private val _sessions = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    val sessions: Set<Int> get() = _sessions

    // kitchen/posOrders/caja: asignar directamente (`UiPresence.kitchen = true`) — un `fun set<X>(Boolean)`
    // explícito chocaría en JVM con el setter autogenerado de la propiedad (mismo nombre + firma).

    fun setDashboard(active: Boolean, range: DashboardRange? = null) {
        dashboard = active
        dashboardRange = if (active) range else null
    }

    fun trackSession(sessionId: Int, active: Boolean) {
        if (active) _sessions.add(sessionId) else _sessions.remove(sessionId)
    }

    fun resetForTests() {
        kitchen = false
        posOrders = false
        caja = false
        dashboard = false
        dashboardRange = null
        _sessions.clear()
    }
}
