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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.restaurant.Floor
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens

@Composable
fun MesasAdminScreen(
    modifier: Modifier = Modifier,
    viewModel: MesasAdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = "Configurar mesas",
            subtitle = "${state.filteredTables.size} mesas · ${state.floors.size} ambientes",
            actions = {
                IconButton(onClick = viewModel::openCreateFloor) {
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
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = "Buscar mesa",
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
                selected = state.floorFilterId == null,
                onClick = { viewModel.setFloorFilter(null) },
                label = { Text("Todos") },
            )
            state.floors.forEach { floor ->
                FilterChip(
                    selected = state.floorFilterId == floor.id,
                    onClick = { viewModel.setFloorFilter(floor.id) },
                    label = { Text(floor.name) },
                )
            }
        }
        if (state.filteredTables.isEmpty() && !state.loading) {
            Text(
                "Sin mesas",
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = BendeyTabletTokens.tableGridColumns(maxWidth)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.filteredTables, key = { it.id }) { table ->
                        AdminTableCard(
                            table = table,
                            floorName = state.floors.firstOrNull { it.id == table.floorId }?.name ?: table.floorName,
                            deleteBlocked = viewModel.tableDeleteBlockedReason(table),
                            onEdit = { viewModel.openEditTable(table) },
                            onDelete = { viewModel.requestDeleteTable(table.id) },
                        )
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.floors.forEach { floor ->
                    FilterChip(
                        selected = state.tableForm.floorId == floor.id,
                        onClick = { viewModel.updateTableForm { it.copy(floorId = floor.id) } },
                        label = { Text(floor.name) },
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
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(table.name, fontWeight = FontWeight.SemiBold)
            floorName?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            Text(
                "Cap. ${table.capacity} · ${table.status.label}",
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete, enabled = deleteBlocked == null) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
                }
            }
        }
    }
}
