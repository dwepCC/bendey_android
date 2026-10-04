package com.bendey.restaurant.core.domain.auth

/**
 * PIN inicial del administrador: lo genera el backend (aleatorio por tenant) y llega UNA sola vez
 * en la respuesta del registro. Vive solo en memoria de proceso: no se persiste, no va por
 * argumentos de navegación ni por SavedState, y se descarta al salir de la pantalla de éxito.
 * Si la pantalla se abre sin PIN en el holder (por ejemplo tras morir el proceso), se muestra el texto sin PIN.
 */
object InitialAdminPinHolder {
    @Volatile
    private var pending: String? = null

    fun stash(raw: String?) {
        pending = normalizeInitialPin(raw)
    }

    fun peek(): String? = pending

    fun clear() {
        pending = null
    }
}

/** Acepta solo PIN numéricos de 4 a 6 dígitos (el backend genera 4); cualquier otra cosa se ignora. */
fun normalizeInitialPin(raw: String?): String? {
    val pin = raw?.trim() ?: return null
    return if (pin.length in 4..6 && pin.all { it in '0'..'9' }) pin else null
}

/** Texto de apoyo bajo el PIN; null si no hay PIN válido (la pantalla muestra el texto sin PIN). */
fun initialPinNotice(pin: String?): String? {
    if (normalizeInitialPin(pin) == null) return null
    return "Sirve para entrar a las estaciones Cocina y Delivery. Anótalo: no se vuelve a mostrar. " +
        "Puedes cambiarlo en Ajustes → Operación → Usuarios del restaurante."
}
