package com.bendey.restaurant.navigation

import com.bendey.restaurant.core.domain.connectivity.ReachabilityLevel
import com.bendey.restaurant.core.realtime.dispatcher.ConnectionState
import com.bendey.restaurant.core.ui.components.BendeyConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStatusTest {
    private val R = ReachabilityLevel.REACHABLE

    @Test
    fun sinRedSiempreEsOffline_aunqueTodoLoDemasDigaBien() {
        ConnectionState.entries.forEach {
            assertEquals(BendeyConnectionStatus.OFFLINE, resolveConnectionStatus(it, hasNetwork = false, reachability = R))
        }
    }

    @Test
    fun backendInalcanzableEsOffline_aunConRedYSocketReady() {
        assertEquals(
            BendeyConnectionStatus.OFFLINE,
            resolveConnectionStatus(ConnectionState.READY, true, ReachabilityLevel.UNREACHABLE),
        )
    }

    @Test
    fun nuncaEsVerdeSinComprobar() {
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.READY, true, ReachabilityLevel.UNKNOWN))
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.READY, true, ReachabilityLevel.DEGRADED))
    }

    @Test
    fun conRedBackendYSocketAutenticadoEsOnline() {
        assertEquals(BendeyConnectionStatus.ONLINE, resolveConnectionStatus(ConnectionState.READY, true, R))
    }

    @Test
    fun conRedYSocketConectandoOReconectandoEsConectando() {
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.CONNECTING, true, R))
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.RECONNECTING, true, R))
    }

    @Test
    fun conBackendComprobadoYSocketApagadoAPropositoEsOnline() {
        assertEquals(BendeyConnectionStatus.ONLINE, resolveConnectionStatus(ConnectionState.DISCONNECTED, true, R))
    }
}
