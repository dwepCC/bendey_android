package com.bendey.restaurant.core.domain.permission

import com.bendey.restaurant.core.domain.model.PinStation

object RestaurantPermissions {

    fun defaultRoute(permissions: List<String>, employeeType: String?): String {
        val et = employeeType?.lowercase().orEmpty()
        if ((et == "admin" || et == "cashier" || et == "supervisor") && hasDashboard(permissions)) {
            return "dashboard"
        }
        val order = listOf(
            "pos" to hasPos(permissions),
            "salas" to hasSalas(permissions),
            "cocina" to hasComandas(permissions),
            "ventas" to hasVentas(permissions),
            "caja" to hasCaja(permissions),
        )
        return order.firstOrNull { it.second }?.first ?: "cocina"
    }

    fun hasDashboard(permissions: List<String>): Boolean = permissions.contains("c.v")

    fun hasPos(permissions: List<String>): Boolean = permissions.contains("p.u")

    fun hasSalas(permissions: List<String>): Boolean = permissions.contains("t.v")

    fun hasComandas(permissions: List<String>): Boolean = permissions.contains("k.v")

    fun hasVentas(permissions: List<String>): Boolean =
        permissions.contains("o.ch") || permissions.contains("c.v")

    fun hasCaja(permissions: List<String>): Boolean = permissions.contains("c.v")

    /** Descuento en cobro — admin/supervisor o permiso o.ch */
    fun canApplyCheckoutDiscount(permissions: List<String>, employeeType: String?): Boolean {
        val et = employeeType?.lowercase().orEmpty()
        if (et == "admin" || et == "supervisor") return true
        return permissions.contains("o.ch")
    }

    fun postLoginRoute(permissions: List<String>, station: PinStation): String {
        if (station == PinStation.KITCHEN && hasComandas(permissions)) return "cocina"
        return defaultRoute(permissions, station.routeKey)
    }
}
