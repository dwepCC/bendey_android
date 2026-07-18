package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyQuickImageThumb
import com.bendey.restaurant.core.domain.products.CategoryItem
import com.bendey.restaurant.core.domain.products.IgvAffectation
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.products.ProductFormInput
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductType
import com.bendey.restaurant.core.domain.products.ProductosTab
import com.bendey.restaurant.core.domain.products.UnitOfMeasure
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(
    onOpenModificadores: () -> Unit = {},
    onOpenAreasPreparacion: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProductosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    BendeyListScreenLayout(
            modifier = modifier.fillMaxSize(),
            isRefreshing = state.loading && state.products.isEmpty() && state.tab == ProductosTab.PRODUCTOS,
            onRefresh = {
                if (state.tab == ProductosTab.PRODUCTOS) viewModel.refreshProducts() else viewModel.loadCategories()
            },
            header = {
                BendeyScreenToolbar(
                    title = "Catálogo",
                    subtitle = when (state.tab) {
                        ProductosTab.PRODUCTOS -> "${state.total} productos"
                        ProductosTab.CATEGORIAS -> "${state.categories.size} categorías"
                    },
                    actions = {
                        if (state.tab == ProductosTab.PRODUCTOS) {
                            BendeyIconButton(
                                onClick = viewModel::openImportDialog,
                                icon = Icons.Default.UploadFile,
                                contentDescription = "Importar Excel",
                            )
                        }
                        BendeyIconButton(
                            onClick = {
                                if (state.tab == ProductosTab.PRODUCTOS) viewModel.refreshProducts()
                                else viewModel.loadCategories()
                            },
                            icon = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                        )
                        if (state.tab == ProductosTab.PRODUCTOS) {
                            BendeyIconButton(
                                onClick = viewModel::openCreateProduct,
                                icon = Icons.Default.Add,
                                contentDescription = "Nuevo producto",
                            )
                        } else {
                            BendeyIconButton(
                                onClick = viewModel::openCreateCategory,
                                icon = Icons.Default.Add,
                                contentDescription = "Nueva categoría",
                            )
                        }
                    },
                )
                ProductCatalogSectionNav(
                    onOpenModificadores = onOpenModificadores,
                    onOpenAreasPreparacion = onOpenAreasPreparacion,
                    onOpenCombos = onOpenCombos,
                )
                ProductosTabRow(selected = state.tab, onSelect = viewModel::selectTab)
            },
        ) { contentModifier ->
            when (state.tab) {
                ProductosTab.PRODUCTOS -> ProductsTabContent(
                    state = state,
                    currency = currency,
                    assetsBaseUrl = viewModel.tenantBaseUrl,
                    error = state.error?.takeIf { !state.productFormOpen },
                    onSearch = viewModel::setSearchQuery,
                    onCategoryFilter = viewModel::setCategoryFilter,
                    onBranchFilter = viewModel::setBranchFilter,
                    onProductTypeFilter = viewModel::setProductTypeFilter,
                    onShowInactiveChange = viewModel::setShowInactive,
                    onEdit = viewModel::openEditProduct,
                    onDelete = viewModel::requestDeleteProduct,
                    onToggleActive = viewModel::toggleProductActive,
                    onAdjustStock = viewModel::openStockAdjustment,
                    onLoadMore = viewModel::loadMoreProducts,
                    onQuickImagePicked = viewModel::uploadQuickProductImage,
                    modifier = contentModifier,
                )
                ProductosTab.CATEGORIAS -> CategoriesTabContent(
                    categories = state.categories,
                    loading = state.loading,
                    error = state.error?.takeIf { !state.categoryFormOpen },
                    onEdit = viewModel::openEditCategory,
                    onDelete = viewModel::requestDeleteCategory,
                    modifier = contentModifier,
                )
            }
        }

    if (state.productFormOpen) {
        ProductFormDialog(
            form = state.productForm,
            categories = state.categories,
            preparationAreas = state.preparationAreas,
            modifierGroups = state.modifierGroups,
            tenantBaseUrl = viewModel.tenantBaseUrl,
            showMoreOptions = state.showMoreOptions,
            presentationsOpen = state.presentationsOpen,
            loading = state.actionLoading,
            error = state.error,
            isEditing = state.editingProductId != null,
            editingProductId = state.editingProductId,
            recipeDraft = state.recipeDraft,
            onDismiss = viewModel::dismissProductForm,
            onFormChange = viewModel::updateProductForm,
            onToggleMore = viewModel::toggleMoreOptions,
            onTogglePresentations = viewModel::togglePresentationsSheet,
            onDismissPresentations = viewModel::dismissPresentationsSheet,
            onToggleModifierGroup = viewModel::toggleModifierGroup,
            onImagePicked = viewModel::setPendingImage,
            onSaveRecipeDraft = viewModel::setRecipeDraft,
            onSave = viewModel::saveProduct,
        )
    }

    ProductImportDialog(
        open = state.importDialogOpen,
        validation = state.importValidation,
        progress = state.importProgress,
        loading = state.importLoading,
        error = state.error?.takeIf { state.importDialogOpen },
        onDismiss = viewModel::dismissImportDialog,
        onFilePicked = viewModel::validateImportFile,
        onImport = viewModel::runImport,
        onDownloadTemplate = viewModel::getImportTemplateBytes,
        onDownloadError = { message -> viewModel.showSnackMessage(message) },
    )

    if (state.categoryFormOpen) {
        CategoryFormDialog(
            form = state.categoryForm,
            loading = state.actionLoading,
            error = state.error,
            isEditing = state.editingCategoryId != null,
            onDismiss = viewModel::dismissCategoryForm,
            onFormChange = viewModel::updateCategoryForm,
            onSave = viewModel::saveCategory,
        )
    }

    state.deleteProductId?.let {
        ConfirmDeleteDialog(
            title = "Eliminar producto",
            message = "¿Eliminar este producto del catálogo?",
            onDismiss = viewModel::dismissDeleteProduct,
            onConfirm = viewModel::confirmDeleteProduct,
        )
    }

    state.deleteCategoryId?.let {
        ConfirmDeleteDialog(
            title = "Eliminar categoría",
            message = "¿Eliminar esta categoría?",
            onDismiss = viewModel::dismissDeleteCategory,
            onConfirm = viewModel::confirmDeleteCategory,
        )
    }

    state.stockAdjustment?.let { adjustment ->
        StockAdjustmentDialog(
            form = adjustment,
            branches = state.branches,
            loading = state.adjustmentLoading,
            error = state.error?.takeIf { state.stockAdjustment != null },
            onDismiss = viewModel::dismissStockAdjustment,
            onFormChange = viewModel::updateStockAdjustment,
            onConfirm = viewModel::confirmStockAdjustment,
        )
    }
}

@Composable
private fun ProductosTabRow(
    selected: ProductosTab,
    onSelect: (ProductosTab) -> Unit,
) {
    BendeyHorizontalScrollRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = BendeySpacing.md,
            vertical = BendeySpacing.xs,
        ),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        ProductosTab.entries.forEach { tab ->
            BendeyFilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                text = tab.label,
            )
        }
    }
}

@Composable
private fun ProductsTabContent(
    state: ProductosUiState,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    error: String?,
    selectedProductId: Int? = null,
    onSearch: (String) -> Unit,
    onCategoryFilter: (Int?) -> Unit,
    onBranchFilter: (Int?) -> Unit,
    onProductTypeFilter: (String) -> Unit,
    onShowInactiveChange: (Boolean) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onToggleActive: (Int) -> Unit,
    onAdjustStock: (ProductItem) -> Unit,
    onLoadMore: () -> Unit,
    onQuickImagePicked: suspend (Int, ByteArray, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxSize()) {
        error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs))
        }
        val categoryOptions = remember(state.categories) {
            listOf(BendeySelectOption(-1, "Todas las categorías")) +
                state.categories.map { BendeySelectOption(it.id, it.name) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                label = "Buscar",
                modifier = Modifier.weight(1.2f),
            )
            BendeySearchableSelect(
                options = categoryOptions,
                selectedId = state.categoryFilterId ?: -1,
                onSelect = { id -> onCategoryFilter(if (id == -1) null else id) },
                label = "",
                placeholder = "Categoría",
                modifier = Modifier.weight(1f),
            )
        }
        BendeyHorizontalScrollRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyFilterChip(
                selected = state.productTypeFilter == "non_insumo",
                onClick = { onProductTypeFilter("non_insumo") },
                text = "Productos",
            )
            BendeyFilterChip(
                selected = state.productTypeFilter == "insumo",
                onClick = { onProductTypeFilter("insumo") },
                text = "Insumos",
            )
            BendeyFilterChip(
                selected = state.showInactive,
                onClick = { onShowInactiveChange(!state.showInactive) },
                text = "Solo inactivos",
            )
        }
        if (state.branches.size > 1) {
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.md,
                    vertical = BendeySpacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                BendeyFilterChip(
                    selected = state.branchFilterId == null,
                    onClick = { onBranchFilter(null) },
                    text = "Todas sucursales",
                )
                state.branches.forEach { branch ->
                    BendeyFilterChip(
                        selected = state.branchFilterId == branch.id,
                        onClick = { onBranchFilter(branch.id) },
                        label = { Text(branch.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        BendeyFlexibleContentSlot {
            if (state.products.isEmpty() && !state.loading) {
                BendeyEmptyState(
                    title = "Sin productos",
                    inline = true,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            } else {
                BendeyLazyColumn(
                    modifier = it,
                    state = listState,
                    contentPadding = PaddingValues(
                        start = BendeySpacing.md,
                        end = BendeySpacing.md,
                        top = BendeySpacing.md,
                        bottom = BendeySpacing.md + bottomScrollPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            stockQty = state.stockByProductId[product.id],
                            currency = currency,
                            assetsBaseUrl = assetsBaseUrl,
                            selected = product.id == selectedProductId,
                            onEdit = { onEdit(product.id) },
                            onDelete = { onDelete(product.id) },
                            onToggleActive = { onToggleActive(product.id) },
                            onAdjustStock = { onAdjustStock(product) },
                            onImagePicked = { bytes, mime -> onQuickImagePicked(product.id, bytes, mime) },
                        )
                    }
                    if (state.hasMore) {
                        item {
                            BendeyPrimaryButton(
                                text = if (state.loading) "Cargando…" else "Cargar más",
                                onClick = onLoadMore,
                                enabled = !state.loading,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductItem,
    stockQty: Double?,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    selected: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onAdjustStock: () -> Unit,
    onImagePicked: suspend (ByteArray, String) -> Unit,
) {
    val imageUrl = resolvePublicAssetUrl(assetsBaseUrl, product.imageUrl).takeIf { it.isNotBlank() }
    var menuExpanded by remember { mutableStateOf(false) }
    BendeyCard(
        containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            BendeyQuickImageThumb(
                imageUrl = imageUrl,
                contentDescription = product.name,
                onImagePicked = onImagePicked,
                size = 60.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    product.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.categoryName != null || product.preparationArea?.name != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        product.categoryName?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        product.preparationArea?.name?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Text(
                    currency.format(product.salePrice),
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (!product.active || !product.availableForSale || product.manageStock) {
                    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), modifier = Modifier.padding(top = 2.dp)) {
                        if (!product.active) {
                            BendeyStatusChip(label = "Inactivo", accentColor = BendeyColors.Error)
                        }
                        if (!product.availableForSale) {
                            BendeyStatusChip(label = "Solo combo", accentColor = BendeyColors.Warning)
                        }
                        if (product.manageStock) {
                            val stockLabel = stockQty?.let { qty ->
                                if (qty % 1.0 == 0.0) "Stock: ${qty.toInt()}" else "Stock: $qty"
                            } ?: "Stock"
                            val lowStock = stockQty != null && product.minStock > 0 && stockQty < product.minStock
                            BendeyStatusChip(
                                label = stockLabel,
                                accentColor = if (lowStock) BendeyColors.Warning else BendeyColors.Info,
                            )
                        }
                    }
                }
            }
            ProductRowOverflowMenu(
                productName = product.name,
                showAdjustStock = product.manageStock,
                isActive = product.active,
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onEdit = onEdit,
                onAdjustStock = onAdjustStock,
                onToggleActive = onToggleActive,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ProductRowOverflowMenu(
    productName: String,
    showAdjustStock: Boolean,
    isActive: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        BendeyIconButton(
            onClick = { onExpandedChange(true) },
            icon = Icons.Default.MoreVert,
            contentDescription = "Acciones de $productName",
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text("Editar") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onEdit()
                },
            )
            if (showAdjustStock) {
                DropdownMenuItem(
                    text = { Text("Ajuste de stock") },
                    leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    onClick = {
                        onExpandedChange(false)
                        onAdjustStock()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(if (isActive) "Desactivar" else "Activar") },
                leadingIcon = {
                    Icon(
                        if (isActive) Icons.Default.ToggleOff else Icons.Default.ToggleOn,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onToggleActive()
                },
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = BendeyColors.Error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error) },
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun CategoriesTabContent(
    categories: List<CategoryItem>,
    loading: Boolean,
    error: String?,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    val listState = rememberLazyListState()
    Column(modifier = modifier) {
        error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (categories.isEmpty() && !loading) {
                BendeyEmptyState(
                    title = "Sin categorías",
                    inline = true,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            } else {
                BendeyLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = BendeySpacing.md,
                        end = BendeySpacing.md,
                        top = BendeySpacing.md,
                        bottom = BendeySpacing.md + bottomScrollPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    items(categories, key = { it.id }) { category ->
                        BendeyManagementCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.name, fontWeight = FontWeight.SemiBold)
                                    category.description?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                                    }
                                }
                                BendeyIconButton(
                                    onClick = { onEdit(category.id) },
                                    icon = Icons.Default.Edit,
                                    contentDescription = "Editar categoría",
                                )
                                BendeyIconButton(
                                    onClick = { onDelete(category.id) },
                                    icon = Icons.Default.Delete,
                                    contentDescription = "Eliminar categoría",
                                    tint = BendeyColors.Error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFormPane(
    form: ProductFormInput,
    categories: List<CategoryItem>,
    preparationAreas: List<PreparationAreaItem>,
    modifierGroups: List<com.bendey.restaurant.core.domain.catalog.ModifierGroup>,
    tenantBaseUrl: String?,
    showMoreOptions: Boolean,
    presentationsOpen: Boolean,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    editingProductId: Int?,
    recipeDraft: RecipeDraft?,
    onDismiss: () -> Unit,
    onFormChange: ((ProductFormInput) -> ProductFormInput) -> Unit,
    onToggleMore: () -> Unit,
    onTogglePresentations: () -> Unit,
    onDismissPresentations: () -> Unit,
    onToggleModifierGroup: (Int) -> Unit,
    onImagePicked: (ByteArray, String) -> Unit,
    onSaveRecipeDraft: (RecipeDraft) -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            if (isEditing) "Editar producto" else "Nuevo producto",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = BendeySpacing.sm),
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            ProductFormFields(
                form = form,
                categories = categories,
                preparationAreas = preparationAreas,
                modifierGroups = modifierGroups,
                tenantBaseUrl = tenantBaseUrl,
                showMoreOptions = showMoreOptions,
                presentationsOpen = presentationsOpen,
                isEditing = isEditing,
                editingProductId = editingProductId,
                recipeDraft = recipeDraft,
                error = error,
                onFormChange = onFormChange,
                onToggleMore = onToggleMore,
                onTogglePresentations = onTogglePresentations,
                onDismissPresentations = onDismissPresentations,
                onToggleModifierGroup = onToggleModifierGroup,
                onImagePicked = onImagePicked,
                onSaveRecipeDraft = onSaveRecipeDraft,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            BendeyTextButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            )
            BendeyPrimaryButton(
                text = if (loading) "Guardando…" else "Guardar",
                onClick = onSave,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProductFormFields(
    form: ProductFormInput,
    categories: List<CategoryItem>,
    preparationAreas: List<PreparationAreaItem>,
    modifierGroups: List<com.bendey.restaurant.core.domain.catalog.ModifierGroup>,
    tenantBaseUrl: String?,
    showMoreOptions: Boolean,
    presentationsOpen: Boolean,
    isEditing: Boolean,
    editingProductId: Int?,
    recipeDraft: RecipeDraft?,
    error: String?,
    onFormChange: ((ProductFormInput) -> ProductFormInput) -> Unit,
    onToggleMore: () -> Unit,
    onTogglePresentations: () -> Unit,
    onDismissPresentations: () -> Unit,
    onToggleModifierGroup: (Int) -> Unit,
    onImagePicked: (ByteArray, String) -> Unit,
    onSaveRecipeDraft: (RecipeDraft) -> Unit,
) {
        var recipeEditorOpen by remember { mutableStateOf(false) }
        BendeyTextField(
            value = form.name,
            onValueChange = { value -> onFormChange { it.copy(name = value) } },
            label = "Nombre *",
        )
        BendeyTextField(
            value = form.code,
            onValueChange = { value -> onFormChange { it.copy(code = value) } },
            label = "Código",
        )
        BendeyTextField(
            value = form.salePrice,
            onValueChange = { value -> onFormChange { it.copy(salePrice = value) } },
            label = "Precio venta *",
        )
        val categoryFormOptions = remember(categories) {
            listOf(BendeySelectOption(-1, "Sin categoría")) +
                categories.map { BendeySelectOption(it.id, it.name) }
        }
        BendeySearchableSelect(
            options = categoryFormOptions,
            selectedId = form.categoryId ?: -1,
            onSelect = { id -> onFormChange { it.copy(categoryId = if (id == -1) null else id) } },
            label = "Categoría",
            placeholder = "Buscar categoría…",
        )
        val preparationAreaOptions = remember(preparationAreas) {
            listOf(BendeySelectOption(-1, "Sin área")) +
                preparationAreas.filter { it.active }.map { BendeySelectOption(it.id, it.name) }
        }
        BendeySearchableSelect(
            options = preparationAreaOptions,
            selectedId = form.preparationAreaId ?: -1,
            onSelect = { id -> onFormChange { it.copy(preparationAreaId = if (id == -1) null else id) } },
            label = "Área de preparación",
            placeholder = "Buscar área…",
        )
        BendeySimpleSelect(
            options = UnitOfMeasure.entries.map { BendeyOption(it.code, it.label) },
            selectedValue = form.unit,
            onSelect = { code -> onFormChange { it.copy(unit = UnitOfMeasure.fromCode(code).code) } },
            label = "Unidad de medida",
        )
        Text(
            "Para insumos, es la unidad en la que se controla su stock (kg, litro, unidad…).",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
        BendeySimpleSelect(
            options = IgvAffectation.entries.map { BendeyOption(it.code, it.label) },
            selectedValue = form.igvAffectation.code,
            onSelect = { code ->
                val igv = IgvAffectation.fromCode(code)
                onFormChange { it.copy(igvAffectation = igv) }
            },
            label = "Afectación IGV",
        )
        BendeySimpleSelect(
            options = ProductType.entries.map { BendeyOption(it.code, it.label) },
            selectedValue = form.productType.code,
            onSelect = { code ->
                val productType = ProductType.fromCode(code)
                onFormChange {
                    it.copy(
                        productType = productType,
                        manageStock = if (productType == ProductType.ELABORADO) false else it.manageStock,
                        availableForSale = if (productType == ProductType.INSUMO) false else it.availableForSale,
                    )
                }
            },
            label = "Tipo de producto",
        )
        if (form.productType == ProductType.ELABORADO) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                BendeyTextButton(
                    text = if (recipeDraft != null || editingProductId != null) "Gestionar receta" else "Armar receta",
                    onClick = { recipeEditorOpen = true },
                )
                if (recipeDraft != null) {
                    Text(
                        "${recipeDraft.items.size} ingrediente${if (recipeDraft.items.size == 1) "" else "s"} — " +
                            "se guarda${if (editingProductId != null) "n los cambios" else ""} al pulsar Guardar",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.Warning,
                    )
                } else if (editingProductId == null) {
                    Text(
                        "Sin receta todavía — se guarda junto con el producto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }
        BendeyCheckboxRow(
            label = "Visible en carta (POS/mesa)",
            checked = form.availableForSale,
            onCheckedChange = { checked -> onFormChange { it.copy(availableForSale = checked) } },
        )
        BendeyCheckboxRow(
            label = "Visible en menú digital",
            checked = form.menuChannelEnabled,
            onCheckedChange = { checked -> onFormChange { it.copy(menuChannelEnabled = checked) } },
        )
        if (IgvAffectation.isGravado(form.igvAffectation.code)) {
            BendeyCheckboxRow(
                label = "Precio incluye IGV",
                checked = form.priceIncludesIgv,
                onCheckedChange = { checked -> onFormChange { it.copy(priceIncludesIgv = checked) } },
            )
        }
        BendeyCheckboxRow(
            label = if (form.productType == ProductType.ELABORADO) {
                "Controlar stock (los elaborados no controlan stock propio — se descuenta vía receta)"
            } else {
                "Controlar stock"
            },
            checked = if (form.productType == ProductType.ELABORADO) false else form.manageStock,
            enabled = form.productType != ProductType.ELABORADO,
            onCheckedChange = { checked ->
                onFormChange {
                    it.copy(
                        manageStock = checked,
                        minStock = if (checked) it.minStock.ifBlank { "0" } else "0",
                        initialStock = if (checked) it.initialStock else "",
                    )
                }
            },
        )
        if (form.productType != ProductType.ELABORADO && form.manageStock) {
            BendeyTextField(
                value = form.minStock,
                onValueChange = { value -> onFormChange { it.copy(minStock = value) } },
                label = "Stock mínimo",
            )
            if (!isEditing) {
                BendeyTextField(
                    value = form.initialStock,
                    onValueChange = { value -> onFormChange { it.copy(initialStock = value) } },
                    label = "Stock inicial",
                )
            }
        }
        ProductImageSection(form, tenantBaseUrl, onImagePicked)
        BendeySectionTitle(text = "Presentaciones / variantes")
        BendeyTextButton(
            text = if (form.presentations.isEmpty()) "Agregar presentaciones" else "${form.presentations.size} presentación(es)",
            onClick = onTogglePresentations,
        )
        if (presentationsOpen) {
            ProductPresentationsSection(form.presentations) { list ->
                onFormChange { it.copy(presentations = list, hasVariants = list.any { p -> p.name.isNotBlank() }) }
            }
            BendeyTextButton(text = "Cerrar presentaciones", onClick = onDismissPresentations)
        }
        BendeySectionTitle(text = "Grupos de modificadores")
        ProductModifiersSection(modifierGroups, form.modifierGroupIds, onToggleModifierGroup)
        BendeyTextButton(
            text = if (showMoreOptions) "Menos opciones" else "Más opciones",
            onClick = onToggleMore,
        )
        if (showMoreOptions) {
            BendeyTextField(
                value = form.description,
                onValueChange = { value -> onFormChange { it.copy(description = value) } },
                label = "Descripción",
                singleLine = false,
            )
            BendeyTextField(
                value = form.purchasePrice,
                onValueChange = { value -> onFormChange { it.copy(purchasePrice = value) } },
                label = "Precio compra",
            )
        }
        error?.let {
            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
        }
        if (recipeEditorOpen) {
            RecipeEditorSheet(
                productId = editingProductId ?: 0,
                productName = form.name,
                initialDraft = recipeDraft,
                onDismiss = { recipeEditorOpen = false },
                onSave = onSaveRecipeDraft,
            )
        }
}

@Composable
private fun ProductFormDialog(
    form: ProductFormInput,
    categories: List<CategoryItem>,
    preparationAreas: List<PreparationAreaItem>,
    modifierGroups: List<com.bendey.restaurant.core.domain.catalog.ModifierGroup>,
    tenantBaseUrl: String?,
    showMoreOptions: Boolean,
    presentationsOpen: Boolean,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    editingProductId: Int?,
    recipeDraft: RecipeDraft?,
    onDismiss: () -> Unit,
    onFormChange: ((ProductFormInput) -> ProductFormInput) -> Unit,
    onToggleMore: () -> Unit,
    onTogglePresentations: () -> Unit,
    onDismissPresentations: () -> Unit,
    onToggleModifierGroup: (Int) -> Unit,
    onImagePicked: (ByteArray, String) -> Unit,
    onSaveRecipeDraft: (RecipeDraft) -> Unit,
    onSave: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar producto" else "Nuevo producto",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
        enableContentScroll = true,
    ) {
        ProductFormFields(
            form = form,
            categories = categories,
            preparationAreas = preparationAreas,
            modifierGroups = modifierGroups,
            tenantBaseUrl = tenantBaseUrl,
            showMoreOptions = showMoreOptions,
            presentationsOpen = presentationsOpen,
            isEditing = isEditing,
            editingProductId = editingProductId,
            recipeDraft = recipeDraft,
            error = error,
            onFormChange = onFormChange,
            onToggleMore = onToggleMore,
            onTogglePresentations = onTogglePresentations,
            onDismissPresentations = onDismissPresentations,
            onToggleModifierGroup = onToggleModifierGroup,
            onImagePicked = onImagePicked,
            onSaveRecipeDraft = onSaveRecipeDraft,
        )
    }
}

@Composable
private fun CategoryFormDialog(
    form: CategoryForm,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((CategoryForm) -> CategoryForm) -> Unit,
    onSave: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar categoría" else "Nueva categoría",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
    ) {
        BendeyTextField(
            value = form.name,
            onValueChange = { value -> onFormChange { it.copy(name = value) } },
            label = "Nombre *",
        )
        BendeyTextField(
            value = form.description,
            onValueChange = { value -> onFormChange { it.copy(description = value) } },
            label = "Descripción",
            singleLine = false,
        )
        error?.let {
            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = message,
        onConfirm = onConfirm,
        confirmText = "Eliminar",
    )
}
