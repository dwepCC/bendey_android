package com.bendey.restaurant.core.domain.permission

/** Features UI alineadas con `restaurantPermissions.ts` (Capacitor). */
enum class RestaurantFeature {
    PRODUCTOS,
    MODIFICADORES,
    MESAS,
    POS,
    SALAS,
    MESA,
    COMANDAS,
    CERRAR_MESA,
    VENTAS,
    CAJA,
    CLIENTES,
    REPARTIDORES,
    /** Vista de reparto (tablero de solo lectura): mismo permiso d.v que Repartidores. */
    ENTREGAS,
    DASHBOARD,
    CONFIGURACION,
    IMPRESORAS,
    REPORTES,
    COMPRAS,
    PROVEEDORES,
    /** Propinas: pantalla abierta a todo el personal (cada pestaña pide su permiso). */
    PROPINAS,
    /** Reparto del recargo al consumo: ver con c.v o s.m. */
    RC_REPARTO,
}
