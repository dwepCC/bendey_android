package com.bendey.restaurant.core.navigation

import com.bendey.restaurant.core.domain.permission.RestaurantFeature
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

fun TopLevelDestination.requiredFeature(): RestaurantFeature = when (this) {
    TopLevelDestination.DASHBOARD -> RestaurantFeature.DASHBOARD
    TopLevelDestination.POS -> RestaurantFeature.POS
    TopLevelDestination.MESAS -> RestaurantFeature.SALAS
    TopLevelDestination.COCINA -> RestaurantFeature.COMANDAS
    TopLevelDestination.CAJA -> RestaurantFeature.CAJA
    TopLevelDestination.ENTREGAS -> RestaurantFeature.ENTREGAS
    TopLevelDestination.VENTAS -> RestaurantFeature.VENTAS
    TopLevelDestination.PRODUCTOS -> RestaurantFeature.PRODUCTOS
    TopLevelDestination.CLIENTES -> RestaurantFeature.CLIENTES
}

fun routeRequiredFeature(route: String?): RestaurantFeature? = when {
    route == null -> null
    route == BendeyRoutes.DASHBOARD -> RestaurantFeature.DASHBOARD
    route == BendeyRoutes.POS -> RestaurantFeature.POS
    route == BendeyRoutes.MESAS -> RestaurantFeature.SALAS
    route.startsWith("mesa/") -> RestaurantFeature.MESA
    route == BendeyRoutes.MESAS_ADMIN -> RestaurantFeature.MESAS
    route == BendeyRoutes.COCINA -> RestaurantFeature.COMANDAS
    route == BendeyRoutes.CAJA -> RestaurantFeature.CAJA
    route == BendeyRoutes.VENTAS -> RestaurantFeature.VENTAS
    route == BendeyRoutes.REPORTES -> RestaurantFeature.REPORTES
    route == BendeyRoutes.PRODUCTOS -> RestaurantFeature.PRODUCTOS
    route == BendeyRoutes.MODIFICADORES -> RestaurantFeature.MODIFICADORES
    route == BendeyRoutes.AREAS_PREPARACION -> RestaurantFeature.MODIFICADORES
    route == BendeyRoutes.COMBOS -> RestaurantFeature.PRODUCTOS
    route == BendeyRoutes.CLIENTES -> RestaurantFeature.CLIENTES
    route == BendeyRoutes.CONFIGURACION -> RestaurantFeature.CONFIGURACION
    route == BendeyRoutes.REPARTIDORES -> RestaurantFeature.REPARTIDORES
    route == BendeyRoutes.ENTREGAS -> RestaurantFeature.ENTREGAS
    route == BendeyRoutes.PRINTING_TEST -> RestaurantFeature.IMPRESORAS
    route == BendeyRoutes.COMPRAS -> RestaurantFeature.COMPRAS
    route == BendeyRoutes.PROVEEDORES -> RestaurantFeature.PROVEEDORES
    route == BendeyRoutes.SUSCRIPCION -> RestaurantFeature.CONFIGURACION
    else -> null
}

fun canAccessRoute(
    route: String?,
    permissions: List<String>,
    employeeType: String?,
): Boolean {
    // "Mi negocio" lo abre quien administra (s.m o tipo admin/supervisor), no una función suelta.
    if (route == BendeyRoutes.MI_NEGOCIO) return OperationNav.isBusinessManager(permissions, employeeType)
    val feature = routeRequiredFeature(route) ?: return true
    return RestaurantPermissions.canAccessFeature(permissions, feature, employeeType)
}
