package com.bendey.restaurant.feature.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.production.ProductionRepository
import com.bendey.restaurant.core.domain.production.RecipeDraftCost
import com.bendey.restaurant.core.domain.production.RecipeItem
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductType
import com.bendey.restaurant.core.domain.products.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val costeo: RecipeDraftCost? = null,
    val costeando: Boolean = false,
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
                            notes = detail?.recipe?.notes.orEmpty(),
                            items = detail?.items?.map { item ->
                                RecipeIngredientRow(productId = item.productId, quantity = formatQty(item.quantity))
                            } ?: emptyList(),
                        )
                    }
                    recostear()
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
        recostear()
    }

    fun setIngredientProduct(key: String, productId: Int) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.key == key) it.copy(productId = productId) else it })
        }
        recostear()
    }

    fun setIngredientQuantity(key: String, quantity: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.key == key) it.copy(quantity = quantity) else it })
        }
        recostear()
    }

    /** Arma el RecipeDraft desde el estado actual — no persiste en el backend. */
    fun confirm(onConfirmed: (RecipeDraft) -> Unit) {
        val state = _uiState.value
        // La misma lectura que usa el costeo: si el editor cuesta una lista y guarda otra, el numero
        // que se vio al armar el plato no seria el del plato guardado.
        val items = ingredientesElegidos()
        if (items.isEmpty()) {
            _uiState.update { it.copy(error = "La receta debe tener al menos un ingrediente") }
            return
        }
        _uiState.update { it.copy(error = null) }
        onConfirmed(RecipeDraft(notes = state.notes, items = items))
    }

    // EL COSTO SE RECALCULA MIENTRAS SE ARMA EL PLATO, no al guardar.
    //
    // Antes se pedia el costo de la receta ya persistida, asi que agregar un ingrediente no movia el
    // numero: habia que guardar y reabrir para ver el efecto. Se cuesta el borrador, que es lo unico
    // que quien arma la receta esta mirando.
    //
    // Y LO CUESTA EL BACKEND: el costo de un insumo es el promedio de sus compras y, solo si no tiene
    // ninguna, el precio declarado en su ficha. Repetir esa regla aca la dejaria libre de desviarse
    // del numero que el sistema usa para el margen.
    private var trabajoDeCosteo: Job? = null

    private fun recostear() {
        val ingredientes = ingredientesElegidos()
        trabajoDeCosteo?.cancel()
        if (ingredientes.isEmpty()) {
            _uiState.update { it.copy(costeo = null, costeando = false) }
            return
        }
        trabajoDeCosteo = viewModelScope.launch {
            // Un respiro antes de pedir: escribir "0.125" en la cantidad son cinco pulsaciones, y el
            // intermedio "0.1" apareceria como un parpadeo del total.
            delay(350)
            _uiState.update { it.copy(costeando = true) }
            when (val r = productionRepository.costDraft(ingredientes)) {
                is AppResult.Success -> _uiState.update { it.copy(costeo = r.data, costeando = false) }
                // Que el costeo falle no puede impedir armar la receta: se deja de mostrar el numero,
                // que es mas honesto que dejar en pantalla uno viejo.
                is AppResult.Error -> _uiState.update { it.copy(costeo = null, costeando = false) }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Las filas ya completas. Una recien agregada no tiene producto todavia y no es un ingrediente. */
    private fun ingredientesElegidos(): List<RecipeItem> = _uiState.value.items.mapNotNull { row ->
        val productId = row.productId ?: return@mapNotNull null
        val qty = row.quantity.replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
        if (qty <= 0) null else RecipeItem(productId = productId, quantity = qty)
    }

    private fun formatQty(value: Double): String {
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
