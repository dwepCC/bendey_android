package com.bendey.restaurant.feature.productos.reportes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.inventory.CountProduct
import com.bendey.restaurant.core.domain.inventory.CountProductsLoad
import com.bendey.restaurant.core.domain.inventory.InventoryCountRepository
import com.bendey.restaurant.core.domain.inventory.InventoryValuation
import com.bendey.restaurant.core.domain.inventory.PhysicalCountPresenter
import com.bendey.restaurant.core.domain.inventory.PhysicalCountRules
import com.bendey.restaurant.core.domain.inventory.PhysicalCountUiState
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ValorizacionUiState(
    val branches: List<BranchItem> = emptyList(),
    val branchId: Int? = null,
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val data: InventoryValuation? = null,
    val allowsExport: Boolean = false,
    val exportBusy: String? = null,
    val snack: String? = null,
) {
    val rows get() = PhysicalCountRules.filter(data?.rows.orEmpty(), query)
}

/** Reporte «Valorización de inventario» (stock × costo promedio) junto al Kardex. */
@HiltViewModel
class ValorizacionViewModel @Inject constructor(
    private val repository: InventoryCountRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionStore: UserSessionStore,
    private val fileShareService: BendeyFileShareService,
) : ViewModel() {
    private val _state = MutableStateFlow(ValorizacionUiState())
    val state: StateFlow<ValorizacionUiState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val session = sessionStore.userSessionFlow.first()
            _state.update { it.copy(allowsExport = session?.allowsReportExport == true, branchId = session?.activeBranch?.id) }
            (settingsRepository.listBranches() as? AppResult.Success)?.let { r ->
                _state.update { it.copy(branches = r.data.filter { b -> b.active }) }
            }
            load()
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setBranch(id: Int) {
        _state.update { it.copy(branchId = id) }
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.valuation(_state.value.branchId, "")) {
                is AppResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun export(context: Context, format: String) {
        val s = _state.value
        val data = s.data ?: return
        if (s.exportBusy != null) return
        val rows = s.rows
        if (rows.isEmpty()) {
            _state.update { it.copy(snack = "No hay datos para exportar") }
            return
        }
        _state.update { it.copy(exportBusy = format) }
        viewModelScope.launch {
            val result = if (format == "pdf") {
                exportValuationPdf(context, fileShareService, rows, data.totalValue)
            } else {
                exportValuationCsv(context, fileShareService, rows, data.totalValue)
            }
            _state.update {
                it.copy(exportBusy = null, snack = (result as? ExportShareResult.Failure)?.userMessage)
            }
        }
    }

    fun consumeSnack() = _state.update { it.copy(snack = null) }
}

/** Estado de la pestaña «Conteo físico» (el de [PhysicalCountPresenter] + sucursales y permiso). */
data class ConteoUiState(
    val branches: List<BranchItem> = emptyList(),
    val canCount: Boolean = true,
)

@HiltViewModel
class ConteoViewModel @Inject constructor(
    repository: InventoryCountRepository,
    private val productsRepository: ProductsRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {
    private val presenter = PhysicalCountPresenter(viewModelScope, repository) { branchId -> loadCountProducts(branchId) }
    val state: StateFlow<PhysicalCountUiState> = presenter.state

    private val _meta = MutableStateFlow(ConteoUiState())
    val meta: StateFlow<ConteoUiState> = _meta.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val session = sessionStore.userSessionFlow.first()
            // Mismo permiso que los ajustes de stock (catálogo: g.p) o administración (s.m).
            val perms = session?.restaurantPermissions.orEmpty()
            val allowed = RestaurantPermissions.hasPermission(perms, RestaurantPermissions.PERM_PRODUCTOS) ||
                RestaurantPermissions.isRestaurantAdmin(perms)
            _meta.update { it.copy(canCount = allowed) }
            if (!allowed) return@launch
            (settingsRepository.listBranches() as? AppResult.Success)?.let { r ->
                _meta.update { it.copy(branches = r.data.filter { b -> b.active }) }
            }
            presenter.start(session?.activeBranch?.id ?: _meta.value.branches.firstOrNull()?.id)
        }
    }

    private suspend fun loadCountProducts(branchId: Int): AppResult<CountProductsLoad> {
        val out = mutableListOf<CountProduct>()
        var skipped = 0
        var truncated = false
        var page = 1
        while (true) {
            when (val r = productsRepository.listProducts(ProductListQuery(branchId = branchId, page = page, perPage = PAGE))) {
                is AppResult.Error -> return r
                AppResult.Loading -> return AppResult.Loading
                is AppResult.Success -> {
                    val (items, total) = r.data
                    items.filter { it.active && it.manageStock }.forEach {
                        if (it.stockByPresentation) skipped++ else out += CountProduct(it.id, it.code, it.name, it.unit)
                    }
                    if (items.isEmpty() || page * PAGE >= total) break
                    if (page >= MAX_PAGES) {
                        truncated = true
                        break
                    }
                    page++
                }
            }
        }
        return AppResult.Success(CountProductsLoad(out.sortedBy { it.name.lowercase() }, skipped, truncated))
    }

    fun selectBranch(id: Int) = presenter.selectBranch(id)
    fun setQuery(q: String) = presenter.setQuery(q)
    fun setCounted(productId: Int, text: String) = presenter.setCounted(productId, text)
    fun clearCounts() = presenter.clearCounts()
    fun calculate() = presenter.calculate()
    fun openConfirm() = presenter.openConfirm()
    fun dismissConfirm() = presenter.dismissConfirm()
    fun setNote(t: String) = presenter.setNote(t)
    fun apply() = presenter.apply()
    fun consumeSnack() = presenter.consumeSnack()

    private companion object {
        const val PAGE = 100
        const val MAX_PAGES = 10
    }
}
