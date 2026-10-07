package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeFormResult
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeRules
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.canManageDeliverySettings
import com.bendey.restaurant.core.domain.delivery.deliveryFeeFieldText
import com.bendey.restaurant.core.domain.delivery.deliveryMoney
import com.bendey.restaurant.core.domain.delivery.parseDeliveryFeeInput
import com.bendey.restaurant.core.domain.delivery.validateDeliveryFeeForm
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de "Tarifa de delivery" en Ajustes > Operación (D2.0). */
data class DeliveryFeeSettingsState(
    /** Último valor conocido del servidor (o de la caché); null mientras no se haya podido leer. */
    val settings: DeliverySettings? = null,
    val loading: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura (no hay botón de configurar). */
    val canEdit: Boolean = false,
    val dialogOpen: Boolean = false,
    val enabled: Boolean = false,
    val amountText: String = "",
    val affectation: String = DeliveryFeeRules.AFFECTATION_TAXED,
    val saving: Boolean = false,
    /** Error de validación o del servidor, visible dentro del diálogo. */
    val error: String? = null,
) {
    /** Resumen de la tarjeta, ya listo para pintar. */
    val summary: String
        get() {
            val s = settings ?: return if (loading) "Cargando…" else DeliveryFeeCopy.OFF
            if (!s.feeEnabled || s.deliveryFee <= 0.0) return DeliveryFeeCopy.OFF
            return "${deliveryMoney(s.deliveryFee)} por pedido · ${DeliveryFeeRules.affectationLabel(s.feeIgvAffectation)}"
        }
}

/**
 * Reductor de Ajustes > Operación > Tarifa de delivery. Sin Android: lo usa [ConfiguracionViewModel] con
 * `viewModelScope` y los tests con un scope propio.
 *
 * Reglas: solo `s.m` edita; el monto va de 0 a 999.99 con 2 decimales; NO se enciende con 0; guardar manda un
 * PUT con los tres campos y deja la respuesta en la caché compartida ([DeliveryRepository.updateDeliverySettings]).
 */
class DeliveryFeeSettingsPresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryRepository,
) {
    private val _state = MutableStateFlow(DeliveryFeeSettingsState(settings = repository.peekDeliverySettings()))
    val state: StateFlow<DeliveryFeeSettingsState> = _state.asStateFlow()

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = canManageDeliverySettings(permissions)) }
    }

    fun load(forceRefresh: Boolean = false) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            when (val r = repository.getDeliverySettings(forceRefresh)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, settings = r.data) }
                // Sin red se queda lo último conocido (o "Apagada"); no se inventa nada.
                is AppResult.Error -> _state.update { it.copy(loading = false) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openDialog() {
        val s = _state.value
        if (!s.canEdit) return
        val cur = s.settings ?: DeliverySettings()
        _state.update {
            it.copy(
                dialogOpen = true,
                enabled = cur.feeEnabled,
                amountText = deliveryFeeFieldText(cur.deliveryFee),
                affectation = cur.feeIgvAffectation,
                error = null,
            )
        }
    }

    fun dismissDialog() {
        if (_state.value.saving) return
        _state.update { it.copy(dialogOpen = false, error = null) }
    }

    /** Encender con monto 0 no se permite: el interruptor se queda apagado y se explica por qué. */
    fun setEnabled(value: Boolean) {
        if (!_state.value.canEdit || _state.value.saving) return
        if (value && (parseDeliveryFeeInput(_state.value.amountText) ?: 0.0) <= 0.0) {
            _state.update { it.copy(enabled = false, error = DeliveryFeeCopy.AMOUNT_REQUIRED_TO_ENABLE) }
            return
        }
        _state.update { it.copy(enabled = value, error = null) }
    }

    /** Solo dígitos y un separador decimal, máximo 3 enteros y 2 decimales. */
    fun setAmountText(text: String) {
        if (!_state.value.canEdit) return
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        val parts = clean.split('.')
        val limited = if (parts.size <= 1) clean.take(3) else parts[0].take(3) + "." + parts.drop(1).joinToString("").take(2)
        _state.update { it.copy(amountText = limited, error = null) }
    }

    fun setAffectation(code: String) {
        if (!_state.value.canEdit) return
        _state.update { it.copy(affectation = code, error = null) }
    }

    fun save() {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        when (val form = validateDeliveryFeeForm(s.enabled, s.amountText, s.affectation)) {
            is DeliveryFeeFormResult.Invalid -> _state.update { it.copy(error = form.message) }
            is DeliveryFeeFormResult.Ok -> {
                _state.update { it.copy(saving = true, error = null) }
                scope.launch {
                    when (val r = repository.updateDeliverySettings(form.update)) {
                        is AppResult.Success -> _state.update {
                            it.copy(saving = false, dialogOpen = false, settings = r.data, error = null)
                        }
                        // El error llega traducido por el catálogo (DELIVERY_FEE_*); el diálogo sigue abierto.
                        is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                        AppResult.Loading -> _state.update { it.copy(saving = false) }
                    }
                }
            }
        }
    }
}
