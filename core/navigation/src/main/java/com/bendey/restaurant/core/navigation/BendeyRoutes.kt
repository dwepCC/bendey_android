package com.bendey.restaurant.core.navigation

object BendeyRoutes {
    const val WELCOME = "welcome"
    const val REGISTER = "register"
    const val REGISTER_SUCCESS = "register_success/{restaurantName}"
    const val HOME = "home"
    const val PIN = "pin/{station}"
    const val LOGIN = "login"
    const val MAIN = "main"
    /** Wizard de configuración inicial (R4): pantalla completa dentro del shell. */
    const val WIZARD = "wizard"

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
    const val PERFIL = "perfil"
    const val REPARTIDORES = "repartidores"
    /** Vista de reparto (R2b): tablero de entregas de solo lectura; destino único del repartidor. */
    const val ENTREGAS = "entregas"
    /** Índice de tarjetas de gestión (R2b): reemplaza al drawer plano. Solo quien administra. */
    const val MI_NEGOCIO = "mi_negocio"
    const val REPORTES = "reportes"
    const val PRINTING_TEST = "printing_test"
    const val SUSCRIPCION = "suscripcion"
    const val COMPRAS = "compras"
    const val PROVEEDORES = "proveedores"
    /** Centro de ayuda (R10.7): lo ve cualquier puesto. */
    const val AYUDA = "ayuda"

    fun pin(station: String): String = "pin/$station"

    fun registerSuccess(restaurantName: String): String {
        val encoded = java.net.URLEncoder.encode(restaurantName, Charsets.UTF_8.name())
        return "register_success/$encoded"
    }

    fun mesa(sessionId: Int): String = "mesa/$sessionId"

    private val bottomBarRoutes = setOf(DASHBOARD, POS, MESAS, COCINA, CAJA, ENTREGAS)

    private val managementRoutes = setOf(
        VENTAS, PRODUCTOS, CLIENTES, CONFIGURACION, REPARTIDORES, REPORTES,
        MODIFICADORES, AREAS_PREPARACION, COMBOS, MESAS_ADMIN, COMPRAS, PROVEEDORES, AYUDA, MI_NEGOCIO,
    )

    fun showsBottomBar(route: String?): Boolean {
        if (route == null) return false
        if (route.startsWith("mesa/")) return false
        return route in bottomBarRoutes || route in managementRoutes
    }

    fun showsGlobalHeader(route: String?): Boolean {
        if (route == null) return false
        if (route.startsWith("mesa/")) return false
        if (route == PRINTING_TEST || route == WIZARD) return false
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
