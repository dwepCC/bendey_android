package com.bendey.restaurant.core.domain.pos

/** Entrada para ítem manual sin catálogo — paridad `ManualCartLine` (Capacitor). */
data class ManualProductInput(
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val notes: String = "",
    val code: String = "MANUAL",
    val igvAffectationType: String = "10",
    val priceIncludesIgv: Boolean = true,
)
