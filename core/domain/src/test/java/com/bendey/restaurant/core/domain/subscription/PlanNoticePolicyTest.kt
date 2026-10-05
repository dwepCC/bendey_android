package com.bendey.restaurant.core.domain.subscription

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanNoticePolicyTest {
    private fun show(
        modal: Boolean = true,
        admin: Boolean = true,
        tier: String = "warning",
        closed: Boolean = false,
        persisted: String? = null,
        today: String = "2026-10-04",
    ) = PlanNoticePolicy.shouldShow(modal, admin, tier, closed, persisted, today)

    @Test fun diaDeLimaNoEsElDelDispositivo() {
        // 2026-10-05 03:00 UTC = 2026-10-04 22:00 en Lima.
        val ms = Instant.parse("2026-10-05T03:00:00Z").toEpochMilli()
        assertEquals("2026-10-04", PlanNoticePolicy.limaDay(ms))
    }

    @Test fun elBackendDecideCuando() = assertFalse(show(modal = false))

    @Test fun soloAdmin() = assertFalse(show(admin = false))

    @Test fun apareceSiNoSeCerro() = assertTrue(show())

    @Test fun cerradoHoyNoVuelve() = assertFalse(show(persisted = "2026-10-04"))

    @Test fun cerradoAyerVuelveHoy() = assertTrue(show(persisted = "2026-10-03"))

    @Test fun cerrarSiempreCierra_aunSuspendido() = assertFalse(show(tier = "suspended", closed = true))

    @Test fun suspendidoReapareceAlReabrirAunqueHayaCierrePersistido() =
        assertTrue(show(tier = "blocked", persisted = "2026-10-04"))

    @Test fun suspendidoNoPersiste() = assertFalse(PlanNoticePolicy.persistsDismissal("suspended"))

    @Test fun claveEsPorTenant() =
        assertEquals("subscription_expiry_modal_dismissed:abc", PlanNoticePolicy.dismissKey("abc"))
}
