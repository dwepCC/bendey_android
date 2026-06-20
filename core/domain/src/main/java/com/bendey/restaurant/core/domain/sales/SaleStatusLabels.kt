package com.bendey.restaurant.core.domain.sales

fun billingStatusLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return when (raw.trim().lowercase()) {
        "pending" -> "Pendiente"
        "sent" -> "Enviado"
        "accepted" -> "Aceptado"
        "observed" -> "Aceptado con observaciones"
        "rejected" -> "Rechazado"
        "error" -> "Error de envío"
        "voided" -> "Anulado (RA)"
        else -> raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

fun saleStatusLabel(raw: String): String = when (raw.trim().lowercase()) {
    "paid", "pagado" -> "Pagado"
    "cancelled", "canceled", "cancelado" -> "Cancelado"
    "draft", "borrador" -> "Borrador"
    "open", "abierto" -> "Abierto"
    "issued", "emitido" -> "Emitido"
    else -> raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

fun saleStatusDisplayLabel(status: String, billingStatus: String?): String =
    billingStatus?.let { billingStatusLabel(it) }?.takeIf { it != "—" }
        ?: saleStatusLabel(status)
