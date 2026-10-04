package com.bendey.restaurant.navigation

import com.bendey.restaurant.core.realtime.dispatcher.ConnectionState
import com.bendey.restaurant.core.ui.components.BendeyConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStatusTest {

    @Test
    fun sinRedSiempreEsOffline_aunqueElSocketDigaReady() {
        ConnectionState.entries.forEach {
            assertEquals(BendeyConnectionStatus.OFFLINE, resolveConnectionStatus(it, hasNetwork = false))
        }
    }

    @Test
    fun conRedYSocketAutenticadoEsOnline() {
        assertEquals(BendeyConnectionStatus.ONLINE, resolveConnectionStatus(ConnectionState.READY, true))
    }

    @Test
    fun conRedYSocketConectandoOReconectandoEsConectando() {
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.CONNECTING, true))
        assertEquals(BendeyConnectionStatus.CONNECTING, resolveConnectionStatus(ConnectionState.RECONNECTING, true))
    }

    @Test
    fun conRedYSocketApagadoAPropositoNoAfirmaFalloDeConexion() {
        assertEquals(BendeyConnectionStatus.ONLINE, resolveConnectionStatus(ConnectionState.DISCONNECTED, true))
    }
}
