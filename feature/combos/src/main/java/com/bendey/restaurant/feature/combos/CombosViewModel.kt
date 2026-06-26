package com.bendey.restaurant.feature.combos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.ComboEditorTab
import com.bendey.restaurant.core.domain.catalog.ComboFixedItem
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboItem
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ComboSlotOption
import com.bendey.restaurant.core.domain.catalog.CombosRepository
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.catalog.buildComboSaveSlots
import com.bendey.restaurant.core.domain.catalog.usesFixed
import com.bendey.restaurant.core.domain.catalog.usesSlots
import com.bendey.restaurant.core.domain.catalog.validateComboSaveSlotsType
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CombosUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val combos: List<ComboItem> = emptyList(),
    val formOpen: Boolean = false,
    val editingId: Int? = null,
    val form: ComboFormInput = ComboFormInput(),
    val editorTab: ComboEditorTab = ComboEditorTab.GENERAL,
    val branches: List<BranchItem> = emptyList(),
    val productSearchQuery: String = "",
    val productSearchResults: List<ProductItem> = emptyList(),
    val productSearchLoading: Boolean = false,
    val productLabels: Map<Int, String> = emptyMap(),
    val activePicker: ComboProductPickerTarget? = null,
    val deleteId: Int? = null,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CombosViewModel @Inject constructor(
    private val repository: CombosRepository,
    private val productsRepository: ProductsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CombosUiState())
    val uiState: StateFlow<CombosUiState> = _uiState.asStateFlow()

    private val productSearchFlow = MutableStateFlow("")
    private var productSearchJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            productSearchFlow
                .debounce(300)
                .distinctUntilChanged()
                .filter { _uiState.value.formOpen && _uiState.value.activePicker != null }
                .collect { query -> searchProducts(query) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.listCombos()) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, combos = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreate() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val branches = loadBranches()
            _uiState.update {
                it.copy(
                    actionLoading = false,
                    formOpen = true,
                    editingId = null,
                    editorTab = ComboEditorTab.GENERAL,
                    form = ComboFormInput(fixedItems = listOf(ComboFixedItem(productId = 0))),
                    branches = branches,
                    productSearchQuery = "",
                    productSearchResults = emptyList(),
                    productLabels = emptyMap(),
                )
            }
        }
    }

    fun openEdit(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val branches = loadBranches()
            when (val result = repository.getCombo(id)) {
                is AppResult.Success -> {
                    val form = enrichBranchNames(result.data, branches)
                    val productIds = collectProductIds(form)
                    val labels = resolveProductLabels(productIds)
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            formOpen = true,
                            editingId = id,
                            editorTab = ComboEditorTab.GENERAL,
                            form = applyProductLabels(form, labels),
                            branches = branches,
                            productLabels = labels,
                            productSearchQuery = "",
                            productSearchResults = emptyList(),
                        )
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissForm() {
        productSearchJob?.cancel()
        _uiState.update {
            it.copy(
                formOpen = false,
                editingId = null,
                productSearchQuery = "",
                productSearchResults = emptyList(),
            )
        }
    }

    fun setEditorTab(tab: ComboEditorTab) {
        _uiState.update { it.copy(editorTab = tab) }
    }

    fun updateForm(transform: (ComboFormInput) -> ComboFormInput) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun setProductSearchQuery(query: String) {
        _uiState.update { it.copy(productSearchQuery = query) }
        productSearchFlow.value = query
    }

    fun openProductPicker(target: ComboProductPickerTarget) {
        _uiState.update {
            it.copy(
                activePicker = target,
                productSearchQuery = "",
                productSearchResults = emptyList(),
                productSearchLoading = true,
            )
        }
        productSearchFlow.value = ""
        viewModelScope.launch { searchProducts("") }
    }

    fun closeProductPicker() {
        _uiState.update { it.copy(activePicker = null, productSearchQuery = "", productSearchResults = emptyList()) }
    }

    fun selectProduct(target: ComboProductPickerTarget, product: ProductItem) {
        when (target.kind) {
            ComboProductPickerTarget.Kind.FIXED -> selectProductForFixed(target.fixedIndex, product)
            ComboProductPickerTarget.Kind.SLOT_OPTION -> selectProductForSlotOption(target.slotIndex, target.optionIndex, product)
        }
        closeProductPicker()
    }

    fun selectProductForFixed(index: Int, product: ProductItem) {
        _uiState.update { state ->
            val labels = state.productLabels + (product.id to product.name)
            state.copy(
                productLabels = labels,
                productSearchQuery = "",
                productSearchResults = emptyList(),
                form = state.form.copy(
                    fixedItems = state.form.fixedItems.mapIndexed { i, item ->
                        if (i == index) item.copy(productId = product.id, productName = product.name) else item
                    },
                ),
            )
        }
    }

    fun selectProductForSlotOption(slotIndex: Int, optionIndex: Int, product: ProductItem) {
        _uiState.update { state ->
            val labels = state.productLabels + (product.id to product.name)
            state.copy(
                productLabels = labels,
                productSearchQuery = "",
                productSearchResults = emptyList(),
                form = state.form.copy(
                    slots = state.form.slots.mapIndexed { sIdx, slot ->
                        if (sIdx != slotIndex) slot
                        else slot.copy(
                            options = slot.options.mapIndexed { oIdx, option ->
                                if (oIdx != optionIndex) option
                                else option.copy(productId = product.id, productName = product.name)
                            },
                        )
                    },
                ),
            )
        }
    }

    fun addBranchRow() {
        val state = _uiState.value
        val used = state.form.branchSettings.map { it.branchId }.toSet()
        val next = state.branches.firstOrNull { it.id !in used } ?: run {
            _uiState.update { it.copy(error = "Todas las sucursales ya están configuradas") }
            return
        }
        updateForm { form ->
            form.copy(
                branchSettings = form.branchSettings + com.bendey.restaurant.core.domain.catalog.ComboBranchSetting(
                    branchId = next.id,
                    branchName = next.name,
                    active = true,
                ),
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val form = state.form
        if (form.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }

        val fixedPayload = form.fixedItems.filter { it.productId > 0 }
        val slotsPayload = buildComboSaveSlots(form.comboType, form.slots)
            .filter { it.name.trim().isNotEmpty() }
            .map { slot ->
                slot.copy(options = slot.options.filter { it.productId > 0 })
            }

        validateComboSaveSlotsType(form.comboType, slotsPayload)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }

        val usesFixed = form.usesFixed()
        val usesSlots = form.comboType.usesSlots()

        if (usesFixed && fixedPayload.isEmpty() && !usesSlots) {
            _uiState.update { it.copy(error = "Agrega al menos un producto componente") }
            return
        }
        if (usesSlots && slotsPayload.isEmpty()) {
            _uiState.update { it.copy(error = "Agrega al menos un slot con opciones") }
            return
        }
        if (usesSlots) {
            for (slot in slotsPayload) {
                if (slot.options.isEmpty()) {
                    _uiState.update { it.copy(error = "El slot \"${slot.name}\" necesita al menos una opción") }
                    return
                }
                if (slot.minPick > slot.maxPick) {
                    _uiState.update { it.copy(error = "En \"${slot.name}\": mínimo no puede superar máximo") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val payload = form.copy(
                fixedItems = if (usesFixed) fixedPayload else emptyList(),
                slots = slotsPayload,
            )
            val result = if (state.editingId == null) {
                repository.createCombo(payload)
            } else {
                repository.updateCombo(state.editingId, payload)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, formOpen = false, editingId = null) }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDelete(id: Int) { _uiState.update { it.copy(deleteId = id) } }
    fun dismissDelete() { _uiState.update { it.copy(deleteId = null) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun confirmDelete() {
        val id = _uiState.value.deleteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleteId = null) }
            when (val result = repository.deleteCombo(id)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadBranches(): List<BranchItem> =
        when (val result = settingsRepository.listBranches()) {
            is AppResult.Success -> result.data
            else -> emptyList()
        }

    private fun enrichBranchNames(form: ComboFormInput, branches: List<BranchItem>): ComboFormInput {
        val branchMap = branches.associateBy { it.id }
        return form.copy(
            branchSettings = form.branchSettings.map { setting ->
                setting.copy(branchName = branchMap[setting.branchId]?.name)
            },
        )
    }

    private fun collectProductIds(form: ComboFormInput): List<Int> =
        form.fixedItems.map { it.productId } +
            form.slots.flatMap { slot -> slot.options.map { it.productId } }

    private suspend fun resolveProductLabels(ids: List<Int>): Map<Int, String> {
        val unique = ids.filter { it > 0 }.distinct()
        if (unique.isEmpty()) return emptyMap()
        val labels = mutableMapOf<Int, String>()
        unique.forEach { id ->
            when (val result = productsRepository.getProduct(id)) {
                is AppResult.Success -> labels[id] = result.data.name
                else -> Unit
            }
        }
        return labels
    }

    private fun applyProductLabels(form: ComboFormInput, labels: Map<Int, String>): ComboFormInput =
        form.copy(
            fixedItems = form.fixedItems.map { item ->
                item.copy(productName = labels[item.productId] ?: item.productName)
            },
            slots = form.slots.map { slot ->
                slot.copy(
                    options = slot.options.map { option ->
                        option.copy(productName = labels[option.productId] ?: option.productName)
                    },
                )
            },
        )

    private suspend fun searchProducts(query: String) {
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(productSearchLoading = true) }
            when (val result = productsRepository.searchForComboEditor(query)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(productSearchLoading = false, productSearchResults = result.data.first)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(productSearchLoading = false, productSearchResults = emptyList())
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
