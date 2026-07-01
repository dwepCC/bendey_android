package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasAdminScreen(
    onOpenSession: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MesasAdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()

    Column(modifier = modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = "Configurar mesas",
            subtitle = "${state.filteredTables.size} mesas · ${state.floors.size} ambientes",
            actions = {
                BendeyIconButton(
                    onClick = viewModel::openFloorsSheet,
                    icon = Icons.Default.Layers,
                    contentDescription = "Ambientes",
                )
                BendeyIconButton(
                    onClick = viewModel::refresh,
                    icon = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                )
                BendeyIconButton(
                    onClick = viewModel::openCreateTable,
                    icon = Icons.Default.Add,
                    contentDescription = "Nueva mesa",
                )
            },
        )
        state.error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs))
        }
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = "Buscar mesa",
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
        )
        BendeyHorizontalScrollRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = BendeySpacing.md,
                vertical = BendeySpacing.xxs,
            ),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyFilterChip(
                selected = state.floorFilterId == null,
                onClick = { viewModel.setFloorFilter(null) },
                text = "Todos",
            )
            state.floors.forEach { floor ->
                BendeyFilterChip(
                    selected = state.floorFilterId == floor.id,
                    onClick = { viewModel.setFloorFilter(floor.id) },
                    text = floor.name,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.End,
        ) {
            BendeyFilterChip(
                selected = state.viewMode == MesasAdminViewMode.GRID,
                onClick = { viewModel.setViewMode(MesasAdminViewMode.GRID) },
                label = { Icon(Icons.Default.GridView, contentDescription = "Vista cuadrícula") },
            )
            BendeyFilterChip(
                selected = state.viewMode == MesasAdminViewMode.LIST,
                onClick = { viewModel.setViewMode(MesasAdminViewMode.LIST) },
                label = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Vista lista") },
                modifier = Modifier.padding(start = BendeySpacing.xs),
            )
        }
        if (state.filteredTables.isEmpty() && !state.loading) {
            Text(
                "Sin mesas",
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(BendeySpacing.md),
            )
        } else {
            val tables = state.paginatedTables
            if (state.viewMode == MesasAdminViewMode.GRID) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val profile = rememberBendeyAdaptiveProfile()
                    val columns = BendeyTabletTokens.tableGridColumns(profile)
                    BendeyLazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        state = rememberLazyGridState(),
                        contentPadding = PaddingValues(
                            start = BendeySpacing.md,
                            end = BendeySpacing.md,
                            top = BendeySpacing.md,
                            bottom = BendeySpacing.md + bottomScrollPadding,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    ) {
                        items(tables, key = { it.id }) { table ->
                            AdminTableCard(
                                table = table,
                                floorName = state.floors.firstOrNull { it.id == table.floorId }?.name ?: table.floorName,
                                deleteBlocked = viewModel.tableDeleteBlockedReason(table),
                                onEdit = { viewModel.openEditTable(table) },
                                onDelete = { viewModel.requestDeleteTable(table.id) },
                                onOpenSession = table.sessionId?.let { { onOpenSession(it) } },
                            )
                        }
                    }
                }
            } else {
                BendeyLazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(
                        start = BendeySpacing.md,
                        end = BendeySpacing.md,
                        top = BendeySpacing.md,
                        bottom = BendeySpacing.md + bottomScrollPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    items(tables, key = { it.id }) { table ->
                        AdminTableListRow(
                            table = table,
                            floorName = state.floors.firstOrNull { it.id == table.floorId }?.name ?: table.floorName,
                            deleteBlocked = viewModel.tableDeleteBlockedReason(table),
                            onEdit = { viewModel.openEditTable(table) },
                            onDelete = { viewModel.requestDeleteTable(table.id) },
                            onOpenSession = table.sessionId?.let { { onOpenSession(it) } },
                        )
                    }
                }
            }
            if (state.pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(BendeySpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BendeyTextButton(
                        text = "Anterior",
                        onClick = { viewModel.setPage(state.page - 1) },
                        enabled = state.page > 0,
                    )
                    Text("Página ${state.page + 1} / ${state.pageCount}", style = MaterialTheme.typography.bodySmall)
                    BendeyTextButton(
                        text = "Siguiente",
                        onClick = { viewModel.setPage(state.page + 1) },
                        enabled = state.page < state.pageCount - 1,
                    )
                }
            }
        }
    }

    if (state.floorsSheetOpen) {
        BendeyBottomSheet(
            onDismissRequest = viewModel::dismissFloorsSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ambientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    BendeyPrimaryButton("Nuevo", viewModel::openCreateFloor, fillWidth = false)
                }
                state.floors.forEach { floor ->
                    BendeyManagementCard(
                        modifier = Modifier.padding(vertical = BendeySpacing.xxs),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(floor.name, fontWeight = FontWeight.SemiBold)
                                Text("Orden ${floor.sortOrder}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            }
                            BendeyIconButton(
                                onClick = { viewModel.openEditFloor(floor) },
                                icon = Icons.Default.Edit,
                                contentDescription = "Editar",
                            )
                            BendeyIconButton(
                                onClick = { viewModel.requestDeleteFloor(floor.id) },
                                icon = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = BendeyColors.Error,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.floorFormOpen) {
        BendeyFormDialog(
            onDismissRequest = viewModel::dismissFloorForm,
            title = if (state.floorForm.id == null) "Nuevo ambiente" else "Editar ambiente",
            confirmText = if (state.saving) "Guardando…" else "Guardar",
            onConfirm = viewModel::saveFloor,
            onDismiss = viewModel::dismissFloorForm,
            confirmEnabled = !state.saving,
            loading = state.saving,
        ) {
            BendeyTextField(
                value = state.floorForm.name,
                onValueChange = { value -> viewModel.updateFloorForm { it.copy(name = value) } },
                label = "Nombre del ambiente *",
            )
            BendeyTextField(
                value = state.floorForm.sortOrder,
                onValueChange = { value -> viewModel.updateFloorForm { it.copy(sortOrder = value.filter { c -> c.isDigit() }) } },
                label = "Orden",
            )
            if (state.floorForm.id != null) {
                BendeyTextButton(
                    text = "Eliminar ambiente",
                    onClick = { viewModel.requestDeleteFloor(state.floorForm.id!!) },
                    textColor = BendeyColors.Error,
                )
            }
        }
    }

    if (state.tableFormOpen) {
        BendeyFormDialog(
            onDismissRequest = viewModel::dismissTableForm,
            title = if (state.tableForm.id == null) "Nueva mesa" else "Editar mesa",
            confirmText = if (state.saving) "Guardando…" else "Guardar",
            onConfirm = viewModel::saveTable,
            onDismiss = viewModel::dismissTableForm,
            confirmEnabled = !state.saving,
            loading = state.saving,
        ) {
            Text("Ambiente", style = MaterialTheme.typography.labelLarge)
            BendeyHorizontalScrollRow(
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                state.floors.forEach { floor ->
                    BendeyFilterChip(
                        selected = state.tableForm.floorId == floor.id,
                        onClick = { viewModel.updateTableForm { it.copy(floorId = floor.id) } },
                        text = floor.name,
                    )
                }
            }
            BendeyTextField(
                value = state.tableForm.name,
                onValueChange = { value -> viewModel.updateTableForm { it.copy(name = value) } },
                label = "Nombre *",
            )
            BendeyTextField(
                value = state.tableForm.capacity,
                onValueChange = { value -> viewModel.updateTableForm { it.copy(capacity = value.filter { c -> c.isDigit() }) } },
                label = "Capacidad *",
            )
        }
    }

    state.deleteTableId?.let { id ->
        val table = state.tables.firstOrNull { it.id == id }
        val blocked = table?.let(viewModel::tableDeleteBlockedReason)
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteTable,
            title = { Text("Eliminar mesa") },
            text = {
                Text(blocked ?: "¿Eliminar la mesa ${table?.name.orEmpty()}?")
            },
            confirmButton = {
                BendeyPrimaryButton(
                    text = "Eliminar",
                    onClick = viewModel::confirmDeleteTable,
                    enabled = blocked == null && !state.saving,
                    fillWidth = false,
                )
            },
            dismissButton = {
                BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissDeleteTable)
            },
        )
    }

    state.deleteFloorId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteFloor,
            title = { Text("Eliminar ambiente") },
            text = { Text("¿Eliminar este ambiente? Debe estar vacío de mesas activas.") },
            confirmButton = {
                BendeyPrimaryButton(
                    text = "Eliminar",
                    onClick = viewModel::confirmDeleteFloor,
                    enabled = !state.saving,
                    fillWidth = false,
                )
            },
            dismissButton = {
                BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissDeleteFloor)
            },
        )
    }
}

@Composable
private fun AdminTableCard(
    table: RestaurantTable,
    floorName: String?,
    deleteBlocked: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSession: (() -> Unit)?,
) {
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(table.name, fontWeight = FontWeight.SemiBold)
                    floorName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    }
                }
                TableContextMenu(table, deleteBlocked, onEdit, onDelete, onOpenSession)
            }
            Text("Cap. ${table.capacity}", style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
            BendeyStatusChip(table.status.label, statusColor(table.status))
            table.totalAmount?.takeIf { it > 0 }?.let {
                Text("Consumo: S/ ${"%.2f".format(it)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AdminTableListRow(
    table: RestaurantTable,
    floorName: String?,
    deleteBlocked: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSession: (() -> Unit)?,
) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(table.name, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(floorName, "Cap. ${table.capacity}", table.status.label).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            TableContextMenu(table, deleteBlocked, onEdit, onDelete, onOpenSession)
        }
    }
}

@Composable
private fun TableContextMenu(
    table: RestaurantTable,
    deleteBlocked: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSession: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    BendeyIconButton(
        onClick = { open = true },
        icon = Icons.Default.MoreVert,
        contentDescription = "Menú",
    )
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(text = { Text("Editar") }, onClick = { open = false; onEdit() })
        onOpenSession?.let { go ->
            DropdownMenuItem(text = { Text("Ir a mesa") }, onClick = { open = false; go() })
        }
        DropdownMenuItem(
            text = { Text("Eliminar") },
            onClick = { open = false; onDelete() },
            enabled = deleteBlocked == null,
        )
    }
}

private fun statusColor(status: TableStatus) = when (status) {
    TableStatus.LIBRE -> BendeyColors.Success
    TableStatus.OCUPADA -> BendeyColors.Primary
    TableStatus.RESERVADA -> BendeyColors.Warning
    TableStatus.EN_CONSUMO -> BendeyColors.Error
}
