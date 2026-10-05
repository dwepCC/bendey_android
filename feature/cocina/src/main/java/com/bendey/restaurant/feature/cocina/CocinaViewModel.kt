package com.bendey.restaurant.feature.cocina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.kitchen.KitchenPreferencesStore
import com.bendey.restaurant.core.domain.kitchen.KdsAction
import com.bendey.restaurant.core.domain.kitchen.KdsColumn
import com.bendey.restaurant.core.domain.kitchen.KdsCopy
import com.bendey.restaurant.core.domain.kitchen.kdsTicketTitle
import com.bendey.restaurant.core.domain.kitchen.KdsRound
import com.bendey.restaurant.core.domain.kitchen.KdsThresholds
import com.bendey.restaurant.core.domain.kitchen.applyKdsOverrides
import com.bendey.restaurant.core.domain.kitchen.kdsFilterItems
import com.bendey.restaurant.core.domain.kitchen.kdsIdsStillBehind
import com.bendey.restaurant.core.domain.kitchen.kdsIdsToAdvance
import com.bendey.restaurant.core.domain.kitchen.kdsShouldPlayEscalation
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantFeature
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.collectPreparationAreas
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.NewOrderSoundPlayer
import com.bendey.restaurant.core.realtime.UiPresence
import com.bendey.restaurant.core.realtime.recovery.RestaurantHydrators
import com.bendey.restaurant.core.realtime.store.KitchenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CocinaOrderTab(val apiValue: String?, val label: String) {
    ALL(null, "Todos"),
    DINE_IN("dine_in", "Mesas"),
    DELIVERY("delivery", "Delivery"),
    TAKEAWAY("takeaway", "Llevar"),
}

/** Cambio de ronda pendiente de enviar (ventana de "Deshacer" de 5 s). */
data class PendingAdvance(
    val token: Long,
    val orderId: Int?,
    val ids: List<Int>,
    val target: ComandaStatus,
    /** Texto de la barra de deshacer: "MESA 5 · Comanda #2 → LISTO" (mismo que Tauri). */
    val label: String,
)

data class CocinaUiState(
    val loading: Boolean = false,
    /** Items del servidor (store realtime), SIN overrides optimistas. */
    val serverItems: List<KitchenItem> = emptyList(),
    /** comandaId -> estado que ya mostramos aunque el servidor aun no lo confirme. */
    val overrides: Map<Int, ComandaStatus> = emptyMap(),
    val pending: PendingAdvance? = null,
    val orderTab: CocinaOrderTab = CocinaOrderTab.ALL,
    val areaFilter: String = "all",
    val snoozedUntilMs: Long = 0L,
    val voidItem: KitchenItem? = null,
    val voidReason: String = "",
    val voidPin: String = "",
    val voidSubmitting: Boolean = false,
    val error: String? = null,
    val canAnularComanda: Boolean = false,
    val canManageKitchenComandas: Boolean = false,
) {
    /** Lo que se pinta: servidor + capa optimista. */
    val items: List<KitchenItem> get() = applyKdsOverrides(serverItems, overrides)

    /** Areas con items, mas la guardada aunque hoy no tenga (para poder verla seleccionada). */
    val availableAreas: List<String>
        get() = (collectPreparationAreas(serverItems) + areaFilter.takeIf { it != "all" }).filterNotNull().distinct().sorted()

    /** Items ya filtrados por area y tipo de pedido. */
    val visibleItems: List<KitchenItem> get() = kdsFilterItems(items, areaFilter, orderTab.apiValue)
}

@HiltViewModel
class CocinaViewModel @Inject constructor(
    private val kitchenRepository: KitchenRepository,
    private val kitchenStore: KitchenStore,
    private val restaurantHydrators: RestaurantHydrators,
    private val sessionStore: UserSessionStore,
    private val kitchenPrefs: KitchenPreferencesStore,
    private val soundPlayer: NewOrderSoundPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CocinaUiState())
    val uiState: StateFlow<CocinaUiState> = _uiState.asStateFlow()

    /**
     * Scope propio para ENVIAR cambios: NO se cancela con el ViewModel, asi un cambio que esta en
     * su ventana de "Deshacer" se envia igual si el usuario sale de la pantalla.
     */
    private val commitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingJob: Job? = null
    private var tokenSeq = 0L

    private var lastEscalationMs: Long? = null
    private var alertedRed: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                val perms = session?.restaurantPermissions.orEmpty()
                _uiState.update {
                    it.copy(
                        canAnularComanda = RestaurantPermissions.canAnularComanda(perms),
                        canManageKitchenComandas = RestaurantPermissions.canManageKitchenComandas(perms),
                    )
                }
            }
        }
        viewModelScope.launch {
            val saved = runCatching { kitchenPrefs.areaFilter() }.getOrDefault("all")
            _uiState.update { it.copy(areaFilter = saved) }
        }
        viewModelScope.launch {
            kitchenStore.state.collect { snapshot ->
                val items = snapshot.ids.mapNotNull { snapshot.entities[it] }
                _uiState.update { it.copy(serverItems = items) }
            }
        }
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .map { session ->
                    val perms = session?.restaurantPermissions.orEmpty()
                    val et = session?.user?.employeeType
                    RestaurantPermissions.canAccessFeature(perms, RestaurantFeature.COMANDAS, et)
                }
                .distinctUntilChanged()
                .collect { canAccess ->
                    UiPresence.kitchen = canAccess
                    if (canAccess) {
                        refresh()
                    } else {
                        _uiState.update { it.copy(loading = false, serverItems = emptyList(), error = null) }
                        kitchenStore.reset()
                    }
                }
        }
    }

    override fun onCleared() {
        // La pantalla ya no esta: lo pendiente se envia de inmediato (no se pierde el toque).
        flushPending()
        super.onCleared()
    }

    /** Carga visible (pull-to-refresh / boton): muestra el indicador. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val ok = restaurantHydrators.hydrateKitchen()
            _uiState.update {
                it.copy(loading = false, error = if (!ok) "Error al cargar comandas" else null)
            }
        }
    }

    fun setOrderTab(tab: CocinaOrderTab) {
        _uiState.update { it.copy(orderTab = tab) }
    }

    fun setAreaFilter(area: String) {
        _uiState.update { it.copy(areaFilter = area) }
        viewModelScope.launch { runCatching { kitchenPrefs.saveAreaFilter(area) } }
    }

    /** Campana: con la alerta activa, silencia el atraso 5 min; silenciada, la reactiva. Los pedidos nuevos siguen sonando. */
    fun toggleBell(nowMs: Long) {
        _uiState.update {
            it.copy(snoozedUntilMs = if (nowMs < it.snoozedUntilMs) 0L else nowMs + KdsThresholds.ESCALATION_SNOOZE_MS)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // ---------------------------------------------------------------- avance de ronda (optimista)

    /**
     * Un toque por ronda. La tarjeta se mueve YA (override); el PUT se retrasa 5 s para poder
     * deshacer. Un segundo toque envia de inmediato el anterior (solo el ultimo es deshacible).
     */
    fun advanceRound(round: KdsRound, action: KdsAction) {
        if (!_uiState.value.canManageKitchenComandas) return
        val ids = kdsIdsToAdvance(round.items, action.target)
        if (ids.isEmpty()) return
        flushPending()
        val pending = PendingAdvance(
            token = ++tokenSeq,
            orderId = round.orderId,
            ids = ids,
            target = action.target,
            label = KdsCopy.undoMoved(kdsTicketTitle(round), round.orderNumber ?: 0, KdsColumn.of(action.target).label),
        )
        _uiState.update {
            it.copy(
                error = null,
                pending = pending,
                overrides = it.overrides + ids.associateWith { action.target },
            )
        }
        pendingJob = viewModelScope.launch {
            delay(KdsThresholds.UNDO_WINDOW_MS)
            commit(pending)
        }
    }

    /** "Deshacer": no se llego a enviar nada, solo se quita la capa optimista. */
    fun undo() {
        val pending = _uiState.value.pending ?: return
        pendingJob?.cancel()
        pendingJob = null
        _uiState.update {
            it.copy(pending = null, overrides = it.overrides - pending.ids.toSet())
        }
    }

    /** Envia ya lo pendiente (nuevo toque, salida de pantalla o cierre del ViewModel). */
    fun flushPending() {
        val pending = _uiState.value.pending ?: return
        pendingJob?.cancel()
        pendingJob = null
        commit(pending)
    }

    private fun commit(pending: PendingAdvance) {
        _uiState.update { if (it.pending?.token == pending.token) it.copy(pending = null) else it }
        commitScope.launch {
            // Estado ACTUAL del servidor: otra pantalla pudo adelantarse (el backend no retrocede).
            val current = _uiState.value.serverItems.associate { it.id to it.status }
            val ids = kdsIdsStillBehind(pending.ids, current, pending.target)
            var failure: String? = null
            if (ids.isNotEmpty()) {
                val result = kitchenRepository.updateOrderComandasStatus(pending.orderId, ids, pending.target)
                if (result is AppResult.Error) failure = result.message
            }
            // Refresco silencioso (sin indicador): trae la verdad del servidor y recien entonces
            // se quita la capa optimista, para que la tarjeta no "salte" hacia atras y adelante.
            runCatching { restaurantHydrators.hydrateKitchen() }
            _uiState.update {
                it.copy(
                    overrides = it.overrides - pending.ids.toSet(),
                    error = failure ?: it.error,
                )
            }
        }
    }

    // ---------------------------------------------------------------- escalada sonora de atrasos

    /** La pantalla informa cada tick (30 s) que rondas estan en rojo; aqui se decide si suena. */
    fun onRedRounds(redKeys: Set<String>, nowMs: Long) {
        val state = _uiState.value
        val hasNew = redKeys.any { it !in alertedRed }
        alertedRed = redKeys
        if (kdsShouldPlayEscalation(hasNew, redKeys.isNotEmpty(), lastEscalationMs, state.snoozedUntilMs, nowMs)) {
            lastEscalationMs = nowMs
            soundPlayer.play()
        }
    }

    // ---------------------------------------------------------------- anular (con PIN)

    fun openVoidItem(item: KitchenItem) {
        if (!_uiState.value.canAnularComanda) return
        _uiState.update {
            it.copy(voidItem = item, voidReason = "", voidPin = "", error = null)
        }
    }

    fun dismissVoidDialog() {
        if (_uiState.value.voidSubmitting) return
        _uiState.update { it.copy(voidItem = null, voidReason = "", voidPin = "") }
    }

    fun setVoidReason(reason: String) {
        _uiState.update { it.copy(voidReason = reason) }
    }

    fun setVoidPin(pin: String) {
        _uiState.update { it.copy(voidPin = pin.filter { it.isDigit() }.take(6)) }
    }

    fun confirmVoid() {
        val state = _uiState.value
        val item = state.voidItem ?: return
        val reason = state.voidReason.trim()
        val pin = state.voidPin.trim()
        if (reason.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(error = "Indica el motivo y el PIN") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(voidSubmitting = true, error = null) }
            when (val result = kitchenRepository.cancelComanda(item.id, reason, pin)) {
                is AppResult.Success -> {
                    refresh()
                    _uiState.update {
                        it.copy(
                            voidSubmitting = false,
                            voidItem = null,
                            voidReason = "",
                            voidPin = "",
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voidSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}

fun orderTypeLabel(type: String?): String = when (type) {
    "dine_in" -> "Mesa"
    "delivery" -> "Delivery"
    "takeaway" -> "Llevar"
    "quick_sale" -> "Directa"
    else -> type ?: "Pedido"
}
