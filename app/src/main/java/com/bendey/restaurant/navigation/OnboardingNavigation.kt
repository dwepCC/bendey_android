package com.bendey.restaurant.navigation

import androidx.navigation.NavController
import com.bendey.restaurant.core.domain.onboarding.ConfigTabHint
import com.bendey.restaurant.core.domain.onboarding.OnboardingDestination
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.canAccessRoute
import com.bendey.restaurant.core.navigation.navigateToBottomBarDestination
import com.bendey.restaurant.core.navigation.navigateToDrawerDestination

/**
 * Traduce el destino del checklist a una ruta REAL que ya existe (no se inventan rutas).
 * Configuración no admite deep-link a una pestaña: se navega a la pantalla y la pestaña se pide con
 * [ConfigTabHint].
 */
internal fun OnboardingDestination.route(): String = when (this) {
    OnboardingDestination.PRODUCTOS -> BendeyRoutes.PRODUCTOS
    OnboardingDestination.POS -> BendeyRoutes.POS
    OnboardingDestination.MESAS_ADMIN -> BendeyRoutes.MESAS_ADMIN
    OnboardingDestination.IMPRESORAS -> BendeyRoutes.PRINTING_TEST
    OnboardingDestination.CONFIG_OPERACION,
    OnboardingDestination.CONFIG_MENU_DIGITAL,
    OnboardingDestination.CONFIG_SUCURSALES,
    -> BendeyRoutes.CONFIGURACION
    OnboardingDestination.REPARTIDORES -> BendeyRoutes.REPARTIDORES
}

internal fun OnboardingDestination.configTab(): String? = when (this) {
    OnboardingDestination.CONFIG_OPERACION -> ConfigTabHint.OPERACION
    OnboardingDestination.CONFIG_MENU_DIGITAL -> ConfigTabHint.MENU_DIGITAL
    OnboardingDestination.CONFIG_SUCURSALES -> ConfigTabHint.BRANCHES
    else -> null
}

internal fun NavController.navigateToOnboardingDestination(
    destination: OnboardingDestination,
    permissions: List<String>,
    employeeType: String?,
    onShowMessage: (String) -> Unit,
) {
    val route = destination.route()
    if (!canAccessRoute(route, permissions, employeeType)) {
        onShowMessage("No tienes permiso para abrir esa pantalla.")
        return
    }
    destination.configTab()?.let(ConfigTabHint::request)
    when (destination) {
        OnboardingDestination.POS -> navigateToBottomBarDestination(route)
        // Igual que el botón "Impresoras" de Configuración: ruta suelta, sin tocar el back stack del drawer.
        OnboardingDestination.IMPRESORAS -> navigate(route)
        else -> navigateToDrawerDestination(route)
    }
}
