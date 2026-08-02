package com.bendey.restaurant.core.network.error

/**
 * La suscripción del tenant no permite operar (vencida, suspendida o cuenta bloqueada).
 *
 * El backend responde HTTP 402 con el detalle, pero antes ninguna capa lo distinguía: el usuario
 * veía "Error de conexión (402)" justo en el peor momento, cuando el sistema deja de funcionar.
 */
class SubscriptionBlockedException(
    message: String,
    /** true = cuenta bloqueada (no basta con pagar, requiere soporte). */
    val blocked: Boolean,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
