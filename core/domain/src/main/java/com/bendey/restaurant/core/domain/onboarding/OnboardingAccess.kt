package com.bendey.restaurant.core.domain.onboarding

import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * El checklist es del tenant y solo del administrador de SESIÓN COMPLETA. Quien opera con el PIN de
 * turno (aunque su perfil tenga el permiso de administración) usa un token sin permisos de
 * configuración: el backend le respondería 403, así que ni se le pide.
 *
 * `authMethod == "pin"` es la única señal fiable del PIN (ver ComprasViewModel); `staffId` no sirve.
 */
fun UserSession?.canSeeOnboarding(): Boolean =
    this != null && !user.isPinSession && RestaurantPermissions.isRestaurantAdmin(restaurantPermissions)

/**
 * Pestaña de Configuración a la que el checklist quiere llegar. Configuración no admite deep-link
 * (la ruta no lleva argumentos y no se toca la navegación), así que el destino se deja aquí y
 * `ConfiguracionViewModel` lo consume al crearse. Es una pista de un solo uso con caducidad corta:
 * si por algo la pantalla no la consume, no debe cambiar la pestaña de una visita posterior.
 */
object ConfigTabHint {
    const val OPERACION = "OPERACION"
    const val MENU_DIGITAL = "MENU_DIGITAL"
    const val BRANCHES = "BRANCHES"

    /** Vida máxima de la pista: navegar y crear la pantalla tarda milisegundos. */
    const val TTL_MS = 5_000L

    data class Request(val tab: String, val requestedAtMs: Long)

    private val _pending = MutableStateFlow<Request?>(null)
    val pending: StateFlow<Request?> = _pending.asStateFlow()

    fun request(tab: String, nowMs: Long = System.currentTimeMillis()) {
        _pending.value = Request(tab, nowMs)
    }

    fun consume() {
        _pending.value = null
    }

    /** La pestaña pedida, o null si no hay pista o ya caducó. */
    fun freshTab(request: Request?, nowMs: Long = System.currentTimeMillis()): String? =
        request?.takeIf { nowMs - it.requestedAtMs in 0..TTL_MS }?.tab
}
