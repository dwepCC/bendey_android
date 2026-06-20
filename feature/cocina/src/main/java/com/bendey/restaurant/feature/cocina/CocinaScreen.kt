package com.bendey.restaurant.feature.cocina

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.products.PreparationArea
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.VoidPinDialog

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
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CocinaViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            if (state.viewMode == CocinaViewMode.ORDERS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CocinaOrderTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.orderTab == tab,
                            onClick = { viewModel.setOrderTab(tab) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
            if (state.availableAreas.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = state.areaFilter == "all",
                        onClick = { viewModel.setAreaFilter("all") },
                        label = { Text("Todas áreas") },
                    )
                    state.availableAreas.forEach { area ->
                        FilterChip(
                            selected = state.areaFilter == area,
                            onClick = { viewModel.setAreaFilter(area) },
                            label = { Text(PreparationArea.fromApi(area).label) },
                        )
                    }
                }
            }
            if (state.viewMode == CocinaViewMode.ORDERS && state.availableTables.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = state.tableFilter == "all",
                        onClick = { viewModel.setTableFilter("all") },
                        label = { Text("Todas mesas") },
                    )
                    state.availableTables.forEach { table ->
                        FilterChip(
                            selected = state.tableFilter == table,
                            onClick = { viewModel.setTableFilter(table) },
                            label = { Text(table) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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
                            .padding(top = 48.dp),
                        color = BendeyColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else when (state.viewMode) {
                    CocinaViewMode.ITEMS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(filtered, key = { it.kdsKey }) { item ->
                                KitchenCard(
                                    item = item,
                                    accent = activeStatus.accentColor(),
                                    advancing = state.updatingId == item.id,
                                    canAdvance = activeStatus != ComandaStatus.ENTREGADA,
                                    onAdvance = { viewModel.advanceItem(item) },
                                    onVoid = { viewModel.openVoidItem(item) },
                                )
                            }
                        }
                    }
                    CocinaViewMode.ORDERS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(groups, key = { it.key }) { group ->
                                KitchenOrderCard(
                                    group = group,
                                    accent = activeStatus.accentColor(),
                                    updatingId = state.updatingId,
                                    canAdvance = activeStatus != ComandaStatus.ENTREGADA,
                                    onAdvance = viewModel::advanceItem,
                                    onVoid = viewModel::openVoidItem,
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
                    modifier = Modifier.padding(8.dp),
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
    onAdvance: (KitchenItem) -> Unit,
    onVoid: (KitchenItem) -> Unit,
) {
    val elapsed = rememberKitchenElapsed(group.items.firstOrNull()?.kitchenOpenedAt())
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            group.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                            modifier = Modifier.heightIn(min = 32.dp).padding(end = 4.dp),
                        ) {
                            Text(if (updatingId == item.id) "…" else "Avanzar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { onVoid(item) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Anular", tint = BendeyColors.Error)
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
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item.preparationArea?.takeIf { it.isNotBlank() }?.let { area ->
            BendeyStatusChip(
                label = PreparationArea.fromApi(area).label,
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
    onAdvance: () -> Unit,
    onVoid: () -> Unit,
) {
    val elapsed = rememberKitchenElapsed(item.kitchenOpenedAt())
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                    IconButton(onClick = onVoid, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Anular", tint = BendeyColors.Error)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
