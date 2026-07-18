package com.bendey.restaurant.core.network.error

/** 403 con {"error": "...", "module": "billing"} — el backend bloqueó una acción por plan. */
class ModuleLockedException(
    message: String,
    val moduleKey: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
