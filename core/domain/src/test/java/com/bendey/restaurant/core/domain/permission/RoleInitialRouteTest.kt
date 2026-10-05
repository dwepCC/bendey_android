package com.bendey.restaurant.core.domain.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * R2a/R2b: tabla rol -> ruta inicial. Es la MISMA que `defaultRouteForPermissions` de Tauri
 * (`restaurantPermissions.ts`, con rutas /dashboard /salas /comandas /entregas):
 *
 *   admin (s.m)      -> dashboard      cajero (c.v) -> dashboard
 *   mozo (t.v)       -> mesas          cocina (k.v) -> cocina
 *   repartidor (d.v) -> entregas
 *
 * Si cambias una fila, cámbiala también allá (y su test).
 */
class RoleInitialRouteTest {
    private val admin = listOf("s.m", "c.v", "p.u", "t.v", "t.o", "o.c", "o.ch", "k.v", "g.p", "d.v")
    private val cashier = listOf("c.v", "p.u", "t.v", "o.ch")
    private val waiter = listOf("t.v", "t.o", "o.c")
    private val cook = listOf("k.v", "k.u")
    private val driver = listOf("d.v")

    @Test
    fun `tabla rol a ruta inicial`() {
        assertEquals("dashboard", RestaurantPermissions.defaultRoute(admin, "admin"))
        assertEquals("dashboard", RestaurantPermissions.defaultRoute(listOf("s.m"), "admin"))
        assertEquals("dashboard", RestaurantPermissions.defaultRoute(cashier, "cashier"))
        assertEquals("mesas", RestaurantPermissions.defaultRoute(waiter, "waiter"))
        assertEquals("cocina", RestaurantPermissions.defaultRoute(cook, "cook"))
        assertEquals("entregas", RestaurantPermissions.defaultRoute(driver, "driver"))
    }

    @Test
    fun `dashboard se abre con c punto v o con s punto m en ambas plataformas`() {
        assertTrue(RestaurantPermissions.featureAllowed(listOf("c.v"), RestaurantFeature.DASHBOARD))
        assertTrue(RestaurantPermissions.featureAllowed(listOf("s.m"), RestaurantFeature.DASHBOARD))
        assertFalse(RestaurantPermissions.featureAllowed(waiter, RestaurantFeature.DASHBOARD))
        assertFalse(RestaurantPermissions.featureAllowed(cook, RestaurantFeature.DASHBOARD))
    }

    @Test
    fun `la ruta inicial nunca es una ruta sin permiso (sin bucle con el guard)`() {
        val roles = mapOf(
            "admin" to admin, "cashier" to cashier, "waiter" to waiter, "cook" to cook, "driver" to driver,
        )
        roles.forEach { (type, perms) ->
            val route = RestaurantPermissions.defaultRoute(perms, type)
            val feature = when (route) {
                "dashboard" -> RestaurantFeature.DASHBOARD
                "mesas" -> RestaurantFeature.SALAS
                "pos" -> RestaurantFeature.POS
                "cocina" -> RestaurantFeature.COMANDAS
                "entregas" -> RestaurantFeature.ENTREGAS
                else -> null
            }
            if (feature != null) assertTrue(RestaurantPermissions.featureAllowed(perms, feature), "$type -> $route")
        }
    }

    @Test
    fun `repartidor sin d punto v cae en perfil, nunca en una ruta inaccesible`() {
        assertEquals("perfil", RestaurantPermissions.defaultRoute(listOf("t.o"), "driver"))
    }

    @Test
    fun `sin tipo de empleado, d punto v solo aterriza en entregas`() {
        assertEquals("entregas", RestaurantPermissions.defaultRoute(driver, null))
    }

    /** Mismos conjuntos de permisos que `restaurantNavigation.test.ts` de Tauri. */
    @Test
    fun `misma tabla que el test de Tauri`() {
        val adminT = listOf("s.m", "c.v", "g.p", "p.u", "t.v", "t.o", "k.v", "k.u", "o.ch", "d.v")
        assertEquals("dashboard", RestaurantPermissions.defaultRoute(adminT, "admin"))
        assertEquals("mesas", RestaurantPermissions.defaultRoute(listOf("t.v", "t.o"), "waiter"))
        assertEquals("dashboard", RestaurantPermissions.defaultRoute(listOf("c.v", "p.u", "t.v", "o.ch", "k.v"), "cashier"))
        assertEquals("cocina", RestaurantPermissions.defaultRoute(listOf("k.v", "k.u"), "cook"))
        assertEquals("entregas", RestaurantPermissions.defaultRoute(listOf("d.v"), "driver"))
    }
}
