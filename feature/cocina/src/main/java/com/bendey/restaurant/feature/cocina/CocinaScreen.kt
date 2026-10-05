package com.bendey.restaurant.feature.cocina

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.stateColors
import com.bendey.restaurant.core.domain.catalog.preparationAreaDisplayLabel
import com.bendey.restaurant.core.domain.kitchen.KdsColumn
import com.bendey.restaurant.core.domain.kitchen.KdsCopy
import com.bendey.restaurant.core.domain.kitchen.KdsRound
import com.bendey.restaurant.core.domain.kitchen.KdsUrgency
import com.bendey.restaurant.core.domain.kitchen.buildKdsBoard
import com.bendey.restaurant.core.domain.kitchen.buildKdsRounds
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.VoidPinDialog
import com.bendey.restaurant.core.ui.layout.BendeyOrientationPolicy
import kotlinx.coroutines.delay
import com.bendey.restaurant.core.domain.time.PeruDateTime
import java.time.Instant

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Cocina (KDS): 4 columnas NUEVO - PREPARANDO - LISTO - ENTREGADO, una tarjeta por ronda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocinaScreen(
    modifier: Modifier = Modifier,
    viewModel: CocinaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pantalla activa y horizontal SOLO mientras esta pantalla esta en composicion; limpia al salir.
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(view, activity) {
        view.keepScreenOn = true
        activity?.let { BendeyOrientationPolicy.setKdsMode(it, true) }
        onDispose {
            view.keepScreenOn = false
            activity?.let { BendeyOrientationPolicy.setKdsMode(it, false) }
            // Un cambio en su ventana de "Deshacer" se envia ya al salir.
            viewModel.flushPending()
        }
    }

    // Reloj de pantalla: cada 30 s (el tiempo se mide en minutos).
    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val now = remember(nowMs) { Instant.ofEpochMilli(nowMs) }
    val rounds = remember(state.serverItems, state.overrides, state.areaFilter, state.orderTab) {
        buildKdsRounds(state.visibleItems)
    }
    val board = remember(rounds, nowMs) { buildKdsBoard(rounds, now) }

    // Atrasos de cocina (NUEVO/PREPARANDO). En LISTO el atraso es del mozo, no suena.
    val redRounds = remember(board, nowMs) {
        listOf(KdsColumn.NUEVO, KdsColumn.PREPARANDO)
            .flatMap { board[it].orEmpty() }
            .filter { it.urgency(now) == KdsUrgency.ROJO }
    }
    LaunchedEffect(redRounds.map { it.key }.toSet(), nowMs) {
        viewModel.onRedRounds(redRounds.map { it.key }.toSet(), nowMs)
    }

    var activeColumn by remember { mutableStateOf(KdsColumn.NUEVO) }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            KdsHeader(
                state = state,
                nowMs = nowMs,
                redCount = redRounds.size,
                activeCount = board.filterKeys { it != KdsColumn.ENTREGADO }.values.sumOf { it.size },
                onToggleBell = { viewModel.toggleBell(System.currentTimeMillis()) },
                onRefresh = viewModel::refresh,
            )
            KdsFilterRow(
                state = state,
                onArea = viewModel::setAreaFilter,
                onTab = viewModel::setOrderTab,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.xs),
            ) {
                if (maxWidth >= 840.dp) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    ) {
                        KdsColumn.entries.forEach { col ->
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                KdsColumnHeader(col, board[col].orEmpty().size)
                                KdsColumnList(col, board[col].orEmpty(), now, state, viewModel)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        BendeyHorizontalScrollRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = BendeySpacing.xxs),
                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                        ) {
                            KdsColumn.entries.forEach { col ->
                                BendeyFilterChip(
                                    selected = activeColumn == col,
                                    onClick = { activeColumn = col },
                                    text = "${col.label} (${board[col].orEmpty().size})",
                                )
                            }
                        }
                        KdsColumnList(activeColumn, board[activeColumn].orEmpty(), now, state, viewModel)
                    }
                }
            }
            state.pending?.let { pending ->
                KdsUndoBar(
                    text = pending.label,
                    onUndo = viewModel::undo,
                )
            }
            state.error?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(BendeySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = it,
                        color = BendeyColors.ErrorText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::dismissError) { Text("Cerrar") }
                }
            }
        }
    }

    val voidItem = state.voidItem
    VoidPinDialog(
        open = voidItem != null,
        title = "Anular comanda",
        description = "Se elimina el ítem de cocina.",
        itemLabel = voidItem?.let { "${it.productName} ×${kdsFormatQty(it.quantity)}" },
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
private fun KdsHeader(
    state: CocinaUiState,
    nowMs: Long,
    redCount: Int,
    activeCount: Int,
    onToggleBell: () -> Unit,
    onRefresh: () -> Unit,
) {
    val snoozed = nowMs < state.snoozedUntilMs
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Cocina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$activeCount rondas en curso",
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Campana siempre visible: activa = alerta de atraso suena; silenciada 5 min = 🔕.
                TextButton(
                    onClick = onToggleBell,
                    modifier = Modifier.heightIn(min = BendeySpacing.touchPrimary).semantics {
                        contentDescription = if (snoozed) KdsCopy.BELL_SNOOZED else KdsCopy.BELL_ON
                    },
                ) {
                    Icon(
                        imageVector = if (snoozed) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = if (snoozed) "Atraso silenciado hasta ${hhmm(state.snoozedUntilMs)}" else "Alerta de atraso",
                        modifier = Modifier.padding(start = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = BendeySpacing.touchPrimary)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", modifier = Modifier.size(24.dp))
                }
            }
        }
        if (redCount > 0) {
            Surface(
                color = BendeyColors.StateAtrasado.tint,
                modifier = Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.xs),
                shape = BendeyShapeTokens.xs,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BendeyColors.StateAtrasado.fill)
                    Text(
                        text = "$redCount ${KdsCopy.LATE.lowercase()}${if (redCount == 1) "" else "s"}",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.StateAtrasado.onTint,
                    )
                    TextButton(onClick = onToggleBell, modifier = Modifier.heightIn(min = BendeySpacing.touchPrimary)) {
                        Text(if (snoozed) "Reactivar alerta" else "Silenciar 5 min", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun hhmm(ms: Long): String = PeruDateTime.formatTime(ms)

/** UNA sola fila: areas (recordada por dispositivo) y tipo de pedido. */
@Composable
private fun KdsFilterRow(
    state: CocinaUiState,
    onArea: (String) -> Unit,
    onTab: (CocinaOrderTab) -> Unit,
) {
    BendeyHorizontalScrollRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        if (state.availableAreas.isNotEmpty()) {
            Text(KdsCopy.FILTER_LABEL, fontSize = 16.sp, color = BendeyColors.OnSurfaceVariant)
            BendeyFilterChip(
                selected = state.areaFilter == "all",
                onClick = { onArea("all") },
                text = KdsCopy.FILTER_ALL,
            )
            state.availableAreas.forEach { area ->
                BendeyFilterChip(
                    selected = state.areaFilter == area,
                    onClick = { onArea(area) },
                    text = preparationAreaDisplayLabel(area),
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .heightIn(min = 28.dp)
                    .background(BendeyColors.OnSurfaceVariant.copy(alpha = 0.4f)),
            )
        }
        CocinaOrderTab.entries.forEach { tab ->
            BendeyFilterChip(
                selected = state.orderTab == tab,
                onClick = { onTab(tab) },
                text = tab.label,
            )
        }
    }
}

@Composable
private fun KdsColumnHeader(column: KdsColumn, count: Int) {
    val tone = column.status.stateColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tone.tint, BendeyShapeTokens.xs)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(column.label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = tone.onTint)
        Box(
            modifier = Modifier.size(36.dp).background(tone.fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (column == KdsColumn.ENTREGADO) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun KdsColumnList(
    column: KdsColumn,
    rounds: List<KdsRound>,
    now: Instant,
    state: CocinaUiState,
    viewModel: CocinaViewModel,
) {
    if (rounds.isEmpty()) {
        Text(
            text = when (column) {
                KdsColumn.NUEVO -> "${KdsCopy.EMPTY_TITLE}: ${KdsCopy.EMPTY_PENDIENTE}"
                else -> "${KdsCopy.EMPTY_TITLE}: ${KdsCopy.EMPTY_OTHER}"
            },
            modifier = Modifier.fillMaxWidth().padding(top = BendeySpacing.lg),
            color = BendeyColors.OnSurfaceVariant,
            fontSize = 18.sp,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = BendeySpacing.xs, bottom = BendeySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        items(rounds, key = { it.key }) { round ->
            KdsTicket(
                round = round,
                now = now,
                canAdvance = state.canManageKitchenComandas && column != KdsColumn.ENTREGADO,
                canVoid = state.canAnularComanda,
                onAction = { viewModel.advanceRound(round, it) },
                onVoidItem = viewModel::openVoidItem,
            )
        }
    }
}

/** Barra "Deshacer" (5 s): no es un toast por toque, solo existe mientras se puede deshacer. */
@Composable
private fun KdsUndoBar(text: String, onUndo: () -> Unit) {
    Surface(
        color = Color(0xFF263238),
        modifier = Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
        shape = BendeyShapeTokens.md,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 64.dp)) {
                Text(KdsCopy.UNDO_BUTTON, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF81D4FA))
            }
        }
    }
}
