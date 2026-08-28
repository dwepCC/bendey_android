package com.bendey.restaurant.core.domain.sales

/**
 * COMO SE ATENDIO UNA VENTA: en mesa, para llevar, delivery o directa en el mostrador.
 *
 * El dato vive en la sesion de restaurante que origino la venta (`order_type`) y el POS ya lo usa
 * para decidir que pedir al cobrar. Lo que faltaba era poder verlo y filtrarlo en el listado de
 * ventas, que es donde el dueno mira al final del dia.
 *
 * LAS ETIQUETAS SON LAS DEL POS, no unas nuevas: quien vende en «Para llevar» tiene que encontrar
 * «Para llevar» en el reporte.
 *
 * Una venta sin sesion de restaurante —registrada desde el modulo de ventas, sin pasar por comanda—
 * no tiene tipo que mostrar. Se cuenta como directa, porque para el dueno es lo mismo: no fue mesa,
 * ni delivery, ni para llevar.
 */
enum class TipoDeAtencion(val codigo: String, val label: String) {
    TODAS("", "Toda atención"),
    SALA("dine_in", "En sala (mesas)"),
    LLEVAR("takeaway", "Para llevar"),
    DELIVERY("delivery", "Delivery"),
    DIRECTA("quick_sale", "Venta directa"),
}

private val ETIQUETAS = mapOf(
    "dine_in" to "Mesa",
    "takeaway" to "Para llevar",
    "delivery" to "Delivery",
    "quick_sale" to "Directa",
)

/** Etiqueta para mostrar en una fila. Sin tipo se muestra «Directa», que es lo que fue. */
fun etiquetaDeAtencion(orderType: String?): String {
    val t = orderType?.trim().orEmpty()
    if (t.isEmpty()) return "Directa"
    return ETIQUETAS[t] ?: t
}

/** Texto de la fila: el tipo y, si salio de una mesa, cual. */
fun atencionConMesa(orderType: String?, tableName: String?): String {
    val base = etiquetaDeAtencion(orderType)
    val mesa = tableName?.trim().orEmpty()
    return if (mesa.isEmpty()) base else "$base · $mesa"
}
