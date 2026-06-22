package com.bendey.restaurant.feature.modificadores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierGroupFormInput
import com.bendey.restaurant.core.domain.catalog.ModifierOption
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
import com.bendey.restaurant.core.domain.products.CatalogSection
import com.bendey.restaurant.core.ui.components.CatalogSectionNav
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificadoresScreen(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ModificadoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Modificadores",
                subtitle = "${state.groups.size} grupos",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = viewModel::openCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo grupo")
                    }
                },
            )
            CatalogSectionNav(
                current = CatalogSection.MODIFICADORES,
                onOpenProductos = onOpenProductos,
                onOpenModificadores = {},
                onOpenCombos = onOpenCombos,
            )
            state.error?.let {
                Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md))
            }
            LazyColumn(
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.groups, key = { it.id }) { group ->
                    ModifierGroupRow(
                        group = group,
                        currency = currency,
                        onEdit = { viewModel.openEdit(group.id) },
                        onDelete = { viewModel.requestDelete(group.id) },
                    )
                }
            }
        }
    }

    if (state.formOpen) {
        ModifierGroupFormDialog(
            form = state.form,
            loading = state.actionLoading,
            error = state.error,
            isEditing = state.editingId != null,
            onDismiss = viewModel::dismissForm,
            onFormChange = viewModel::updateForm,
            onSave = viewModel::save,
        )
    }

    state.deleteId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar grupo") },
            text = { Text("¿Eliminar este grupo de modificadores?") },
            confirmButton = { BendeyPrimaryButton("Eliminar", viewModel::confirmDelete) },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ModifierGroupRow(
    group: ModifierGroup,
    currency: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                Text(group.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${group.selectionMode.label}${if (group.required) " · Obligatorio" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                group.options.take(3).forEach { option ->
                    Text(
                        "• ${option.name} (+${currency.format(option.extraPrice)})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (group.options.size > 3) {
                    Text("+${group.options.size - 3} más", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
            }
        }
    }
}

@Composable
private fun ModifierGroupFormDialog(
    form: ModifierGroupFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((ModifierGroupFormInput) -> ModifierGroupFormInput) -> Unit,
    onSave: () -> Unit,
) {
    val selectionModeOptions = ModifierSelectionMode.entries.map { BendeyOption(it.apiValue, it.label) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar grupo" else "Nuevo grupo",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
    ) {
        BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
        BendeySimpleSelect(
            options = selectionModeOptions,
            selectedValue = form.selectionMode.apiValue,
            onSelect = { value ->
                val mode = ModifierSelectionMode.entries.firstOrNull { it.apiValue == value } ?: return@BendeySimpleSelect
                onFormChange { it.copy(selectionMode = mode) }
            },
            label = "Modo de selección",
        )
        BendeySwitchRow(
            label = "Obligatorio",
            checked = form.required,
            onCheckedChange = { checked -> onFormChange { it.copy(required = checked) } },
        )
        BendeyTextField(form.minSelect, { v -> onFormChange { it.copy(minSelect = v) } }, "Mínimo")
        BendeyTextField(form.maxSelect, { v -> onFormChange { it.copy(maxSelect = v) } }, "Máximo")
        Text("Opciones", fontWeight = FontWeight.SemiBold)
        form.options.forEachIndexed { index, option ->
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyTextField(
                    option.name,
                    { v -> onFormChange { f -> f.copy(options = f.options.mapIndexed { i, o -> if (i == index) o.copy(name = v) else o }) } },
                    "Opción",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    option.extraPrice.toString(),
                    { v -> onFormChange { f -> f.copy(options = f.options.mapIndexed { i, o -> if (i == index) o.copy(extraPrice = v.replace(",", ".").toDoubleOrNull() ?: 0.0) else o }) } },
                    "Extra",
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
        TextButton(onClick = { onFormChange { it.copy(options = it.options + ModifierOption(name = "")) } }) {
            Text("Agregar opción")
        }
        error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
    }
}
