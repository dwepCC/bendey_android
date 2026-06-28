package com.bendey.restaurant.feature.combos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.ComboItem
import com.bendey.restaurant.core.domain.products.CatalogSection
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.CatalogSectionNav
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.core.ui.layout.rememberIsExpandedWidth
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombosScreen(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenModificadores: () -> Unit = {},
    onOpenAreasPreparacion: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CombosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val listState = rememberLazyListState()
    val isExpanded = rememberIsExpandedWidth()
    val listContentPadding = rememberBendeyLazyListContentPadding(
        includeBottomBar = !isExpanded,
        horizontal = BendeySpacing.md,
        top = BendeySpacing.md,
    )

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Combos",
                subtitle = "${state.combos.size} combos",
                onBack = onBack,
                actions = {
                    BendeyIconButton(
                        onClick = viewModel::refresh,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                    )
                    BendeyIconButton(
                        onClick = viewModel::openCreate,
                        icon = Icons.Default.Add,
                        contentDescription = "Nuevo combo",
                    )
                },
            )
            CatalogSectionNav(
                current = CatalogSection.COMBOS,
                onOpenProductos = onOpenProductos,
                onOpenModificadores = onOpenModificadores,
                onOpenAreasPreparacion = onOpenAreasPreparacion,
                onOpenCombos = {},
            )
            state.error?.takeIf { !state.formOpen }?.let {
                Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md))
            }
            BendeyLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = listContentPadding,
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                items(state.combos, key = { it.id }) { combo ->
                    ComboRow(combo, currency, { viewModel.openEdit(combo.id) }, { viewModel.requestDelete(combo.id) })
                }
            }
        }
    }

    ComboEditorSheet(
        open = state.formOpen,
        form = state.form,
        editorTab = state.editorTab,
        branches = state.branches,
        productSearchQuery = state.productSearchQuery,
        productSearchResults = state.productSearchResults,
        productSearchLoading = state.productSearchLoading,
        loading = state.actionLoading,
        error = state.error,
        isEditing = state.editingId != null,
        activePicker = state.activePicker,
        onDismiss = viewModel::dismissForm,
        onTabChange = viewModel::setEditorTab,
        onFormChange = viewModel::updateForm,
        onProductSearchChange = viewModel::setProductSearchQuery,
        onOpenPicker = viewModel::openProductPicker,
        onClosePicker = viewModel::closeProductPicker,
        onSelectProduct = viewModel::selectProduct,
        onAddBranchRow = viewModel::addBranchRow,
        onSave = viewModel::save,
    )

    state.deleteId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = "Eliminar combo",
            message = "¿Eliminar este combo?",
            onConfirm = viewModel::confirmDelete,
            confirmText = "Eliminar",
        )
    }
}

@Composable
private fun ComboRow(combo: ComboItem, currency: NumberFormat, onEdit: () -> Unit, onDelete: () -> Unit) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                Text(combo.name, fontWeight = FontWeight.SemiBold)
                Text(combo.comboType.label, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                Text(currency.format(combo.basePrice), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
                Text(
                    "${combo.fixedItemsCount} fijos · ${combo.slotsCount} slots",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (combo.validFrom != null || combo.validTo != null) {
                    Text(
                        "Vigencia: ${combo.validFrom.orEmpty()} — ${combo.validTo.orEmpty()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                if (!combo.active) BendeyStatusChip("Inactivo", BendeyColors.Warning)
            }
            BendeyIconButton(
                onClick = onEdit,
                icon = Icons.Default.Edit,
                contentDescription = "Editar combo",
            )
            BendeyIconButton(
                onClick = onDelete,
                icon = Icons.Default.Delete,
                contentDescription = "Eliminar combo",
                tint = BendeyColors.Error,
            )
        }
    }
}
