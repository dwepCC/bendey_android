package com.bendey.restaurant.feature.areaspreparacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.DEFAULT_PREPARATION_AREA_COLORS
import com.bendey.restaurant.core.domain.catalog.PreparationAreaFormInput
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.products.CatalogSection
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeySwitch
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.CatalogSectionNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasPreparacionScreen(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenModificadores: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AreasPreparacionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Áreas de preparación",
                subtitle = "${state.filteredAreas.size} áreas",
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
                        contentDescription = "Nueva área",
                    )
                },
            )
            CatalogSectionNav(
                current = CatalogSection.AREAS_PREPARACION,
                onOpenProductos = onOpenProductos,
                onOpenModificadores = onOpenModificadores,
                onOpenAreasPreparacion = {},
                onOpenCombos = onOpenCombos,
            )
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Buscar área",
                modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                BendeyFilterChip(
                    selected = !state.showInactive,
                    onClick = { viewModel.setShowInactive(false) },
                    text = "Activas",
                )
                BendeyFilterChip(
                    selected = state.showInactive,
                    onClick = { viewModel.setShowInactive(true) },
                    text = "Incluir inactivas",
                )
            }
            state.error?.let {
                Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md))
            }
            val listState = rememberLazyListState()
            BendeyLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                items(state.filteredAreas, key = { it.id }) { area ->
                    PreparationAreaRow(
                        area = area,
                        onEdit = { viewModel.openEdit(area.id) },
                        onToggleStatus = { active -> viewModel.toggleStatus(area.id, active) },
                    )
                }
            }
        }
    }

    if (state.formOpen) {
        PreparationAreaFormDialog(
            form = state.form,
            loading = state.actionLoading,
            error = state.error,
            isEditing = state.editingId != null,
            onDismiss = viewModel::dismissForm,
            onFormChange = viewModel::updateForm,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun PreparationAreaRow(
    area: PreparationAreaItem,
    onEdit: () -> Unit,
    onToggleStatus: (Boolean) -> Unit,
) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(parseAreaColor(area.color)),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = BendeySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(area.name, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                    if (!area.active) {
                        BendeyStatusChip(label = "Inactiva", accentColor = BendeyColors.Error)
                    }
                }
                if (area.description.isNotBlank()) {
                    Text(area.description, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                Text(
                    "Orden ${area.sortOrder} · ${area.estimatedMinutes} min estimados",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            BendeySwitch(
                checked = area.active,
                onCheckedChange = onToggleStatus,
            )
            BendeyIconButton(
                onClick = onEdit,
                icon = Icons.Default.Edit,
                contentDescription = "Editar área",
            )
        }
    }
}

@Composable
private fun PreparationAreaFormDialog(
    form: PreparationAreaFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((PreparationAreaFormInput) -> PreparationAreaFormInput) -> Unit,
    onSave: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar área" else "Nueva área",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
    ) {
        BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
        BendeyTextField(
            form.description,
            { v -> onFormChange { it.copy(description = v) } },
            "Descripción",
            singleLine = false,
        )
        BendeySectionTitle(text = "Color")
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            DEFAULT_PREPARATION_AREA_COLORS.forEach { color ->
                val selected = form.color.equals(color, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseAreaColor(color))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) BendeyColors.Primary else BendeyColors.Outline.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onFormChange { it.copy(color = color) } },
                )
            }
        }
        BendeyTextField(form.color, { v -> onFormChange { it.copy(color = v) } }, "Color (#hex)")
        BendeyTextField(form.estimatedMinutes, { v -> onFormChange { it.copy(estimatedMinutes = v) } }, "Minutos estimados")
        BendeyTextField(form.sortOrder, { v -> onFormChange { it.copy(sortOrder = v) } }, "Orden")
        if (isEditing) {
            BendeySwitchRow(
                label = "Activa",
                checked = form.active,
                onCheckedChange = { checked -> onFormChange { it.copy(active = checked) } },
            )
        }
        error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun parseAreaColor(raw: String): Color = runCatching {
    Color(raw.toColorInt())
}.getOrElse { BendeyColors.Primary }
