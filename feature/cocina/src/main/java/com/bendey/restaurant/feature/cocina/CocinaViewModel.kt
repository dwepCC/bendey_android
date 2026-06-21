package com.bendey.restaurant.feature.cocina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.collectPreparationAreas
import com.bendey.restaurant.core.domain.restaurant.collectTableNames
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.normalizePreparationAreaKey
import com.bendey.restaurant.core.domain.permission.RestaurantFeature
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CocinaViewMode(val label: String) {
    ITEMS("Por ítem"),
    ORDERS("Por pedido"),
}

enum class CocinaOrderTab(val apiValue: String?, val label: String) {
    ALL(null, "Todos"),
    DINE_IN("dine_in", "Mesas"),
    DELIVERY("delivery", "Delivery"),
    TAKEAWAY("takeaway", "Llevar"),
}

data class KitchenOrderGroup(
    val key: String,
    val title: String,
    val subtitle: String?,
    val orderType: String?,
    val items: List<KitchenItem>,
)

data class CocinaUiState(
    val loading: Boolean = false,
    val updatingId: Int? = null,
    val items: List<KitchenItem> = emptyList(),
    val viewMode: CocinaViewMode = CocinaViewMode.ITEMS,
    val orderTab: CocinaOrderTab = CocinaOrderTab.ALL,
    val areaFilter: String = "all",
    val tableFilter: String = "all",
    val voidItem: KitchenItem? = null,
    val voidReason: String = "",
    val voidPin: String = "",
    val voidSubmitting: Boolean = false,
    val error: String? = null,
    val canAnularComanda: Boolean = false,
) {
    val availableAreas: List<String> get() = collectPreparationAreas(items)
    val availableTables: List<String> get() = collectTableNames(items)

    fun count(status: ComandaStatus): Int = baseItems.count { it.status == status }

    private val baseItems: List<KitchenItem>
        get() = items.let { list ->
            list.filter { item ->
                val areaOk = areaFilter == "all" ||
                    normalizePreparationAreaKey(item.preparationArea) == areaFilter
                val tableOk = tableFilter == "all" || item.tableName == tableFilter
                areaOk && tableOk
            }
        }

    fun itemsFor(status: ComandaStatus): List<KitchenItem> =
        baseItems.filter { it.status == status }

    fun filteredItems(status: ComandaStatus): List<KitchenItem> {
        val byStatus = itemsFor(status)
        val tab = orderTab
        if (tab == CocinaOrderTab.ALL) return byStatus
        return byStatus.filter { it.orderType == tab.apiValue }
    }

    fun orderGroups(status: ComandaStatus): List<KitchenOrderGroup> =
        groupKitchenItems(filteredItems(status))
}

@HiltViewModel
class CocinaViewModel @Inject constructor(
    private val kitchenRepository: KitchenRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CocinaUiState())
    val uiState: StateFlow<CocinaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                val perms = session?.restaurantPermissions.orEmpty()
                _uiState.update {
                    it.copy(canAnularComanda = RestaurantPermissions.canAnularComanda(perms))
                }
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
                    if (canAccess) {
                        refresh()
                    } else {
                        _uiState.update { it.copy(loading = false, items = emptyList(), error = null) }
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = kitchenRepository.loadKitchen()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(loading = false, items = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setViewMode(mode: CocinaViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setOrderTab(tab: CocinaOrderTab) {
        _uiState.update { it.copy(orderTab = tab) }
    }

    fun setAreaFilter(area: String) {
        _uiState.update { it.copy(areaFilter = area) }
    }

    fun setTableFilter(table: String) {
        _uiState.update { it.copy(tableFilter = table) }
    }

    fun advanceItem(item: KitchenItem) {
        val next = ComandaStatus.next(item.status.backendValue) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(updatingId = item.id, error = null) }
            when (val result = kitchenRepository.updateComandaStatus(item.id, next)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(updatingId = null) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(updatingId = null, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun markRoundReady(items: List<KitchenItem>) {
        val pending = items.filter {
            it.status == ComandaStatus.PENDIENTE || it.status == ComandaStatus.PREPARACION
        }
        if (pending.isEmpty()) return
        val unique = pending.distinctBy { it.id }
        viewModelScope.launch {
            _uiState.update { it.copy(updatingId = -1, error = null) }
            var failed = false
            for (item in unique) {
                when (kitchenRepository.updateComandaStatus(item.id, ComandaStatus.LISTA)) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> failed = true
                    AppResult.Loading -> Unit
                }
            }
            _uiState.update { it.copy(updatingId = null) }
            if (failed) {
                _uiState.update { it.copy(error = "No se pudo marcar la ronda como lista") }
            }
            refresh()
        }
    }

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
            _uiState.update { it.copy(error = "Indique motivo y PIN") }
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

fun groupKitchenItems(items: List<KitchenItem>): List<KitchenOrderGroup> {
    if (items.isEmpty()) return emptyList()
    return items
        .groupBy { item ->
            item.orderCode?.takeIf { it.isNotBlank() }
                ?: item.tableName?.takeIf { it.isNotBlank() }?.let { "mesa-$it" }
                ?: "item-${item.id}"
        }
        .map { (key, groupItems) ->
            val first = groupItems.first()
            KitchenOrderGroup(
                key = key,
                title = first.tableName ?: first.orderCode ?: first.customerName ?: "Pedido",
                subtitle = listOfNotNull(
                    first.orderCode?.takeIf { first.tableName != null },
                    first.waiterName,
                    first.floorName,
                ).joinToString(" · ").ifBlank { null },
                orderType = first.orderType,
                items = groupItems,
            )
        }
        .sortedBy { it.title }
}

fun orderTypeLabel(type: String?): String = when (type) {
    "dine_in" -> "Mesa"
    "delivery" -> "Delivery"
    "takeaway" -> "Llevar"
    "quick_sale" -> "Directa"
    else -> type ?: "Pedido"
}
