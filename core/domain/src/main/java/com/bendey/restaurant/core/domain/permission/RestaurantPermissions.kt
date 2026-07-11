package com.bendey.restaurant.core.domain.permission

import com.bendey.restaurant.core.domain.model.PinStation

object RestaurantPermissions {

    const val PERM_CAJA = "c.v"
    const val PERM_POS = "p.u"
    const val PERM_SALAS = "t.v"
    const val PERM_MESA = "t.o"
    const val PERM_ORDERS_CREATE = "o.c"
    const val PERM_COMANDAS = "k.v"
    const val PERM_KITCHEN_UPDATE = "k.u"
    const val PERM_ORDERS_CHARGE = "o.ch"
    const val PERM_PRODUCTOS = "g.p"
    const val PERM_REPARTIDORES = "d.v"
    const val PERM_ADMIN = "s.m"

    fun hasPermission(permissions: List<String>, perm: String): Boolean =
        permissions.contains(perm)

    fun hasOperationalAccess(permissions: List<String>): Boolean =
        permissions.isNotEmpty()

    fun featureAllowed(permissions: List<String>, feature: RestaurantFeature): Boolean {
        if (permissions.isEmpty()) return false
        return when (feature) {
            RestaurantFeature.VENTAS ->
                hasPermission(permissions, PERM_ORDERS_CHARGE) || hasPermission(permissions, PERM_CAJA)
            RestaurantFeature.REPORTES ->
                hasPermission(permissions, PERM_PRODUCTOS) ||
                    hasPermission(permissions, PERM_ORDERS_CHARGE) ||
                    hasPermission(permissions, PERM_CAJA)
            RestaurantFeature.DASHBOARD ->
                hasPermission(permissions, PERM_CAJA) || hasPermission(permissions, PERM_ADMIN)
            RestaurantFeature.CONFIGURACION ->
                isRestaurantAdmin(permissions)
            RestaurantFeature.IMPRESORAS ->
                canConfigureDevicePrinters(permissions, null)
            else -> hasPermission(permissions, requiredPerm(feature))
        }
    }

    fun requiredPerm(feature: RestaurantFeature): String = when (feature) {
        RestaurantFeature.PRODUCTOS, RestaurantFeature.MODIFICADORES, RestaurantFeature.MESAS -> PERM_PRODUCTOS
        RestaurantFeature.POS -> PERM_POS
        RestaurantFeature.SALAS -> PERM_SALAS
        RestaurantFeature.MESA -> PERM_MESA
        RestaurantFeature.COMANDAS -> PERM_COMANDAS
        RestaurantFeature.CERRAR_MESA, RestaurantFeature.CLIENTES -> PERM_ORDERS_CHARGE
        RestaurantFeature.VENTAS -> PERM_ORDERS_CHARGE
        RestaurantFeature.CAJA, RestaurantFeature.DASHBOARD -> PERM_CAJA
        RestaurantFeature.REPARTIDORES -> PERM_REPARTIDORES
        RestaurantFeature.REPORTES -> PERM_PRODUCTOS
        RestaurantFeature.CONFIGURACION, RestaurantFeature.IMPRESORAS -> PERM_ADMIN
    }

    fun isRestaurantAdmin(permissions: List<String>): Boolean =
        hasPermission(permissions, PERM_ADMIN)

    /** App Android nativa — equivalente a `isNativePrintAvailable()` en Capacitor. */
    fun canConfigureDevicePrinters(
        permissions: List<String>,
        employeeType: String?,
    ): Boolean {
        if (isRestaurantAdmin(permissions)) return true
        val et = employeeType?.lowercase().orEmpty()
        if (et in setOf("waiter", "mozo", "cashier", "cook", "driver", "admin", "supervisor")) {
            return true
        }
        return hasPermission(permissions, PERM_MESA) ||
            hasPermission(permissions, PERM_SALAS) ||
            hasPermission(permissions, PERM_ORDERS_CREATE) ||
            hasPermission(permissions, PERM_ORDERS_CHARGE) ||
            hasPermission(permissions, PERM_COMANDAS) ||
            hasPermission(permissions, PERM_POS)
    }

    fun canAccessAppSettings(permissions: List<String>, employeeType: String?): Boolean =
        isRestaurantAdmin(permissions) || canConfigureDevicePrinters(permissions, employeeType)

    fun canManageRestaurantSettings(permissions: List<String>): Boolean =
        isRestaurantAdmin(permissions)

    fun canManageCashSettings(permissions: List<String>, employeeType: String?): Boolean {
        if (isRestaurantAdmin(permissions)) return true
        val et = employeeType?.lowercase().orEmpty()
        return et == "admin" || et == "supervisor"
    }

    fun canViewCashSettings(permissions: List<String>, employeeType: String?): Boolean =
        canManageCashSettings(permissions, employeeType) || hasPermission(permissions, PERM_CAJA)

    fun canViewAllCashSessions(permissions: List<String>, employeeType: String?): Boolean =
        canManageCashSettings(permissions, employeeType)

    fun canChargeCashByRole(employeeType: String?, permissions: List<String>): Boolean {
        val et = employeeType?.lowercase().orEmpty()
        if (et == "waiter" || et == "mozo") return false
        return hasPermission(permissions, PERM_CAJA) ||
            et in setOf("cashier", "admin", "supervisor")
    }

    fun canAccessFeature(
        permissions: List<String>,
        feature: RestaurantFeature,
        employeeType: String? = null,
    ): Boolean = when (feature) {
        RestaurantFeature.CONFIGURACION -> canManageRestaurantSettings(permissions)
        RestaurantFeature.IMPRESORAS -> canConfigureDevicePrinters(permissions, employeeType)
        else -> featureAllowed(permissions, feature)
    }

    /** Ruta inicial alineada con `defaultRouteForPermissions` (Capacitor). */
    fun defaultRoute(permissions: List<String>, employeeType: String?): String {
        if (permissions.isEmpty()) return "cocina"
        val et = employeeType?.lowercase().orEmpty()
        if ((et == "admin" || et == "cashier" || et == "supervisor") &&
            featureAllowed(permissions, RestaurantFeature.DASHBOARD)
        ) {
            return "dashboard"
        }
        if (et == "waiter" || et == "mozo") {
            if (featureAllowed(permissions, RestaurantFeature.SALAS)) return "mesas"
            if (featureAllowed(permissions, RestaurantFeature.POS)) return "pos"
        }
        if (et == "cook" || et == "cocinero") {
            if (featureAllowed(permissions, RestaurantFeature.COMANDAS)) return "cocina"
        }
        val order = listOf(
            RestaurantFeature.POS to "pos",
            RestaurantFeature.SALAS to "mesas",
            RestaurantFeature.COMANDAS to "cocina",
            RestaurantFeature.VENTAS to "ventas",
            RestaurantFeature.CAJA to "caja",
            RestaurantFeature.CLIENTES to "clientes",
            RestaurantFeature.CONFIGURACION to "configuracion",
        )
        return order.firstOrNull { featureAllowed(permissions, it.first) }?.second ?: "cocina"
    }

    fun canChargeOrders(permissions: List<String>): Boolean =
        hasPermission(permissions, PERM_ORDERS_CHARGE)

    fun canAnularComanda(permissions: List<String>): Boolean =
        isRestaurantAdmin(permissions) ||
            hasPermission(permissions, PERM_ORDERS_CHARGE) ||
            hasPermission(permissions, PERM_POS)

    /** Cambiar estados de comandas en cocina (pendiente → preparación → lista → entregada). */
    fun canManageKitchenComandas(permissions: List<String>): Boolean =
        isRestaurantAdmin(permissions) ||
            hasPermission(permissions, PERM_KITCHEN_UPDATE) ||
            hasPermission(permissions, PERM_POS)

    fun canAssignTableStaff(permissions: List<String>): Boolean =
        isRestaurantAdmin(permissions)

    fun hasDashboard(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.DASHBOARD)

    fun hasPos(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.POS)

    fun hasSalas(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.SALAS)

    fun hasComandas(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.COMANDAS)

    fun hasVentas(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.VENTAS)

    fun hasCaja(permissions: List<String>): Boolean =
        featureAllowed(permissions, RestaurantFeature.CAJA)

    fun canApplyCheckoutDiscount(permissions: List<String>, employeeType: String?): Boolean {
        val et = employeeType?.lowercase().orEmpty()
        if (et == "admin" || et == "supervisor") return true
        return hasPermission(permissions, PERM_ORDERS_CHARGE)
    }

    fun postLoginRoute(
        permissions: List<String>,
        station: PinStation,
        employeeType: String?,
    ): String {
        if (station == PinStation.KITCHEN && hasComandas(permissions)) return "cocina"
        return defaultRoute(permissions, employeeType)
    }
}
