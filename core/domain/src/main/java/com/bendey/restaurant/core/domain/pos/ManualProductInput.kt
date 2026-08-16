package com.bendey.restaurant.core.domain.pos

/** Entrada para ítem manual sin catálogo — paridad `ManualCartLine` (Capacitor). */
data class ManualProductInput(
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val notes: String = "",
    val code: String = "MANUAL",
    // Vacio = «el usuario no eligio». El backend aplica entonces la politica del tenant, que en un
    // negocio acogido a la Ley 27037 es «20 Exonerado». Escribir "10" aca lo declaraba Gravado sin
    // que nadie lo hubiera elegido.
    val igvAffectationType: String = "",
    val priceIncludesIgv: Boolean = true,
)
