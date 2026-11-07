package com.bendey.restaurant.core.domain.session

/**
 * Aviso para quien entra a Resto sin perfil de restaurante. Un administrador puede operar así, pero no
 * entra con PIN ni recibe notificaciones, y sin este aviso no tiene cómo saber por qué. El perfil no se
 * crea solo: lo activa el tenant desde su panel. Se muestra una vez al día por usuario hasta que exista.
 */
object StaffProfileNotice {
    const val TITLE = "Tu usuario no tiene perfil de restaurante"

    const val MESSAGE =
        "Puedes seguir trabajando, pero mientras no lo actives no podrás entrar con PIN, " +
            "recibir notificaciones de pedidos con la app cerrada ni operar entregas.\n\n" +
            "Para activarlo, entra a Bendey ERP → Usuarios → Perfil restaurante, elige el tipo de " +
            "empleado y asigna un PIN. Luego cierra sesión y vuelve a entrar."

    fun missingProfile(staffId: Int?): Boolean = staffId == null || staffId <= 0

    fun dismissKey(tenantSlug: String?, userId: Int): String =
        "staff_profile_notice_dismissed:${tenantSlug.orEmpty()}:$userId"

    fun shouldShow(staffId: Int?, closedThisSession: Boolean, persistedDismissDay: String?, today: String): Boolean {
        if (!missingProfile(staffId)) return false
        if (closedThisSession) return false
        return persistedDismissDay != today
    }
}
