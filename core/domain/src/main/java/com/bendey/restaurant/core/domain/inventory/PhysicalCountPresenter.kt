package com.bendey.restaurant.core.domain.inventory

import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Producto contable en el conteo físico (con control de stock y sin stock por presentación). */
data class CountProduct(val productId: Int, val code: String, val name: String, val unit: String)

/** Lo que devuelve el cargador de productos del conteo. */
data class CountProductsLoad(
    val products: List<CountProduct>,
    /** Productos con control de stock por presentación: no se cuentan aquí. */
    val skippedByPresentation: Int = 0,
    /** Había más productos que el tope de carga. */
    val truncated: Boolean = false,
)

data class PhysicalCountUiState(
    val branchId: Int? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val products: List<CountProduct> = emptyList(),
    val skippedByPresentation: Int = 0,
    val truncated: Boolean = false,
    val query: String = "",
    /** Cantidad tecleada por producto (clave = producto, presentación 0). */
    val counted: Map<Pair<Int, Int>, String> = emptyMap(),
    val calculating: Boolean = false,
    val preview: PhysicalCountResult? = null,
    val confirmOpen: Boolean = false,
    val note: String = "",
    val applying: Boolean = false,
    val applyResult: PhysicalCountResult? = null,
    val actionError: String? = null,
    val snack: String? = null,
) {
    val visibleProducts: List<CountProduct>
        get() {
            val t = query.trim().lowercase()
            return if (t.isEmpty()) products else products.filter { it.name.lowercase().contains(t) || it.code.lowercase().contains(t) }
        }

    val countedCount: Int get() = PhysicalCountRules.buildLines(counted).size

    val canCalculate: Boolean get() = !calculating && !applying && branchId != null && PhysicalCountRules.validate(counted) == null

    /** «Aplicar ajustes» solo con una vista previa vigente que tenga diferencias. */
    val canApply: Boolean
        get() = !applying && !calculating && preview != null && PhysicalCountRules.withDifference(preview).any { it.error == null }
}

/**
 * Conteo físico del inventario: contar, ver diferencias (vista previa, no toca nada) y aplicar ajustes.
 * Sin Android: lo usan el ViewModel y los tests.
 */
class PhysicalCountPresenter(
    private val scope: CoroutineScope,
    private val repository: InventoryCountRepository,
    private val loadProducts: suspend (branchId: Int) -> AppResult<CountProductsLoad>,
) {
    private val _state = MutableStateFlow(PhysicalCountUiState())
    val state: StateFlow<PhysicalCountUiState> = _state.asStateFlow()

    fun start(branchId: Int?) {
        if (branchId == null) {
            _state.update { it.copy(error = "No hay una sucursal activa para contar.") }
            return
        }
        if (_state.value.branchId == branchId && _state.value.products.isNotEmpty()) return
        selectBranch(branchId)
    }

    fun selectBranch(branchId: Int) {
        _state.update {
            it.copy(
                branchId = branchId, loading = true, error = null, products = emptyList(), counted = emptyMap(),
                preview = null, applyResult = null,
            )
        }
        scope.launch {
            when (val r = loadProducts(branchId)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        loading = false, products = r.data.products,
                        skippedByPresentation = r.data.skippedByPresentation, truncated = r.data.truncated,
                    )
                }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setCounted(productId: Int, text: String) = _state.update {
        // Cambiar una cantidad invalida la vista previa: hay que volver a calcular.
        it.copy(counted = it.counted + ((productId to 0) to text), preview = null, applyResult = null, actionError = null)
    }

    fun clearCounts() = _state.update { it.copy(counted = emptyMap(), preview = null, applyResult = null, actionError = null) }

    /** «Calcular diferencias» (apply = false). */
    fun calculate() {
        val s = _state.value
        val branch = s.branchId ?: return
        PhysicalCountRules.validate(s.counted)?.let { msg ->
            _state.update { it.copy(actionError = msg) }
            return
        }
        _state.update { it.copy(calculating = true, actionError = null, applyResult = null) }
        scope.launch {
            when (val r = repository.physicalCount(branch, PhysicalCountRules.buildLines(s.counted), "", apply = false)) {
                is AppResult.Success -> _state.update { it.copy(calculating = false, preview = r.data) }
                is AppResult.Error -> _state.update { it.copy(calculating = false, actionError = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openConfirm() {
        if (_state.value.canApply) _state.update { it.copy(confirmOpen = true, actionError = null) }
    }

    fun dismissConfirm() {
        if (!_state.value.applying) _state.update { it.copy(confirmOpen = false) }
    }

    fun setNote(text: String) = _state.update { it.copy(note = text.take(200)) }

    /** «Aplicar ajustes» (apply = true): genera un ajuste por cada diferencia. */
    fun apply() {
        val s = _state.value
        val branch = s.branchId ?: return
        if (s.applying || !s.canApply) return
        _state.update { it.copy(applying = true, actionError = null) }
        scope.launch {
            when (val r = repository.physicalCount(branch, PhysicalCountRules.buildLines(s.counted), s.note, apply = true)) {
                is AppResult.Success -> _state.update { st ->
                    val failedKeys = r.data.lines.filter { it.error != null }.map { it.productId to 0 }.toSet()
                    st.copy(
                        applying = false,
                        confirmOpen = false,
                        preview = null,
                        applyResult = r.data,
                        note = "",
                        // Lo aplicado se limpia; lo que falló queda para reintentar.
                        counted = st.counted.filterKeys { it in failedKeys },
                        snack = if (r.data.failedCount == 0) "Conteo aplicado: ${r.data.adjustedCount} ajustes." else null,
                    )
                }
                is AppResult.Error -> _state.update { it.copy(applying = false, actionError = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnack() = _state.update { it.copy(snack = null) }
}
