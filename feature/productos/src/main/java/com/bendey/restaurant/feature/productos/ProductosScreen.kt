package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.products.CategoryItem
import com.bendey.restaurant.core.domain.products.IgvAffectation
import com.bendey.restaurant.core.domain.products.PreparationArea
import com.bendey.restaurant.core.domain.products.ProductFormInput
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductosTab
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(
    onOpenModificadores: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProductosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.products.isEmpty() && state.tab == ProductosTab.PRODUCTOS,
        onRefresh = {
            if (state.tab == ProductosTab.PRODUCTOS) viewModel.refreshProducts() else viewModel.loadCategories()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Catálogo",
                subtitle = when (state.tab) {
                    ProductosTab.PRODUCTOS -> "${state.total} productos"
                    ProductosTab.CATEGORIAS -> "${state.categories.size} categorías"
                },
                actions = {
                    if (state.tab == ProductosTab.PRODUCTOS) {
                        IconButton(onClick = viewModel::openImportDialog) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Importar Excel")
                        }
                    }
                    IconButton(onClick = {
                        if (state.tab == ProductosTab.PRODUCTOS) viewModel.refreshProducts()
                        else viewModel.loadCategories()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    if (state.tab == ProductosTab.PRODUCTOS) {
                        IconButton(onClick = viewModel::openCreateProduct) {
                            Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                        }
                    } else {
                        IconButton(onClick = viewModel::openCreateCategory) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
                        }
                    }
                },
            )
            ProductCatalogSectionNav(onOpenModificadores = onOpenModificadores, onOpenCombos = onOpenCombos)
            ProductosTabRow(selected = state.tab, onSelect = viewModel::selectTab)
            when (state.tab) {
                ProductosTab.PRODUCTOS -> ProductsTabContent(
                    state = state,
                    currency = currency,
                    assetsBaseUrl = viewModel.tenantBaseUrl,
                    error = state.error?.takeIf { !state.productFormOpen },
                    onSearch = viewModel::setSearchQuery,
                    onCategoryFilter = viewModel::setCategoryFilter,
                    onAreaFilter = viewModel::setAreaFilter,
                    onBranchFilter = viewModel::setBranchFilter,
                    onEdit = viewModel::openEditProduct,
                    onDelete = viewModel::requestDeleteProduct,
                    onLoadMore = viewModel::loadMoreProducts,
                )
                ProductosTab.CATEGORIAS -> CategoriesTabContent(
                    categories = state.categories,
                    loading = state.loading,
                    error = state.error?.takeIf { !state.categoryFormOpen },
                    onEdit = viewModel::openEditCategory,
                    onDelete = viewModel::requestDeleteCategory,
                )
            }
        }
    }

    if (state.productFormOpen) {
        ProductFormDialog(
            form = state.productForm,
            categories = state.categories,
            modifierGroups = state.modifierGroups,
            tenantBaseUrl = viewModel.tenantBaseUrl,
            showMoreOptions = state.showMoreOptions,
            presentationsOpen = state.presentationsOpen,
            loading = state.actionLoading,
            error = state.error,
            isEditing = state.editingProductId != null,
            onDismiss = viewModel::dismissProductForm,
            onFormChange = viewModel::updateProductForm,
            onToggleMore = viewModel::toggleMoreOptions,
            onTogglePresentations = viewModel::togglePresentationsSheet,
            onDismissPresentations = viewModel::dismissPresentationsSheet,
            onToggleModifierGroup = viewModel::toggleModifierGroup,
            onImagePicked = viewModel::setPendingImage,
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
}

@Composable
private fun ProductosTabRow(
    selected: ProductosTab,
    onSelect: (ProductosTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProductosTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
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
    onSearch: (String) -> Unit,
    onCategoryFilter: (Int?) -> Unit,
    onAreaFilter: (PreparationArea?) -> Unit,
    onBranchFilter: (Int?) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column {
        error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            label = "Buscar por nombre o código",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.categoryFilterId == null,
                onClick = { onCategoryFilter(null) },
                label = { Text("Todas") },
            )
            state.categories.forEach { category ->
                FilterChip(
                    selected = state.categoryFilterId == category.id,
                    onClick = { onCategoryFilter(category.id) },
                    label = { Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.areaFilter == null,
                onClick = { onAreaFilter(null) },
                label = { Text("Todas áreas") },
            )
            PreparationArea.entries.filter { it != PreparationArea.NONE }.forEach { area ->
                FilterChip(
                    selected = state.areaFilter == area,
                    onClick = { onAreaFilter(area) },
                    label = { Text(area.label) },
                )
            }
        }
        if (state.branches.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.branchFilterId == null,
                    onClick = { onBranchFilter(null) },
                    label = { Text("Todas sucursales") },
                )
                state.branches.forEach { branch ->
                    FilterChip(
                        selected = state.branchFilterId == branch.id,
                        onClick = { onBranchFilter(branch.id) },
                        label = { Text(branch.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        if (state.products.isEmpty() && !state.loading) {
            Text(
                "Sin productos",
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        stockQty = state.stockByProductId[product.id],
                        currency = currency,
                        assetsBaseUrl = assetsBaseUrl,
                        onEdit = { onEdit(product.id) },
                        onDelete = { onDelete(product.id) },
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

@Composable
private fun ProductRow(
    product: ProductItem,
    stockQty: Double?,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val imageUrl = resolvePublicAssetUrl(assetsBaseUrl, product.imageUrl).takeIf { it.isNotBlank() }
    BendeyManagementCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    product.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.categoryName?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                    }
                    product.preparationArea?.let {
                        Text(
                            PreparationArea.fromApi(it).label,
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
                Text(
                    currency.format(product.salePrice),
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!product.availableForSale) {
                        BendeyStatusChip(label = "Solo combo", accentColor = BendeyColors.Warning)
                    }
                    if (product.manageStock) {
                        val stockLabel = stockQty?.let { qty ->
                            if (qty % 1.0 == 0.0) "Stock: ${qty.toInt()}" else "Stock: $qty"
                        } ?: "Stock"
                        BendeyStatusChip(label = stockLabel, accentColor = BendeyColors.Info)
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
            }
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
) {
    Column(modifier = Modifier.fillMaxSize()) {
        error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp))
        }
    if (categories.isEmpty() && !loading) {
        Text(
            "Sin categorías",
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return@Column
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
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
                    IconButton(onClick = { onEdit(category.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onDelete(category.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ProductFormDialog(
    form: ProductFormInput,
    categories: List<CategoryItem>,
    modifierGroups: List<com.bendey.restaurant.core.domain.catalog.ModifierGroup>,
    tenantBaseUrl: String?,
    showMoreOptions: Boolean,
    presentationsOpen: Boolean,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((ProductFormInput) -> ProductFormInput) -> Unit,
    onToggleMore: () -> Unit,
    onTogglePresentations: () -> Unit,
    onDismissPresentations: () -> Unit,
    onToggleModifierGroup: (Int) -> Unit,
    onImagePicked: (ByteArray, String) -> Unit,
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
    ) {
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
        ProductImageSection(form, tenantBaseUrl, onImagePicked)
        Text("Presentaciones / variantes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onTogglePresentations) {
            Text(if (form.presentations.isEmpty()) "Agregar presentaciones" else "${form.presentations.size} presentación(es)")
        }
        if (presentationsOpen) {
            ProductPresentationsSection(form.presentations) { list ->
                onFormChange { it.copy(presentations = list, hasVariants = list.any { p -> p.name.isNotBlank() }) }
            }
            TextButton(onClick = onDismissPresentations) { Text("Cerrar presentaciones") }
        }
        Text("Grupos de modificadores", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        ProductModifiersSection(modifierGroups, form.modifierGroupIds, onToggleModifierGroup)
        Text("Categoría", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = form.categoryId == null,
                onClick = { onFormChange { it.copy(categoryId = null) } },
                label = { Text("Sin categoría") },
            )
            categories.forEach { category ->
                FilterChip(
                    selected = form.categoryId == category.id,
                    onClick = { onFormChange { it.copy(categoryId = category.id) } },
                    label = { Text(category.name) },
                )
            }
        }
        Text("Área de preparación", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreparationArea.entries.forEach { area ->
                FilterChip(
                    selected = form.preparationArea == area,
                    onClick = { onFormChange { it.copy(preparationArea = area) } },
                    label = { Text(area.label) },
                )
            }
        }
        Text("Afectación IGV", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IgvAffectation.entries.forEach { igv ->
                FilterChip(
                    selected = form.igvAffectation == igv,
                    onClick = { onFormChange { it.copy(igvAffectation = igv) } },
                    label = { Text(igv.code) },
                )
            }
        }
        ToggleRow(
            label = "Visible en carta (POS/mesa)",
            checked = form.availableForSale,
            onCheckedChange = { checked -> onFormChange { it.copy(availableForSale = checked) } },
        )
        if (IgvAffectation.isGravado(form.igvAffectation.code)) {
            ToggleRow(
                label = "Precio incluye IGV",
                checked = form.priceIncludesIgv,
                onCheckedChange = { checked -> onFormChange { it.copy(priceIncludesIgv = checked) } },
            )
        }
        ToggleRow(
            label = "Controlar stock",
            checked = form.manageStock,
            onCheckedChange = { checked -> onFormChange { it.copy(manageStock = checked) } },
        )
        if (form.manageStock && !isEditing) {
            BendeyTextField(
                value = form.initialStock,
                onValueChange = { value -> onFormChange { it.copy(initialStock = value) } },
                label = "Stock inicial",
            )
        }
        TextButton(onClick = onToggleMore) {
            Text(if (showMoreOptions) "Menos opciones" else "Más opciones")
        }
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
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BendeyColors.Surface,
        tonalElevation = 0.dp,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            BendeyPrimaryButton(text = "Eliminar", onClick = onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
