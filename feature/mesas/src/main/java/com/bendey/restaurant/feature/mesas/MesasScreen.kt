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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeyTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyTableCard
import com.bendey.restaurant.core.designsystem.components.BendeyTableStatsRow
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BindSnackMessage
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenSession: (Int) -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MesasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BindSnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

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
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mapa de mesas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = state.selectedFloorId == null,
                    onClick = { viewModel.selectFloor(null) },
                    label = { Text("Todas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BendeyColors.Primary,
                        selectedLabelColor = BendeyColors.OnPrimary,
                    ),
                )
                state.floors.forEach { floor ->
                    FilterChip(
                        selected = state.selectedFloorId == floor.id,
                        onClick = { viewModel.selectFloor(floor.id) },
                        label = { Text(floor.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BendeyColors.Primary,
                            selectedLabelColor = BendeyColors.OnPrimary,
                        ),
                    )
                }
            }
            BendeyTableStatsRow(
                libre = state.stats.libre,
                ocupada = state.stats.ocupada,
                reservada = state.stats.reservada,
                enConsumo = state.stats.enConsumo,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Buscar mesa...",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val columns = BendeyTabletTokens.tableGridColumns(maxWidth)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.floorSections.forEach { section ->
                        val showSectionHeader = state.selectedFloorId == null && state.floors.size > 1
                        if (showSectionHeader) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FloorSectionHeader(
                                    title = section.floorName,
                                    tableCount = section.tables.size,
                                )
                            }
                        }
                        items(section.tables, key = { it.id }) { table ->
                            BendeyTableCard(
                                table = table,
                                onClick = { viewModel.onTableClick(table) },
                            )
                        }
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
        val staffAutoId = -1
        BendeyFormDialog(
            onDismissRequest = viewModel::dismissOpenDialog,
            title = "Abrir ${table.name}",
            confirmText = if (state.opening) "Abriendo…" else "Abrir mesa",
            confirmEnabled = !state.opening,
            loading = state.opening,
            onConfirm = viewModel::confirmOpenTable,
            onDismiss = viewModel::dismissOpenDialog,
        ) {
            if (state.canAssignStaff && state.staff.isNotEmpty()) {
                BendeySearchableSelect(
                    options = listOf(BendeySelectOption(staffAutoId, "Yo (automático)")) +
                        state.staff.map { BendeySelectOption(it.id, it.displayName) },
                    selectedId = state.openForm.staffId ?: staffAutoId,
                    onSelect = { id ->
                        viewModel.updateOpenForm {
                            it.copy(staffId = if (id == staffAutoId) null else id)
                        }
                    },
                    label = "Empleado",
                )
            } else if (state.currentUserName.isNotBlank()) {
                Text("Mozo", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = state.currentUserName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            BendeyTextField(
                value = state.openForm.guestsText,
                onValueChange = { value ->
                    viewModel.updateOpenForm { it.copy(guestsText = value.filter { it.isDigit() }) }
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
        }
    }
}

@Composable
private fun FloorSectionHeader(
    title: String,
    tableCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.OnSurface,
        )
        Text(
            text = "$tableCount mesas",
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
}
