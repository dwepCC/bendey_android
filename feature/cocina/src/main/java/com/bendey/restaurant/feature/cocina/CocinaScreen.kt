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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton

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
    val filtered = remember(state.items, activeStatus) { state.itemsFor(activeStatus) }

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
                if (filtered.isEmpty() && !state.loading) {
                    Text(
                        text = "No hay ítems en «${activeStatus.label}».",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        color = BendeyColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            KitchenCard(
                                item = item,
                                accent = activeStatus.accentColor(),
                                advancing = state.updatingId == item.id,
                                canAdvance = activeStatus != ComandaStatus.ENTREGADA,
                                onAdvance = { viewModel.advanceItem(item) },
                            )
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
}

@Composable
private fun KitchenCard(
    item: KitchenItem,
    accent: androidx.compose.ui.graphics.Color,
    advancing: Boolean,
    canAdvance: Boolean,
    onAdvance: () -> Unit,
) {
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
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = itemTitle(item),
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatQty(item.quantity),
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
