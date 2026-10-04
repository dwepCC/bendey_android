package com.bendey.restaurant.core.domain.onboarding

/**
 * Cuándo vale la pena volver a pedir el checklist al servidor. El estado cambia poco (se completa
 * un paso de vez en cuando), así que se conserva ~60 s y solo se relee al volver a primer plano o
 * por una acción del usuario. Sin polling.
 */
object OnboardingRefreshPolicy {
    const val TTL_MS = 60_000L

    fun shouldRefresh(
        lastLoadedAtMs: Long?,
        nowMs: Long,
        force: Boolean = false,
        ttlMs: Long = TTL_MS,
    ): Boolean = when {
        force -> true
        lastLoadedAtMs == null -> true
        nowMs < lastLoadedAtMs -> true // el reloj retrocedió: no nos fiemos de la marca
        else -> nowMs - lastLoadedAtMs >= ttlMs
    }
}
