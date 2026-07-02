package com.bendey.restaurant.feature.cocina

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.catalog.preparationAreaDisplayLabel
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.VoidPinDialog
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding

private val STATUS_FILTERS = listOf(
    ComandaStatus.PENDIENTE,
    ComandaStatus.PREPARACION,
    ComandaStatus.LISTA,
    ComandaStatus.ENTREGADA,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocinaScreen(
    modifier: Modifier = Modifier,
    viewModel: CocinaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeStatus by remember { mutableStateOf(ComandaStatus.PENDIENTE) }
    val filtered = remember(state, activeStatus) { state.filteredItems(activeStatus) }
    val groups = remember(state, activeStatus) { state.orderGroups(activeStatus) }
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Comandas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.items.size} ítems · ${state.count(ComandaStatus.PENDIENTE)} pendientes",
                        style = MaterialTheme.typography.labelMedium,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                BendeyIconButton(
                    onClick = viewModel::refresh,
                    icon = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                )
            }
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.xs,
                    vertical = BendeySpacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                CocinaViewMode.entries.forEach { mode ->
                    BendeyFilterChip(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        text = mode.label,
                    )
                }
            }
            if (state.viewMode == CocinaViewMode.ORDERS) {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = BendeySpacing.xs,
                        vertical = BendeySpacing.xxs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    CocinaOrderTab.entries.forEach { tab ->
                        BendeyFilterChip(
                            selected = state.orderTab == tab,
                            onClick = { viewModel.setOrderTab(tab) },
                            text = tab.label,
                        )
                    }
                }
            }
            if (state.availableAreas.isNotEmpty()) {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = BendeySpacing.xs,
                        vertical = BendeySpacing.xxs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    BendeyFilterChip(
                        selected = state.areaFilter == "all",
                        onClick = { viewModel.setAreaFilter("all") },
                        text = "Todas áreas",
                    )
                    state.availableAreas.forEach { area ->
                        BendeyFilterChip(
                            selected = state.areaFilter == area,
                            onClick = { viewModel.setAreaFilter(area) },
                            text = preparationAreaDisplayLabel(area),
                        )
                    }
                }
            }
            if (state.viewMode == CocinaViewMode.ORDERS && state.availableTables.size > 1) {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = BendeySpacing.xs,
                        vertical = BendeySpacing.xxs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    BendeyFilterChip(
                        selected = state.tableFilter == "all",
                        onClick = { viewModel.setTableFilter("all") },
                        text = "Todas mesas",
                    )
                    state.availableTables.forEach { table ->
                        BendeyFilterChip(
                            selected = state.tableFilter == table,
                            onClick = { viewModel.setTableFilter(table) },
                            text = table,
                        )
                    }
                }
            }
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.xs,
                    vertical = BendeySpacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                STATUS_FILTERS.forEach { status ->
                    val accent = status.accentColor()
                    val selected = activeStatus == status
                    FilterChip(
                        selected = selected,
                        onClick = { activeStatus = status },
                        label = {
                            Text(
                                "${status.label} (${state.count(status)})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = BendeyColors.OnPrimary,
                            containerColor = accent.copy(alpha = 0.12f),
                            labelColor = accent,
                        ),
                        shape = BendeyShapeTokens.chip,
                        border = null,
                    )
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.xs),
            ) {
                val columns = when {
                    maxWidth >= 1200.dp -> 4
                    maxWidth >= 840.dp -> 3
                    maxWidth >= 600.dp -> 2
                    else -> 1
                }
                val isEmpty = if (state.viewMode == CocinaViewMode.ITEMS) filtered.isEmpty() else groups.isEmpty()
                if (isEmpty && !state.loading) {
                    Text(
                        text = "No hay ítems en «${activeStatus.label}».",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BendeySpacing.xl),
                        color = BendeyColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else when (state.viewMode) {
                    CocinaViewMode.ITEMS -> {
                        BendeyLazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyGridState(),
                            contentPadding = PaddingValues(bottom = BendeySpacing.sm + bottomScrollPadding),
                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                        ) {
                            items(filtered, key = { it.kdsKey }) { item ->
                                KitchenCard(
                                    item = item,
                                    accent = activeStatus.accentColor(),
                                    advancing = state.updatingId == item.id,
                                    canAdvance = activeStatus != ComandaStatus.ENTREGADA && state.canManageKitchenComandas,
                                    canVoid = state.canAnularComanda,
                                    onAdvance = { viewModel.advanceItem(item) },
                                    onVoid = { viewModel.openVoidItem(item) },
                                )
                            }
                        }
                    }
                    CocinaViewMode.ORDERS -> {
                        BendeyLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(bottom = BendeySpacing.sm + bottomScrollPadding),
                            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        ) {
                            items(groups, key = { it.key }) { group ->
                                KitchenOrderCard(
                                    group = group,
                                    accent = activeStatus.accentColor(),
                                    updatingId = state.updatingId,
                                    canAdvance = activeStatus != ComandaStatus.ENTREGADA && state.canManageKitchenComandas,
                                    canVoid = state.canAnularComanda,
                                    onAdvance = viewModel::advanceItem,
                                    onVoid = viewModel::openVoidItem,
                                    onMarkRoundReady = { viewModel.markRoundReady(it) },
                                )
                            }
                        }
                    }
                }
            }
            state.error?.let {
                Text(
                    text = it,
                    color = BendeyColors.Error,
                    modifier = Modifier.padding(BendeySpacing.xs),
                )
            }
        }
    }

    val voidItem = state.voidItem
    VoidPinDialog(
        open = voidItem != null,
        title = "Anular comanda",
        description = "Se elimina el ítem de cocina.",
        itemLabel = voidItem?.let { "${it.productName} ×${formatQty(it.quantity)}" },
        reason = state.voidReason,
        pin = state.voidPin,
        loading = state.voidSubmitting,
        error = if (voidItem != null) state.error else null,
        onReasonChange = viewModel::setVoidReason,
        onPinChange = viewModel::setVoidPin,
        onDismiss = viewModel::dismissVoidDialog,
        onConfirm = viewModel::confirmVoid,
    )
}

@Composable
private fun KitchenOrderCard(
    group: KitchenOrderGroup,
    accent: androidx.compose.ui.graphics.Color,
    updatingId: Int?,
    canAdvance: Boolean,
    canVoid: Boolean,
    onAdvance: (KitchenItem) -> Unit,
    onVoid: (KitchenItem) -> Unit,
    onMarkRoundReady: (List<KitchenItem>) -> Unit,
) {
    val hasPendingRound = group.items.any {
        it.status == ComandaStatus.PENDIENTE || it.status == ComandaStatus.PREPARACION
    }
    val elapsed = rememberKitchenElapsed(group.items.firstOrNull()?.kitchenOpenedAt())
    BendeyManagementCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    group.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    }
                    if (elapsed.isNotBlank()) {
                        Text(
                            "Abierto hace $elapsed",
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.Warning,
                        )
                    }
                }
                BendeyStatusChip(
                    label = orderTypeLabel(group.orderType),
                    accentColor = BendeyColors.Primary,
                )
            }
            if (canAdvance && hasPendingRound) {
                OutlinedButton(
                    onClick = { onMarkRoundReady(group.items) },
                    enabled = updatingId != -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BendeySpacing.xxs),
                ) {
                    Text(if (updatingId == -1) "Marcando…" else "Marcar ronda lista")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
            group.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BendeySpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${formatQty(item.kdsQuantity)}× ${item.kdsName}",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (canAdvance) {
                        OutlinedButton(
                            onClick = { onAdvance(item) },
                            enabled = updatingId != item.id,
                            modifier = Modifier.heightIn(min = 32.dp).padding(end = BendeySpacing.xxs),
                        ) {
                            Text(if (updatingId == item.id) "…" else "Avanzar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (canVoid) {
                        BendeyIconButton(
                            onClick = { onVoid(item) },
                            modifier = Modifier.size(32.dp),
                            icon = Icons.Default.Delete,
                            contentDescription = "Anular",
                            tint = BendeyColors.Error,
                        )
                    }
                }
                item.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.Warning)
                }
                item.modifierLines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KitchenItemMetaRow(item: KitchenItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        item.preparationArea?.takeIf { it.isNotBlank() }?.let { area ->
            BendeyStatusChip(
                label = preparationAreaDisplayLabel(area),
                accentColor = BendeyColors.AccentTeal,
            )
        }
        if (item.isComboComponent && item.comboName != null) {
            BendeyStatusChip(label = "Combo", accentColor = BendeyColors.AccentPurple)
        }
    }
}

@Composable
private fun KitchenCard(
    item: KitchenItem,
    accent: androidx.compose.ui.graphics.Color,
    advancing: Boolean,
    canAdvance: Boolean,
    canVoid: Boolean,
    onAdvance: () -> Unit,
    onVoid: () -> Unit,
) {
    val elapsed = rememberKitchenElapsed(item.kitchenOpenedAt())
    BendeyManagementCard(contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(BendeySpacing.xxs)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.kdsName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (canVoid) {
                        BendeyIconButton(
                            onClick = onVoid,
                            modifier = Modifier.size(32.dp),
                            icon = Icons.Default.Delete,
                            contentDescription = "Anular",
                            tint = BendeyColors.Error,
                        )
                    }
                }
                Text(
                    text = itemTitle(item),
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (elapsed.isNotBlank()) {
                    Text(
                        "Abierto hace $elapsed",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.Warning,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (item.isComboComponent && item.comboName != null) {
                    Text(
                        "↳ ${item.comboName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.AccentPurple,
                    )
                }
                KitchenItemMetaRow(item)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    Text(
                        text = formatQty(item.kdsQuantity),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = accent,
                    )
                    Text("unidades", style = MaterialTheme.typography.labelLarge, color = BendeyColors.OnSurfaceVariant)
                }
                item.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BendeyColors.Warning,
                    )
                }
                item.modifierLines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                if (canAdvance) {
                    BendeyPrimaryButton(
                        text = if (advancing) "Avanzando…" else "Avanzar",
                        onClick = onAdvance,
                        enabled = !advancing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun itemTitle(item: KitchenItem): String {
    val parts = mutableListOf<String>()
    item.orderCode?.let { parts += it }
    item.orderNumber?.let { parts += "Comanda #$it" }
    item.tableName?.let { parts += it }
    item.customerName?.takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(" · ").ifBlank { item.orderCode ?: "Pedido" }
}

private fun formatQty(qty: Double): String =
    if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
