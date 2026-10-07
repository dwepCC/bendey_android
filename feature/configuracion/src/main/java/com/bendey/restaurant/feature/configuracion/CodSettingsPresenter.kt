package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryCopy
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.canManageDeliverySettings
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de "Pago contra entrega" en Ajustes > Operación (D2b). */
data class CodSettingsState(
    /** Último valor conocido del servidor (o de la caché); null mientras no se haya podido leer. */
    val settings: DeliverySettings? = null,
    val loading: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura (el interruptor no responde). */
    val canEdit: Boolean = false,
    /** Confirmación al ENCENDER (apagar no pide confirmación). */
    val confirmOpen: Boolean = false,
    val saving: Boolean = false,
    /** Error del servidor (p. ej. 503 si la función aún no está lista), traducido por el catálogo. */
    val error: String? = null,
) {
    val enabled: Boolean get() = settings?.codEnabled == true

    /** Resumen corto, ya listo para pintar. */
    val summary: String
        get() = when {
            settings == null && loading -> "Cargando…"
            enabled -> DeliveryCopy.text("cod.on")
            else -> DeliveryCopy.text("cod.off")
        }
}

/**
 * Reductor de Ajustes > Operación > Pago contra entrega. Sin Android: lo usa [ConfiguracionViewModel] con
 * `viewModelScope` y los tests con un scope propio.
 *
 * Reglas: solo `s.m` cambia el interruptor; encender pide confirmación; guardar manda un PUT PARCIAL con solo
 * `cod_enabled` (no pisa la tarifa) y deja la respuesta en la caché compartida; si el servidor lo rechaza (503),
 * se muestra el mensaje del catálogo y el interruptor queda apagado.
 */
class CodSettingsPresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryRepository,
) {
    private val _state = MutableStateFlow(CodSettingsState(settings = repository.peekDeliverySettings()))
    val state: StateFlow<CodSettingsState> = _state.asStateFlow()

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = canManageDeliverySettings(permissions)) }
    }

    fun load(forceRefresh: Boolean = false) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            when (val r = repository.getDeliverySettings(forceRefresh)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, settings = r.data) }
                // Sin red se queda lo último conocido (o "Apagado"); no se inventa nada.
                is AppResult.Error -> _state.update { it.copy(loading = false) }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Toca el interruptor: apagar guarda ya; encender abre la confirmación. */
    fun toggle(value: Boolean) {
        val s = _state.value
        if (!s.canEdit || s.saving || value == s.enabled) return
        if (value) {
            _state.update { it.copy(confirmOpen = true, error = null) }
        } else {
            save(false)
        }
    }

    fun dismissConfirm() = _state.update { it.copy(confirmOpen = false) }

    fun confirmEnable() {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        _state.update { it.copy(confirmOpen = false) }
        save(true)
    }

    private fun save(enabled: Boolean) {
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            when (val r = repository.updateDeliverySettings(DeliverySettingsUpdate(codEnabled = enabled))) {
                is AppResult.Success -> _state.update { it.copy(saving = false, settings = r.data, error = null) }
                // El error llega traducido por el catálogo; `settings` no cambia, así que el interruptor sigue apagado.
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
