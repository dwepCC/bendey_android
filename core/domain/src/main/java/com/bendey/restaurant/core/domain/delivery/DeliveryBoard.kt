package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.kitchen.kdsElapsedLabel
import com.bendey.restaurant.core.domain.kitchen.kdsElapsedMinutes
import com.bendey.restaurant.core.domain.kitchen.kdsParseInstant
import java.time.Instant

/**
 * Tablero de entregas activas (R10.9, SOLO LECTURA). Es la misma vista que "Entregas activas" de Tauri
 * (`GET /api/restaurant/delivery-assignments`): las entregas de la sucursal que aún no terminaron. Android
 * no cambia estados desde acá; quien avanza una entrega es el repartidor con Bendey Delivery.
 */
data class DeliveryBoardItem(
    val assignmentId: Int,
    val sessionId: Int,
    /** assigned | accepted | picked_up | on_the_way (las terminales no llegan al tablero). */
    val status: String,
    val assignedAt: String?,
    val driverId: Int,
    val driverName: String,
    val customerName: String,
    val deliveryAddress: String,
    val failedReason: String? = null,
)

/** Cada cuánto se refresca el tablero mientras está abierto (además de al entrar). */
const val DELIVERY_BOARD_REFRESH_MS = 60_000L

/** Mismas etiquetas que `DELIVERY_ASSIGNMENT_STATUS_LABELS` de Tauri. */
fun deliveryStatusLabel(status: String): String = when (status) {
    "assigned" -> "Asignado"
    "accepted" -> "Aceptado"
    "rejected" -> "Rechazado"
    "picked_up" -> "Recogido"
    "on_the_way" -> "En camino"
    "delivered" -> "Entregado"
    "failed" -> "Incidencia"
    else -> "En curso"
}

enum class DeliveryTone { NEUTRAL, INFO, WARNING, SUCCESS, DANGER }

fun deliveryStatusTone(status: String): DeliveryTone = when (status) {
    "assigned" -> DeliveryTone.WARNING
    "accepted", "picked_up" -> DeliveryTone.NEUTRAL
    "on_the_way" -> DeliveryTone.INFO
    "delivered" -> DeliveryTone.SUCCESS
    "failed", "rejected" -> DeliveryTone.DANGER
    else -> DeliveryTone.NEUTRAL
}

/** "hace 12 min" / "hace 1 h 05 min" desde que se asignó; "" si no hay fecha válida. */
fun deliveryElapsedText(assignedAt: String?, now: Instant): String {
    val minutes = kdsElapsedMinutes(kdsParseInstant(assignedAt), now) ?: return ""
    return if (minutes < 1) "recién asignada" else "hace " + kdsElapsedLabel(minutes)
}

/** Más de esto sin avanzar de "Asignado" merece una mirada (el repartidor no la aceptó). */
const val DELIVERY_UNACCEPTED_WARN_MIN = 10

fun deliveryNeedsAttention(item: DeliveryBoardItem, now: Instant): Boolean {
    if (item.status != "assigned") return false
    val minutes = kdsElapsedMinutes(kdsParseInstant(item.assignedAt), now) ?: return false
    return minutes >= DELIVERY_UNACCEPTED_WARN_MIN
}

fun deliveryBoardTitle(count: Int): String = when (count) {
    0 -> "Entregas activas"
    1 -> "Entregas activas · 1 en curso"
    else -> "Entregas activas · $count en curso"
}

object DeliveryBoardCopy {
    const val TITLE = "Entregas activas"
    const val EMPTY_TITLE = "No hay entregas en curso"
    const val EMPTY_DESCRIPTION = "Cuando asignes un pedido de delivery a un repartidor, aparecerá aquí."
    const val ERROR_TITLE = "No pudimos cargar las entregas"
    const val ERROR_DESCRIPTION = "Revisa tu conexión a internet e inténtalo de nuevo."
    const val RETRY = "Reintentar"
    const val READ_ONLY_HINT = "Solo para mirar: el repartidor avanza cada entrega desde Bendey Delivery."
    const val NO_NAME = "Sin nombre"
    const val NO_ADDRESS = "Sin dirección"

    fun driverLine(address: String, driver: String): String =
        "${address.ifBlank { NO_ADDRESS }} · Repartidor: ${driver.ifBlank { "sin asignar" }}"
}
