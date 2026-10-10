package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryRulesRules
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

/** Estado de «Pedido mínimo y tiempo de entrega» (D3) en Ajustes > Operación. */
data class DeliveryRulesState(
    val settings: DeliverySettings? = null,
    val loading: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura. */
    val canEdit: Boolean = false,
    val saving: Boolean = false,
    val minOrderText: String = "0",
    val etaText: String = "0",
    val error: String? = null,
    val savedNotice: Boolean = false,
) {
    val summary: String
        get() {
            val s = settings ?: return if (loading) "Cargando…" else "Sin reglas"
            val min = if (s.minOrderAmount > 0) "Pedido mínimo S/ ${DeliveryRulesRules.formatAmount(s.minOrderAmount)}" else "Sin pedido mínimo"
            val eta = if (s.etaMinutes > 0) "entrega prometida en ${s.etaMinutes} min" else "sin hora prometida"
            return "$min · $eta"
        }

    /** Hay algo distinto de lo guardado en los campos. */
    val dirty: Boolean
        get() {
            val s = settings ?: return false
            return DeliveryRulesRules.parseMinOrder(minOrderText) != s.minOrderAmount || DeliveryRulesRules.parseEta(etaText) != s.etaMinutes
        }
}

/**
 * Reductor de Ajustes > Operación > Pedido mínimo y tiempo de entrega (D3). Sin Android.
 * Solo `s.m` guarda; valida local (0 a 9999.99 con 2 decimales; 0 a 240 enteros) y manda un PUT PARCIAL con lo
 * que cambió. Los errores del servidor (DELIVERY_*_INVALID, DELIVERY_RULES_UNAVAILABLE) llegan ya traducidos.
 */
class DeliveryRulesPresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryRepository,
) {
    private val _state = MutableStateFlow(initial(repository.peekDeliverySettings()))
    val state: StateFlow<DeliveryRulesState> = _state.asStateFlow()

    private fun initial(s: DeliverySettings?) = DeliveryRulesState(
        settings = s,
        minOrderText = DeliveryRulesRules.formatAmount(s?.minOrderAmount ?: 0.0),
        etaText = (s?.etaMinutes ?: 0).toString(),
    )

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = canManageDeliverySettings(permissions)) }
    }

    fun load(forceRefresh: Boolean = false) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            when (val r = repository.getDeliverySettings(forceRefresh)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        settings = r.data,
                        minOrderText = DeliveryRulesRules.formatAmount(r.data.minOrderAmount),
                        etaText = r.data.etaMinutes.toString(),
                    )
                }
                is AppResult.Error -> _state.update { it.copy(loading = false) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setMinOrderText(text: String) = _state.update {
        it.copy(minOrderText = text.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7), error = null, savedNotice = false)
    }

    fun setEtaText(text: String) = _state.update { it.copy(etaText = text.filter(Char::isDigit).take(3), error = null, savedNotice = false) }

    fun save() {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        val min = DeliveryRulesRules.parseMinOrder(s.minOrderText)
        if (min == null) {
            _state.update { it.copy(error = "El pedido mínimo debe estar entre S/ 0 y S/ ${DeliveryRulesRules.formatAmount(DeliveryRulesRules.MAX_MIN_ORDER)}, con hasta 2 decimales.") }
            return
        }
        val eta = DeliveryRulesRules.parseEta(s.etaText)
        if (eta == null) {
            _state.update { it.copy(error = "El tiempo de entrega debe ser un número entero entre 0 y ${DeliveryRulesRules.MAX_ETA} minutos.") }
            return
        }
        val cur = s.settings
        val update = DeliverySettingsUpdate(
            minOrderAmount = min.takeIf { it != cur?.minOrderAmount },
            etaMinutes = eta.takeIf { it != cur?.etaMinutes },
        )
        if (update.minOrderAmount == null && update.etaMinutes == null) return
        _state.update { it.copy(saving = true, error = null, savedNotice = false) }
        scope.launch {
            when (val r = repository.updateDeliverySettings(update)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        settings = r.data,
                        minOrderText = DeliveryRulesRules.formatAmount(r.data.minOrderAmount),
                        etaText = r.data.etaMinutes.toString(),
                        savedNotice = true,
                    )
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
