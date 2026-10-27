package com.bendey.restaurant.core.domain.time

/**
 * Nombre anterior de [TenantDateTime] (incremento I4c del paquete de país). Se deja como alias para
 * no romper ramas en curso que aún lo usen; el código nuevo usa [TenantDateTime].
 */
@Deprecated(
    "Usa TenantDateTime: la zona ya no es siempre Lima, es la del tenant.",
    ReplaceWith("TenantDateTime", "com.bendey.restaurant.core.domain.time.TenantDateTime"),
)
typealias PeruDateTime = TenantDateTime
