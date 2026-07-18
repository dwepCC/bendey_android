package com.bendey.restaurant.feature.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.BulkImportProgress
import com.bendey.restaurant.core.domain.catalog.BulkImportValidationResult
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.catalog.ProductImportRepository
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.inventory.InventoryRepository
import com.bendey.restaurant.core.domain.inventory.InventoryAdjustmentInput
import com.bendey.restaurant.core.domain.digitalmenu.DigitalMenuRepository
import com.bendey.restaurant.core.domain.digitalmenu.isMenuChannelEnabled
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.CategoryItem
import com.bendey.restaurant.core.domain.products.IgvAffectation
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.catalog.PreparationAreasRepository
import com.bendey.restaurant.core.domain.products.ProductFormInput
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.production.ProductionRepository
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductosTab
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.products.generateProductCode
import com.bendey.restaurant.core.domain.products.toFormInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryForm(
    val name: String = "",
    val description: String = "",
)

data class ProductosUiState(
    val tab: ProductosTab = ProductosTab.PRODUCTOS,
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val products: List<ProductItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val perPage: Int = 25,
    val searchQuery: String = "",
    val categoryFilterId: Int? = null,
    val areaFilterId: Int? = null,
    val productTypeFilter: String = "non_insumo",
    val showInactive: Boolean = false,
    val preparationAreas: List<PreparationAreaItem> = emptyList(),
    val branchFilterId: Int? = null,
    val branches: List<BranchItem> = emptyList(),
    val stockByProductId: Map<Int, Double> = emptyMap(),
    val categories: List<CategoryItem> = emptyList(),
    val modifierGroups: List<ModifierGroup> = emptyList(),
    val productFormOpen: Boolean = false,
    val editingProductId: Int? = null,
    val productForm: ProductFormInput = ProductFormInput(),
    val recipeDraft: RecipeDraft? = null,
    val showMoreOptions: Boolean = false,
    val presentationsOpen: Boolean = false,
    val deleteProductId: Int? = null,
    val categoryFormOpen: Boolean = false,
    val editingCategoryId: Int? = null,
    val categoryForm: CategoryForm = CategoryForm(),
    val deleteCategoryId: Int? = null,
    val importDialogOpen: Boolean = false,
    val importValidation: BulkImportValidationResult? = null,
    val importProgress: BulkImportProgress? = null,
    val importLoading: Boolean = false,
    val stockAdjustment: StockAdjustmentForm? = null,
    val adjustmentLoading: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val hasMore: Boolean get() = products.size < total
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductosViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val productionRepository: ProductionRepository,
    private val digitalMenuRepository: DigitalMenuRepository,
    private val inventoryRepository: InventoryRepository,
    private val modifiersRepository: ModifiersRepository,
    private val preparationAreasRepository: PreparationAreasRepository,
    private val productImportRepository: ProductImportRepository,
    private val productImageRepository: ProductImageRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val tenantBaseUrl: String? = productImageRepository.tenantAssetsBaseUrl()

    private val _uiState = MutableStateFlow(ProductosUiState())
    val uiState: StateFlow<ProductosUiState> = _uiState.asStateFlow()

    private val searchFlow = MutableStateFlow("")

    init {
        loadCategories()
        loadBranches()
        loadModifierGroups()
        loadPreparationAreas()
        refreshProducts()
        viewModelScope.launch {
            searchFlow
                .debounce(900)
                .distinctUntilChanged()
                .collect { query ->
                    val normalized = query.trim()
                    if (normalized.length >= 2 || normalized.isEmpty()) {
                        _uiState.update { it.copy(searchQuery = normalized, page = 1) }
                        refreshProducts()
                    }
                }
        }
    }

    fun selectTab(tab: ProductosTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update { it.copy(tab = tab, error = null) }
        if (tab == ProductosTab.CATEGORIAS) loadCategories()
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        searchFlow.value = value
    }

    fun setCategoryFilter(categoryId: Int?) {
        _uiState.update { it.copy(categoryFilterId = categoryId, page = 1) }
        refreshProducts()
    }

    fun setAreaFilter(areaId: Int?) {
        _uiState.update { it.copy(areaFilterId = areaId, page = 1) }
        refreshProducts()
    }

    fun setBranchFilter(branchId: Int?) {
        _uiState.update { it.copy(branchFilterId = branchId, page = 1) }
        refreshProducts()
    }

    fun setProductTypeFilter(filter: String) {
        if (_uiState.value.productTypeFilter == filter) return
        _uiState.update { it.copy(productTypeFilter = filter, page = 1) }
        refreshProducts()
    }

    fun setShowInactive(value: Boolean) {
        if (_uiState.value.showInactive == value) return
        _uiState.update { it.copy(showInactive = value, page = 1) }
        refreshProducts()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(loading = true, error = null) }
            when (
                val result = productsRepository.listProducts(
                    ProductListQuery(
                        query = state.searchQuery,
                        categoryId = state.categoryFilterId,
                        preparationAreaId = state.areaFilterId,
                        branchId = state.branchFilterId,
                        page = 1,
                        perPage = state.perPage,
                        productTypeFilter = state.productTypeFilter,
                        inactiveOnly = state.showInactive,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val products = result.data.first
                    _uiState.update {
                        it.copy(loading = false, products = products, total = result.data.second, page = 1)
                    }
                    loadStockForProducts(products)
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadMoreProducts() {
        val state = _uiState.value
        if (state.loading || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            when (
                val result = productsRepository.listProducts(
                    ProductListQuery(
                        query = state.searchQuery,
                        categoryId = state.categoryFilterId,
                        preparationAreaId = state.areaFilterId,
                        branchId = state.branchFilterId,
                        page = state.page + 1,
                        perPage = state.perPage,
                        productTypeFilter = state.productTypeFilter,
                        inactiveOnly = state.showInactive,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val merged = state.products + result.data.first
                    _uiState.update {
                        it.copy(loading = false, products = merged, total = result.data.second, page = state.page + 1)
                    }
                    loadStockForProducts(merged)
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = productsRepository.listCategories()) {
                is AppResult.Success -> _uiState.update { it.copy(categories = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadBranches() {
        viewModelScope.launch {
            when (val result = settingsRepository.listBranches()) {
                is AppResult.Success -> _uiState.update { it.copy(branches = result.data.filter { it.active }) }
                else -> Unit
            }
        }
    }

    private suspend fun loadStockForProducts(products: List<ProductItem>) {
        val ids = products.filter { it.manageStock }.map { it.id }
        if (ids.isEmpty()) {
            _uiState.update { it.copy(stockByProductId = emptyMap()) }
            return
        }
        when (val result = productsRepository.getStockSummary(ids)) {
            is AppResult.Success -> _uiState.update { it.copy(stockByProductId = result.data) }
            else -> Unit
        }
    }

    private fun loadPreparationAreas() {
        viewModelScope.launch {
            when (val result = preparationAreasRepository.listPreparationAreas(activeOnly = true)) {
                is AppResult.Success -> _uiState.update { it.copy(preparationAreas = result.data) }
                is AppResult.Error -> Unit
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadModifierGroups() {
        viewModelScope.launch {
            when (val result = modifiersRepository.listModifierGroups()) {
                is AppResult.Success -> _uiState.update { it.copy(modifierGroups = result.data) }
                is AppResult.Error -> Unit
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreateProduct() {
        _uiState.update {
            it.copy(
                productFormOpen = true,
                editingProductId = null,
                productForm = ProductFormInput(code = generateProductCode(), menuChannelEnabled = true),
                recipeDraft = null,
                showMoreOptions = false,
                presentationsOpen = false,
                error = null,
            )
        }
    }

    fun openEditProduct(productId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val detailResult = productsRepository.getProductDetail(productId)
            val channelsResult = digitalMenuRepository.getProductPublicationChannels(productId)
            when (detailResult) {
                is AppResult.Success -> {
                    val menuEnabled = when (channelsResult) {
                        is AppResult.Success -> channelsResult.data.isMenuChannelEnabled()
                        else -> true
                    }
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            productFormOpen = true,
                            editingProductId = productId,
                            productForm = detailResult.data.toFormInput().copy(menuChannelEnabled = menuEnabled),
                            recipeDraft = null,
                            showMoreOptions = true,
                        )
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = detailResult.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissProductForm() {
        _uiState.update {
            it.copy(
                productFormOpen = false,
                editingProductId = null,
                recipeDraft = null,
                showMoreOptions = false,
                presentationsOpen = false,
            )
        }
    }

    fun updateProductForm(transform: (ProductFormInput) -> ProductFormInput) {
        _uiState.update { it.copy(productForm = transform(it.productForm)) }
    }

    /** Borrador local de receta — NO persiste en el backend. Se guarda recién cuando
     * saveProduct() confirma el resto del formulario, junto con internal/production. */
    fun setRecipeDraft(draft: RecipeDraft) {
        _uiState.update { it.copy(recipeDraft = draft) }
    }

    fun toggleMoreOptions() {
        _uiState.update { it.copy(showMoreOptions = !it.showMoreOptions) }
    }

    fun togglePresentationsSheet() {
        _uiState.update { state ->
            val form = state.productForm
            val presentations = if (form.presentations.isEmpty()) {
                listOf(ProductPresentation(name = "", salePrice = form.salePrice.replace(",", ".").toDoubleOrNull() ?: 0.0))
            } else form.presentations
            state.copy(presentationsOpen = !state.presentationsOpen, productForm = form.copy(presentations = presentations, hasVariants = true))
        }
    }

    fun dismissPresentationsSheet() {
        _uiState.update { it.copy(presentationsOpen = false) }
    }

    fun toggleModifierGroup(groupId: Int) {
        _uiState.update { state ->
            val ids = state.productForm.modifierGroupIds.toMutableList()
            if (ids.contains(groupId)) ids.remove(groupId) else ids.add(groupId)
            state.copy(productForm = state.productForm.copy(modifierGroupIds = ids, hasModifiers = ids.isNotEmpty()))
        }
    }

    fun setPendingImage(bytes: ByteArray, mimeType: String) {
        _uiState.update { it.copy(productForm = it.productForm.copy(pendingImageBytes = bytes, pendingImageMimeType = mimeType)) }
    }

    /** Sube y reemplaza la imagen de un producto directamente desde la lista, sin abrir el formulario. */
    suspend fun uploadQuickProductImage(productId: Int, bytes: ByteArray, mimeType: String) {
        when (val result = productImageRepository.uploadProductImage(productId, bytes, mimeType)) {
            is AppResult.Success -> _uiState.update { state ->
                state.copy(products = state.products.map { if (it.id == productId) it.copy(imageUrl = result.data) else it })
            }
            is AppResult.Error -> _uiState.update { it.copy(snackMessage = "No se pudo subir la imagen: ${result.message}") }
            AppResult.Loading -> Unit
        }
    }

    fun openImportDialog() {
        _uiState.update { it.copy(importDialogOpen = true, importValidation = null, importProgress = null, error = null) }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(importDialogOpen = false, importValidation = null, importProgress = null, importLoading = false) }
    }

    fun getImportTemplateBytes(): ByteArray = productImportRepository.generateTemplateBytes()

    fun validateImportFile(bytes: ByteArray) {
        viewModelScope.launch {
            val validation = productImportRepository.validateExcel(bytes)
            _uiState.update { it.copy(importValidation = validation, importProgress = null, error = null) }
        }
    }

    fun runImport() {
        val validation = _uiState.value.importValidation ?: return
        if (validation.errors.isNotEmpty() || validation.rows.isEmpty()) {
            _uiState.update { it.copy(error = "Corrige los errores del Excel antes de importar") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(importLoading = true, error = null) }
            val categoryMap = _uiState.value.categories.associate { it.name.trim().lowercase() to it.id }
            when (val result = productImportRepository.importRows(validation.rows, categoryMap)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            importLoading = false,
                            importProgress = result.data,
                            snackMessage = "Importados ${result.data.created} productos",
                        )
                    }
                    refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(importLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        val form = state.productForm
        if (form.name.trim().isEmpty()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        val price = form.salePrice.replace(",", ".").toDoubleOrNull()
        if (price == null || price < 0) {
            _uiState.update { it.copy(error = "Ingresa un precio de venta válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (state.editingProductId == null) {
                productsRepository.createProduct(form)
            } else {
                productsRepository.updateProduct(state.editingProductId, form)
            }
            when (result) {
                is AppResult.Success -> {
                    val productId = result.data.id
                    val pendingBytes = form.pendingImageBytes
                    val pendingMime = form.pendingImageMimeType
                    when (val channelResult = digitalMenuRepository.setMenuChannelEnabled(productId, form.menuChannelEnabled)) {
                        is AppResult.Error -> {
                            _uiState.update { it.copy(snackMessage = "Producto guardado; no se pudo actualizar el canal menú digital") }
                        }
                        else -> Unit
                    }
                    if (pendingBytes != null && pendingMime != null) {
                        when (val imageResult = productImageRepository.uploadProductImage(productId, pendingBytes, pendingMime)) {
                            is AppResult.Success -> Unit
                            is AppResult.Error -> {
                                _uiState.update {
                                    it.copy(snackMessage = "Producto guardado; no se pudo subir la imagen: ${imageResult.message}")
                                }
                            }
                            AppResult.Loading -> Unit
                        }
                    }
                    val draft = state.recipeDraft
                    var recipeSnack: String? = null
                    if (draft != null) {
                        when (productionRepository.upsertRecipe(productId, draft.notes, draft.items)) {
                            is AppResult.Success -> Unit
                            is AppResult.Error -> {
                                recipeSnack = "Producto guardado; no se pudo guardar la receta"
                            }
                            AppResult.Loading -> Unit
                        }
                    }
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            productFormOpen = false,
                            editingProductId = null,
                            recipeDraft = null,
                            snackMessage = recipeSnack
                                ?: if (state.editingProductId == null) "Producto creado" else "Producto actualizado",
                        )
                    }
                    refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteProduct(productId: Int) { _uiState.update { it.copy(deleteProductId = productId) } }
    fun dismissDeleteProduct() { _uiState.update { it.copy(deleteProductId = null) } }

    fun confirmDeleteProduct() {
        val productId = _uiState.value.deleteProductId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, deleteProductId = null) }
            when (val result = productsRepository.deleteProduct(productId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, snackMessage = "Producto eliminado") }
                    refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Activa/desactiva un producto sin eliminarlo. */
    fun toggleProductActive(productId: Int) {
        val product = _uiState.value.products.firstOrNull { it.id == productId }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = productsRepository.toggleProduct(productId)) {
                is AppResult.Success -> {
                    val message = if (product?.active == false) "Producto activado" else "Producto desactivado"
                    _uiState.update { it.copy(actionLoading = false, snackMessage = message) }
                    refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openStockAdjustment(product: ProductItem) {
        val defaultBranch = _uiState.value.branches.firstOrNull()?.id
            ?: _uiState.value.branchFilterId
        _uiState.update {
            it.copy(
                stockAdjustment = StockAdjustmentForm(
                    productId = product.id,
                    productName = product.name,
                    branchId = defaultBranch,
                ),
                error = null,
            )
        }
    }

    fun dismissStockAdjustment() {
        _uiState.update { it.copy(stockAdjustment = null, adjustmentLoading = false) }
    }

    fun updateStockAdjustment(transform: (StockAdjustmentForm) -> StockAdjustmentForm) {
        _uiState.update { state ->
            val current = state.stockAdjustment ?: return@update state
            state.copy(stockAdjustment = transform(current))
        }
    }

    fun confirmStockAdjustment() {
        val adjustment = _uiState.value.stockAdjustment ?: return
        val branchId = adjustment.branchId
        if (branchId == null || branchId <= 0) {
            _uiState.update { it.copy(error = "Selecciona una sucursal") }
            return
        }
        val qty = adjustment.quantity.replace(",", ".").toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(error = "La cantidad debe ser mayor a 0") }
            return
        }
        if (adjustment.notes.trim().isEmpty()) {
            _uiState.update { it.copy(error = "Indica el motivo del ajuste") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(adjustmentLoading = true, error = null) }
            if (!adjustment.isIncrease) {
                when (val stockResult = inventoryRepository.getStock(adjustment.productId, branchId)) {
                    is AppResult.Success -> {
                        if (qty > stockResult.data) {
                            _uiState.update {
                                it.copy(
                                    adjustmentLoading = false,
                                    error = "Stock insuficiente. Disponible: ${stockResult.data}",
                                )
                            }
                            return@launch
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(adjustmentLoading = false, error = stockResult.message) }
                        return@launch
                    }
                    AppResult.Loading -> return@launch
                }
            }
            when (
                val result = inventoryRepository.recordAdjustment(
                    InventoryAdjustmentInput(
                        productId = adjustment.productId,
                        branchId = branchId,
                        type = if (adjustment.isIncrease) "in" else "out",
                        quantity = qty,
                        notes = adjustment.notes.trim(),
                    ),
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            adjustmentLoading = false,
                            stockAdjustment = null,
                            snackMessage = "Ajuste registrado en kardex",
                        )
                    }
                    refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(adjustmentLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreateCategory() {
        _uiState.update { it.copy(categoryFormOpen = true, editingCategoryId = null, categoryForm = CategoryForm(), error = null) }
    }

    fun openEditCategory(categoryId: Int) {
        val category = _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return
        _uiState.update {
            it.copy(categoryFormOpen = true, editingCategoryId = categoryId, categoryForm = CategoryForm(name = category.name, description = category.description.orEmpty()), error = null)
        }
    }

    fun dismissCategoryForm() { _uiState.update { it.copy(categoryFormOpen = false, editingCategoryId = null) } }
    fun updateCategoryForm(transform: (CategoryForm) -> CategoryForm) { _uiState.update { it.copy(categoryForm = transform(it.categoryForm)) } }

    fun saveCategory() {
        val state = _uiState.value
        val name = state.categoryForm.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "El nombre de categoría es obligatorio") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (state.editingCategoryId == null) {
                productsRepository.createCategory(name, state.categoryForm.description)
            } else {
                productsRepository.updateCategory(state.editingCategoryId, name, state.categoryForm.description)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, categoryFormOpen = false, editingCategoryId = null, snackMessage = if (state.editingCategoryId == null) "Categoría creada" else "Categoría actualizada") }
                    loadCategories()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDeleteCategory(categoryId: Int) { _uiState.update { it.copy(deleteCategoryId = categoryId) } }
    fun dismissDeleteCategory() { _uiState.update { it.copy(deleteCategoryId = null) } }

    fun confirmDeleteCategory() {
        val categoryId = _uiState.value.deleteCategoryId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, deleteCategoryId = null) }
            when (val result = productsRepository.deleteCategory(categoryId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(actionLoading = false, snackMessage = "Categoría eliminada") }
                    loadCategories()
                    if (_uiState.value.categoryFilterId == categoryId) setCategoryFilter(null) else refreshProducts()
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnackMessage() { _uiState.update { it.copy(snackMessage = null) } }

    fun showSnackMessage(message: String) {
        _uiState.update { it.copy(snackMessage = message) }
    }
}
