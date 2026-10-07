package com.bendey.restaurant.core.domain.delivery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Caché compartida de los ajustes de delivery (D2.0): Ajustes, la hoja de asignar y el POS leen el MISMO valor
 * y no repiten la consulta. Se invalida al guardar y vence sola ([ttlMs]) por si otro dispositivo lo cambia.
 * Sin Android ni red: el reloj se inyecta para las pruebas.
 */
class DeliverySettingsCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _value = MutableStateFlow<DeliverySettings?>(null)

    /** Último valor conocido (aunque haya vencido: sirve para pintar mientras se recarga). */
    val value: StateFlow<DeliverySettings?> = _value.asStateFlow()

    @Volatile private var storedAtMs: Long = 0L

    fun put(settings: DeliverySettings) {
        storedAtMs = clock()
        _value.value = settings
    }

    /** El valor si sigue vigente, o null si no hay o venció. */
    fun fresh(): DeliverySettings? {
        val v = _value.value ?: return null
        return if (clock() - storedAtMs <= ttlMs) v else null
    }

    fun invalidate() {
        _value.value = null
        storedAtMs = 0L
    }

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
    }
}
