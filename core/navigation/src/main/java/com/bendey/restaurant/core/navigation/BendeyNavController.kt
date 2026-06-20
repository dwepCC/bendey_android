package com.bendey.restaurant.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions

/** Vuelve al dashboard limpiando pantallas de gestión apiladas encima. */
fun NavController.navigateToDashboard() {
    val reached = popBackStack(BendeyRoutes.DASHBOARD, inclusive = false)
    if (!reached) {
        navigate(BendeyRoutes.DASHBOARD, homeNavOptions())
    }
}

/** Destinos de la barra inferior (Inicio, POS, Mesas, Comandas). */
fun NavController.navigateToBottomBarDestination(route: String) {
    if (route == BendeyRoutes.DASHBOARD) {
        navigateToDashboard()
        return
    }
    navigate(route, bottomBarNavOptions(this))
}

/** Gestión desde el menú lateral — mantiene el dashboard como raíz del back stack. */
fun NavController.navigateToDrawerDestination(route: String) {
    navigate(route, drawerNavOptions(this))
}

private fun homeNavOptions() = navOptions {
    popUpTo(BendeyRoutes.DASHBOARD) {
        inclusive = false
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

private fun bottomBarNavOptions(navController: NavController) = navOptions {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

private fun drawerNavOptions(navController: NavController) = navOptions {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
}
