package com.bendey.restaurant.core.domain.products

import com.bendey.restaurant.core.domain.model.AppResult
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stock de un producto en la sucursal activa, consultado desde su tarjeta (POS y mesa). */
data class PresentationStock(val presentationId: Int, val name: String, val quantity: Double, val minStock: Double)

data class ProductStock(
    val total: Double,
    val minStock: Double,
    /** Solo con stock por presentación: cada tamaño activo. Vacío = el saldo vive en el total. */
    val presentations: List<PresentationStock>,
)

interface ProductStockRepository {
    /** `GET /api/products/:id/presentation-stock?active_only=1` (sucursal activa). */
    suspend fun stock(productId: Int): AppResult<ProductStock>
}

enum class StockLevel { OK, LOW, OUT }

object ProductStockRules {
    /** Sin stock (0 o negativo), bajo el mínimo (si hay mínimo) o bien. */
    fun level(quantity: Double, minStock: Double): StockLevel = when {
        quantity <= 0.0 -> StockLevel.OUT
        minStock > 0.0 && quantity <= minStock -> StockLevel.LOW
        else -> StockLevel.OK
    }

    /** 12.0 → «12», 1.5 → «1.5». */
    fun qty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

    /** El ícono va solo con «Mostrar stock en el POS» encendido y en productos que controlan stock. */
    fun showsIcon(showPosStock: Boolean, manageStock: Boolean): Boolean = showPosStock && manageStock
}

data class ProductStockDialogState(
    val productId: Int,
    val productName: String,
    val loading: Boolean = true,
    val stock: ProductStock? = null,
    val error: String? = null,
)

/**
 * Diálogo «Stock de X»: pide el stock EN EL MOMENTO (el catálogo no lo trae: se cachea y no cambia
 * cuando se vende) y lo guarda 30 s por producto para no repetir la consulta en toques seguidos.
 */
class ProductStockPresenter(
    private val scope: CoroutineScope,
    private val repository: ProductStockRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<ProductStockDialogState?>(null)
    val state: StateFlow<ProductStockDialogState?> = _state.asStateFlow()
    private val cache = mutableMapOf<Int, Pair<Long, ProductStock>>()
    private var job: Job? = null

    fun open(productId: Int, productName: String) {
        job?.cancel()
        cache[productId]?.takeIf { now() - it.first < CACHE_MS }?.let { (_, stock) ->
            _state.value = ProductStockDialogState(productId, productName, loading = false, stock = stock)
            return
        }
        _state.value = ProductStockDialogState(productId, productName)
        job = scope.launch {
            when (val r = repository.stock(productId)) {
                is AppResult.Success -> {
                    cache[productId] = now() to r.data
                    _state.update { s -> s?.takeIf { it.productId == productId }?.copy(loading = false, stock = r.data) }
                }
                is AppResult.Error -> _state.update { s -> s?.takeIf { it.productId == productId }?.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismiss() {
        job?.cancel()
        _state.value = null
    }

    companion object {
        const val CACHE_MS = 30_000L
    }
}
