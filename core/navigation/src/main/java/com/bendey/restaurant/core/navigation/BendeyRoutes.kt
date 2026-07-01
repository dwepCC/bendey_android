package com.bendey.restaurant.core.navigation

object BendeyRoutes {
    const val WELCOME = "welcome"
    const val REGISTER = "register"
    const val REGISTER_SUCCESS = "register_success/{restaurantName}"
    const val HOME = "home"
    const val PIN = "pin/{station}"
    const val LOGIN = "login"
    const val MAIN = "main"

    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val MESAS = "mesas"
    const val MESAS_ADMIN = "mesas_admin"
    const val MESA = "mesa/{sessionId}"
    const val COCINA = "cocina"
    const val CAJA = "caja"
    const val VENTAS = "ventas"
    const val PRODUCTOS = "productos"
    const val CLIENTES = "clientes"
    const val MODIFICADORES = "modificadores"
    const val AREAS_PREPARACION = "areas_preparacion"
    const val COMBOS = "combos"
    const val CONFIGURACION = "configuracion"
    const val REPARTIDORES = "repartidores"
    const val PRINTING_TEST = "printing_test"

    fun pin(station: String): String = "pin/$station"

    fun registerSuccess(restaurantName: String): String {
        val encoded = java.net.URLEncoder.encode(restaurantName, Charsets.UTF_8.name())
        return "register_success/$encoded"
    }

    fun mesa(sessionId: Int): String = "mesa/$sessionId"

    private val bottomBarRoutes = setOf(DASHBOARD, POS, MESAS, COCINA)

    private val managementRoutes = setOf(
        CAJA, VENTAS, PRODUCTOS, CLIENTES, CONFIGURACION, REPARTIDORES,
        MODIFICADORES, AREAS_PREPARACION, COMBOS, MESAS_ADMIN,
    )

    fun showsBottomBar(route: String?): Boolean {
        if (route == null) return false
        if (route.startsWith("mesa/")) return false
        return route in bottomBarRoutes || route in managementRoutes
    }

    fun showsGlobalHeader(route: String?): Boolean {
        if (route == null) return false
        if (route.startsWith("mesa/")) return false
        if (route == PRINTING_TEST) return false
        return true
    }

    fun isMesaDetail(route: String?): Boolean = route?.startsWith("mesa/") == true

    /** Rutas de operación diaria (barra operativa / sin rail en tablet). */
    fun isOperationalRoute(route: String?): Boolean {
        if (route == null) return false
        if (isMesaDetail(route)) return true
        return route in bottomBarRoutes
    }
}
