package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChipVariant
import com.bendey.restaurant.core.designsystem.components.BendeyTableCard
import com.bendey.restaurant.core.designsystem.components.BendeyTableStatsRow
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasScreen(
    onOpenSession: (Int) -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MesasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    val tableCount = state.floorSections.sumOf { it.tables.size }

    BendeySnackMessage(
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

    BendeyListScreenLayout(
        modifier = modifier.fillMaxSize(),
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = BendeySpacing.md,
                        end = BendeySpacing.xs,
                        top = BendeySpacing.sm,
                        bottom = BendeySpacing.xxs,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mapa de mesas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                BendeyIconButton(
                    onClick = viewModel::refresh,
                    icon = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                )
            }
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.sm,
                    vertical = BendeySpacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                BendeyFilterChip(
                    selected = state.selectedFloorId == null,
                    onClick = { viewModel.selectFloor(null) },
                    text = "Todas",
                    variant = BendeyFilterChipVariant.Pos,
                )
                state.floors.forEach { floor ->
                    BendeyFilterChip(
                        selected = state.selectedFloorId == floor.id,
                        onClick = { viewModel.selectFloor(floor.id) },
                        text = floor.name,
                        variant = BendeyFilterChipVariant.Pos,
                    )
                }
            }
            BendeyTableStatsRow(
                libre = state.stats.libre,
                ocupada = state.stats.ocupada,
                reservada = state.stats.reservada,
                enConsumo = state.stats.enConsumo,
                modifier = Modifier.padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
            )
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Buscar mesa...",
                modifier = Modifier.padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
            )
            state.error?.let {
                Text(
                    text = it,
                    color = BendeyColors.Error,
                    modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
                )
            }
        },
    ) { innerModifier ->
        BoxWithConstraints(modifier = innerModifier) {
            val profile = rememberBendeyAdaptiveProfile()
            val columns = BendeyTabletTokens.tableGridColumns(profile)
            val gridState = rememberLazyGridState()
            BendeyLazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(
                    start = BendeySpacing.sm,
                    end = BendeySpacing.sm,
                    top = BendeySpacing.xxs,
                    bottom = BendeySpacing.sm + bottomScrollPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                if (tableCount == 0 && !state.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No hay mesas para mostrar",
                            color = BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.padding(BendeySpacing.md),
                        )
                    }
                }
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
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "$tableCount mesas",
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
}
