package com.bendey.restaurant.core.domain.waiter

import com.bendey.restaurant.core.domain.model.PinStation

/**
 * Estacion recordada por DISPOSITIVO y por restaurante (R8). La estacion es del equipo; el PIN sigue siendo
 * de la persona: nunca se recuerda ni se omite. Solo se guarda tras un PIN valido.
 */
interface StationPreferences {
    /** Estacion recordada de este restaurante en este dispositivo, o null si no hay (o no es saltable). */
    suspend fun remembered(): PinStation?

    suspend fun remember(station: PinStation)

    /** "Cambiar estacion": olvida la estacion para volver a elegir. */
    suspend fun clear()
}
