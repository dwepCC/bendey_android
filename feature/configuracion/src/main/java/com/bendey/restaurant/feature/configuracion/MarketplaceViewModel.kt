package com.bendey.restaurant.feature.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.marketplace.MarketplaceOps
import com.bendey.restaurant.core.domain.marketplace.MarketplaceRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val enabled: Boolean = false,
    val menuDigitalEnabled: Boolean = true,
    val ops: MarketplaceOps? = null,
    val pauseMinutes: Int = 30,
    val pauseReason: String = "",
    val alertMinutes: String = "5",
    val autoCancelMinutes: String = "0",
    val canManage: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
)

/** Publicacion en el marketplace (R-11) + pausa de pedidos y umbrales de pedido sin atender (R-9). */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val sessionStore: UserSessionStore,
    opsRepository: com.bendey.restaurant.core.domain.ops.OpsRepository,
) : ViewModel() {

    /** Horario de atención (lógica en [BusinessHoursPresenter], testeable sin Android). */
    private val hoursPresenter = BusinessHoursPresenter(viewModelScope, opsRepository)
    val hours: StateFlow<BusinessHoursState> = hoursPresenter.state
    fun setHoursDayOpen(day: Int, open: Boolean) = hoursPresenter.setDayOpen(day, open)
    fun addHoursSlot(day: Int) = hoursPresenter.addSlot(day)
    fun removeHoursSlot(day: Int, index: Int) = hoursPresenter.removeSlot(day, index)
    fun updateHoursSlot(day: Int, index: Int, slot: com.bendey.restaurant.core.domain.ops.SlotDraft) = hoursPresenter.updateSlot(day, index, slot)
    fun copyHoursToAll(day: Int) = hoursPresenter.copyToAll(day)
    fun saveHours() = hoursPresenter.save()
    fun loadHours() = hoursPresenter.load()

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { user ->
                val perms = user?.restaurantPermissions.orEmpty()
                hoursPresenter.setPermissions(perms)
                _uiState.update { it.copy(canManage = RestaurantPermissions.canManageRestaurantSettings(perms)) }
            }
        }
        hoursPresenter.load()
        load()
    }

    fun clearSnack() = _uiState.update { it.copy(snackMessage = null) }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val s = repository.getSettings()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(loading = false, enabled = s.data.enabled, menuDigitalEnabled = s.data.menuDigitalEnabled)
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = s.message) }
                AppResult.Loading -> Unit
            }
            (repository.getOps() as? AppResult.Success)?.let { applyOps(it.data) }
        }
    }

    private fun applyOps(o: MarketplaceOps) = _uiState.update {
        it.copy(ops = o, alertMinutes = o.unattendedAlertMinutes.toString(), autoCancelMinutes = o.autoCancelMinutes.toString())
    }

    fun setEnabled(next: Boolean) {
        val previous = _uiState.value.enabled
        _uiState.update { it.copy(enabled = next, busy = true) }
        viewModelScope.launch {
            when (val r = repository.setEnabled(next)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(busy = false, snackMessage = if (next) "Tu restaurante ya aparece en el Marketplace" else "Tu restaurante se retiro del Marketplace")
                }
                is AppResult.Error -> _uiState.update { it.copy(busy = false, enabled = previous, snackMessage = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setPauseMinutes(v: Int) = _uiState.update { it.copy(pauseMinutes = v) }
    fun setPauseReason(v: String) = _uiState.update { it.copy(pauseReason = v.take(255)) }
    fun setAlertMinutes(v: String) = _uiState.update { it.copy(alertMinutes = v.filter(Char::isDigit).take(3)) }
    fun setAutoCancelMinutes(v: String) = _uiState.update { it.copy(autoCancelMinutes = v.filter(Char::isDigit).take(3)) }

    fun pause(paused: Boolean) {
        val s = _uiState.value
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = repository.setPause(paused, if (paused) s.pauseMinutes else 0, s.pauseReason.trim())) {
                is AppResult.Success -> {
                    applyOps(r.data)
                    _uiState.update { it.copy(busy = false, snackMessage = if (paused) "Dejaste de recibir pedidos" else "Volviste a recibir pedidos") }
                }
                is AppResult.Error -> _uiState.update { it.copy(busy = false, snackMessage = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun saveThresholds() {
        val s = _uiState.value
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = repository.setThresholds(s.alertMinutes.toIntOrNull() ?: 0, s.autoCancelMinutes.toIntOrNull() ?: 0)) {
                is AppResult.Success -> {
                    applyOps(r.data)
                    _uiState.update { it.copy(busy = false, snackMessage = "Guardado") }
                }
                is AppResult.Error -> _uiState.update { it.copy(busy = false, snackMessage = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}
