package com.bendey.restaurant.core.domain.connectivity

import kotlin.test.Test
import kotlin.test.assertEquals

class BackendReachabilityTest {
    private fun probe(s: ReachabilityState, ok: Boolean, now: Long = 1_000_000, api: Long = 0) =
        BackendReachability.onProbe(s, ok, now, api)

    @Test fun sinSondaNuncaEsAlcanzable() {
        assertEquals(ReachabilityLevel.UNKNOWN, BackendReachability.level(ReachabilityState()))
    }

    @Test fun unExitoEsAlcanzable() {
        assertEquals(ReachabilityLevel.REACHABLE, BackendReachability.level(probe(ReachabilityState(), true)))
    }

    @Test fun unoYDosFallosDegradan_tresDesconectan() {
        var s = ReachabilityState()
        s = probe(s, false)
        assertEquals(ReachabilityLevel.DEGRADED, BackendReachability.level(s))
        s = probe(s, false)
        assertEquals(ReachabilityLevel.DEGRADED, BackendReachability.level(s))
        s = probe(s, false)
        assertEquals(ReachabilityLevel.UNREACHABLE, BackendReachability.level(s))
    }

    @Test fun unExitoRecuperaDeSinConexion() {
        val s = probe(ReachabilityState(3, true), true)
        assertEquals(ReachabilityLevel.REACHABLE, BackendReachability.level(s))
    }

    @Test fun respuestaDeApiRecienteCuentaComoVida() {
        val s = probe(ReachabilityState(2, true), ok = false, now = 100_000, api = 80_000)
        assertEquals(0, s.consecutiveFailures)
    }

    @Test fun respuestaDeApiVieja_noSalva() {
        val s = probe(ReachabilityState(0, true), ok = false, now = 200_000, api = 80_000)
        assertEquals(1, s.consecutiveFailures)
    }

    @Test fun cadenciaDeSonda() {
        assertEquals(30_000L, BackendReachability.nextDelayMs(ReachabilityState(0, true)))
        assertEquals(10_000L, BackendReachability.nextDelayMs(ReachabilityState(1, true)))
    }
}
