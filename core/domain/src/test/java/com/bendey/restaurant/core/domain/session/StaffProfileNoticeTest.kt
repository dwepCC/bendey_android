package com.bendey.restaurant.core.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaffProfileNoticeTest {
    private val hoy = "2026-11-07"

    private fun show(staffId: Int? = null, closed: Boolean = false, persisted: String? = null) =
        StaffProfileNotice.shouldShow(staffId, closed, persisted, hoy)

    @Test fun sinPerfilSeAvisa() {
        assertTrue(show(staffId = null))
        assertTrue(show(staffId = 0))
    }

    @Test fun conPerfilNoSeAvisa() = assertFalse(show(staffId = 12))

    @Test fun cerrarloLoOcultaEnEstaSesion() = assertFalse(show(closed = true))

    @Test fun cerradoHoyNoReapareceHastaManana() {
        assertFalse(show(persisted = hoy))
        assertTrue(show(persisted = "2026-11-06"))
    }

    @Test fun cadaUsuarioCierraElSuyo() {
        assertEquals("staff_profile_notice_dismissed:demosri:1", StaffProfileNotice.dismissKey("demosri", 1))
    }

    @Test fun elMensajeIndicaDondeActivarlo() = assertTrue(StaffProfileNotice.MESSAGE.contains("Usuarios → Perfil restaurante"))
}
