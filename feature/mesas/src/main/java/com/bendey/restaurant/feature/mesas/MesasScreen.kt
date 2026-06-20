package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyTableCard
import com.bendey.restaurant.core.designsystem.components.BendeyTableStatsRow
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenSession: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MesasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    LaunchedEffect(state.openSessionTarget) {
        state.openSessionTarget?.let { sessionId ->
            onOpenSession(sessionId)
            viewModel.consumeOpenSessionTarget()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Mapa de mesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedFloorId == null,
                    onClick = { viewModel.selectFloor(null) },
                    label = { Text("Todas") },
                )
                state.floors.forEach { floor ->
                    FilterChip(
                        selected = state.selectedFloorId == floor.id,
                        onClick = { viewModel.selectFloor(floor.id) },
                        label = { Text(floor.name) },
                    )
                }
            }
            BendeyTableStatsRow(
                libre = state.stats.libre,
                ocupada = state.stats.ocupada,
                reservada = state.stats.reservada,
                enConsumo = state.stats.enConsumo,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Buscar mesa",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val columns = BendeyTabletTokens.tableGridColumns(maxWidth)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.filteredTables, key = { it.id }) { table ->
                        BendeyTableCard(
                            table = table,
                            onClick = { viewModel.onTableClick(table) },
                        )
                    }
                }
            }
            state.error?.let {
                Text(
                    text = it,
                    color = BendeyColors.Error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    state.openTableTarget?.let { table ->
        AlertDialog(
            onDismissRequest = viewModel::dismissOpenDialog,
            title = { Text("Abrir ${table.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BendeyTextField(
                        value = state.openForm.guests.toString(),
                        onValueChange = { value ->
                            val guests = value.filter { it.isDigit() }.toIntOrNull() ?: 1
                            viewModel.updateOpenForm { it.copy(guests = guests) }
                        },
                        label = "Comensales",
                    )
                    BendeyTextField(
                        value = state.openForm.notes,
                        onValueChange = { notes ->
                            viewModel.updateOpenForm { it.copy(notes = notes) }
                        },
                        label = "Notas (opcional)",
                        singleLine = false,
                    )
                    if (state.staff.isNotEmpty()) {
                        Text("Mozo", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.openForm.staffId == null,
                                onClick = {
                                    viewModel.updateOpenForm { it.copy(staffId = null) }
                                },
                                label = { Text("Auto") },
                            )
                            state.staff.take(4).forEach { staff ->
                                FilterChip(
                                    selected = state.openForm.staffId == staff.id,
                                    onClick = {
                                        viewModel.updateOpenForm { it.copy(staffId = staff.id) }
                                    },
                                    label = { Text(staff.displayName) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                BendeyPrimaryButton(
                    text = if (state.opening) "Abriendo…" else "Abrir mesa",
                    onClick = viewModel::confirmOpenTable,
                    enabled = !state.opening,
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOpenDialog) {
                    Text("Cancelar")
                }
            },
        )
    }
}
