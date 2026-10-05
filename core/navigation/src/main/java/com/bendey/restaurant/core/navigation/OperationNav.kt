package com.bendey.restaurant.core.navigation

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

/**
 * Qué ve cada rol (R2b, matriz de UX-REDESIGN 5.3; idéntica a `buildNavModel` de Tauri):
 *   mozo -> Mesas | cocina -> nada (quiosco) | repartidor -> Entregas |
 *   cajero -> Hoy, Mesas, Vender, Cocina, Caja | admin -> esas 5 + "Mi negocio".
 * Los PERMISOS no cambian: cada entrada exige el mismo permiso que su ruta ([canAccessRoute]).
 * Lógica pura (sin Compose) para poder probar la matriz rol x entrada con JUnit.
 */
object OperationNav {

    /** Quien administra el negocio: permiso s.m, o tipo admin/supervisor (como en Tauri). */
    fun isBusinessManager(permissions: List<String>, employeeType: String?): Boolean {
        if (RestaurantPermissions.isRestaurantAdmin(permissions)) return true
        val et = employeeType?.lowercase().orEmpty()
        return et == "admin" || et == "supervisor"
    }

    /**
     * Entradas de primer nivel de la barra de operación, en orden canónico. "Entregas" no va en la barra
     * del administrador (son 5 + Mi negocio): la tiene en Mi negocio > Clientes > Repartidores y entregas.
     */
    fun operationBar(permissions: List<String>, employeeType: String?): List<TopLevelDestination> {
        val manager = isBusinessManager(permissions, employeeType)
        return TopLevelDestination.bottomBarDestinations.filter {
            if (!canAccessRoute(it.route, permissions, employeeType)) return@filter false
            !(it == TopLevelDestination.ENTREGAS && manager)
        }
    }

    /**
     * Cocina: pantalla única de comandas (monitor fijo) sin barra inferior, menú ni acceso a otras
     * pantallas. Solo cuando lo único que puede abrir es Cocina y no administra.
     */
    fun isKitchenKiosk(permissions: List<String>, employeeType: String?): Boolean =
        !isBusinessManager(permissions, employeeType) &&
            operationBar(permissions, employeeType) == listOf(TopLevelDestination.COCINA)

    /** "Mi negocio" lo ve solo quien administra. */
    fun showsMiNegocio(permissions: List<String>, employeeType: String?): Boolean =
        isBusinessManager(permissions, employeeType)
}
