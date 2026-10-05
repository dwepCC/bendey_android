package com.bendey.restaurant.core.domain.cache

/**
 * Politica PURA de invalidacion de la cache de cobro (series, contactos, metodos de pago) - R10.5.
 * Equivalente de `CatalogVersionWatcher` de Tauri (App.tsx): sin esto un metodo de pago creado en otro
 * equipo no aparecia hasta reiniciar sesion.
 */
object CheckoutCachePolicy {
    /** Cada cuanto se pregunta la version del catalogo con la app en pantalla (igual que Tauri). */
    const val CATALOG_VERSION_POLL_MS = 2 * 60_000L

    /** Tras este tiempo en segundo plano, al volver se descarta la cache de cobro. */
    const val FOREGROUND_MAX_AGE_MS = 10 * 60_000L

    /** La version cambio respecto a la ultima conocida (la primera lectura solo fija la base). */
    fun catalogVersionChanged(lastKnown: String?, current: String): Boolean =
        lastKnown != null && lastKnown != current

    /** Al volver a primer plano: invalidar si estuvo fuera mas de [FOREGROUND_MAX_AGE_MS]. */
    fun shouldInvalidateOnForeground(backgroundedAtMs: Long?, nowMs: Long): Boolean =
        backgroundedAtMs != null && nowMs - backgroundedAtMs >= FOREGROUND_MAX_AGE_MS
}
