package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasAdminScreen(
    onOpenSession: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MesasAdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = "Configurar mesas",
            subtitle = "${state.filteredTables.size} mesas · ${state.floors.size} ambientes",
            actions = {
                IconButton(onClick = viewModel::openFloorsSheet) {
                    Icon(Icons.Default.Layers, contentDescription = "Ambientes")
                }
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
                IconButton(onClick = viewModel::openCreateTable) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva mesa")
                }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            FilterChip(
                selected = state.floorFilterId == null,
                onClick = { viewModel.setFloorFilter(null) },
                label = { Text("Todos") },
                colors = BendeyChipDefaults.filterChipColors(),
                shape = BendeyShapeTokens.chip,
                border = null,
            )
            state.floors.forEach { floor ->
                FilterChip(
                    selected = state.floorFilterId == floor.id,
                    onClick = { viewModel.setFloorFilter(floor.id) },
                    label = { Text(floor.name) },
                    colors = BendeyChipDefaults.filterChipColors(),
                    shape = BendeyShapeTokens.chip,
                    border = null,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = state.viewMode == MesasAdminViewMode.GRID,
                onClick = { viewModel.setViewMode(MesasAdminViewMode.GRID) },
                label = { Icon(Icons.Default.GridView, contentDescription = null) },
                colors = BendeyChipDefaults.filterChipColors(),
                shape = BendeyShapeTokens.chip,
                border = null,
            )
            FilterChip(
                selected = state.viewMode == MesasAdminViewMode.LIST,
                onClick = { viewModel.setViewMode(MesasAdminViewMode.LIST) },
                label = { Icon(Icons.Default.List, contentDescription = null) },
                colors = BendeyChipDefaults.filterChipColors(),
                shape = BendeyShapeTokens.chip,
                border = null,
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
                    val columns = BendeyTabletTokens.tableGridColumns(maxWidth)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(BendeySpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        modifier = Modifier.fillMaxSize(),
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
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(BendeySpacing.md),
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
                    TextButton(onClick = { viewModel.setPage(state.page - 1) }, enabled = state.page > 0) {
                        Text("Anterior")
                    }
                    Text("Página ${state.page + 1} / ${state.pageCount}", style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = { viewModel.setPage(state.page + 1) },
                        enabled = state.page < state.pageCount - 1,
                    ) { Text("Siguiente") }
                }
            }
        }
    }

    if (state.floorsSheetOpen) {
        ModalBottomSheet(
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
                            IconButton(onClick = { viewModel.openEditFloor(floor) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.requestDeleteFloor(floor.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
                            }
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
                TextButton(onClick = { viewModel.requestDeleteFloor(state.floorForm.id!!) }) {
                    Text("Eliminar ambiente", color = BendeyColors.Error)
                }
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
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                state.floors.forEach { floor ->
                    FilterChip(
                        selected = state.tableForm.floorId == floor.id,
                        onClick = { viewModel.updateTableForm { it.copy(floorId = floor.id) } },
                        label = { Text(floor.name) },
                        colors = BendeyChipDefaults.filterChipColors(),
                        shape = BendeyShapeTokens.chip,
                        border = null,
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
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteTable,
            containerColor = BendeyColors.Surface,
            tonalElevation = 0.dp,
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
                TextButton(onClick = viewModel::dismissDeleteTable) { Text("Cancelar") }
            },
        )
    }

    state.deleteFloorId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteFloor,
            containerColor = BendeyColors.Surface,
            tonalElevation = 0.dp,
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
                TextButton(onClick = viewModel::dismissDeleteFloor) { Text("Cancelar") }
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
    IconButton(onClick = { open = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
    }
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
