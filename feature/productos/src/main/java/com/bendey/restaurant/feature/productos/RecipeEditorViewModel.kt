package com.bendey.restaurant.feature.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.production.ProductionRepository
import com.bendey.restaurant.core.domain.production.RecipeItem
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductType
import com.bendey.restaurant.core.domain.products.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeIngredientRow(
    val key: String = UUID.randomUUID().toString(),
    val productId: Int? = null,
    val quantity: String = "1",
)

/** Borrador local de receta — NO representa un estado persistido. El formulario de producto lo
 * guarda junto con internal/production recién cuando se confirma el resto del formulario. */
data class RecipeDraft(val notes: String, val items: List<RecipeItem>)

data class RecipeEditorUiState(
    val productId: Int = 0,
    val productName: String = "",
    val notes: String = "",
    val items: List<RecipeIngredientRow> = emptyList(),
    val ingredientOptions: List<ProductItem> = emptyList(),
    val cost: Double? = null,
    val hasSavedRecipe: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

/** Estado del editor de receta de un producto elaborado. No habla con el backend para guardar:
 * junta insumo+cantidad en un RecipeDraft que el formulario de producto persiste al confirmar
 * (junto con internal/production vía ProductionRepository), para que cancelar el formulario
 * también descarte los cambios de receta. */
@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    private val productionRepository: ProductionRepository,
    private val productsRepository: ProductsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeEditorUiState())
    val uiState: StateFlow<RecipeEditorUiState> = _uiState.asStateFlow()

    fun open(productId: Int, productName: String, initialDraft: RecipeDraft? = null) {
        _uiState.value = RecipeEditorUiState(productId = productId, productName = productName, loading = true)
        viewModelScope.launch {
            val optionsResult = productsRepository.listProducts(ProductListQuery(perPage = 200))
            val options = (optionsResult as? AppResult.Success)?.data?.first
                ?.filter { it.productType != ProductType.ELABORADO && it.id != productId }
                ?: emptyList()

            if (initialDraft != null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        ingredientOptions = options,
                        notes = initialDraft.notes,
                        items = initialDraft.items.map { item ->
                            RecipeIngredientRow(productId = item.productId, quantity = formatQty(item.quantity))
                        },
                    )
                }
                return@launch
            }

            if (productId <= 0) {
                // Producto nuevo (aún sin ID) y sin borrador previo — arranca vacío, no hay receta que buscar.
                _uiState.update { it.copy(loading = false, ingredientOptions = options) }
                return@launch
            }

            when (val result = productionRepository.getRecipe(productId)) {
                is AppResult.Success -> {
                    val detail = result.data
                    _uiState.update {
                        it.copy(
                            loading = false,
                            ingredientOptions = options,
                            hasSavedRecipe = detail != null,
                            notes = detail?.recipe?.notes.orEmpty(),
                            items = detail?.items?.map { item ->
                                RecipeIngredientRow(productId = item.productId, quantity = formatQty(item.quantity))
                            } ?: emptyList(),
                        )
                    }
                    if (detail != null) refreshCost(productId)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, ingredientOptions = options, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun addIngredient() {
        _uiState.update { it.copy(items = it.items + RecipeIngredientRow()) }
    }

    fun removeIngredient(key: String) {
        _uiState.update { it.copy(items = it.items.filterNot { row -> row.key == key }) }
    }

    fun setIngredientProduct(key: String, productId: Int) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.key == key) it.copy(productId = productId) else it })
        }
    }

    fun setIngredientQuantity(key: String, quantity: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.key == key) it.copy(quantity = quantity) else it })
        }
    }

    /** Arma el RecipeDraft desde el estado actual — no persiste en el backend. */
    fun confirm(onConfirmed: (RecipeDraft) -> Unit) {
        val state = _uiState.value
        val items = state.items.mapNotNull { row ->
            val productId = row.productId ?: return@mapNotNull null
            val qty = row.quantity.replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            if (qty <= 0) return@mapNotNull null
            RecipeItem(productId = productId, quantity = qty)
        }
        if (items.isEmpty()) {
            _uiState.update { it.copy(error = "La receta debe tener al menos un ingrediente") }
            return
        }
        _uiState.update { it.copy(error = null) }
        onConfirmed(RecipeDraft(notes = state.notes, items = items))
    }

    private fun refreshCost(productId: Int) {
        viewModelScope.launch {
            val result = productionRepository.getRecipeCost(productId)
            if (result is AppResult.Success) {
                _uiState.update { it.copy(cost = result.data) }
            }
        }
    }

    private fun formatQty(value: Double): String {
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
