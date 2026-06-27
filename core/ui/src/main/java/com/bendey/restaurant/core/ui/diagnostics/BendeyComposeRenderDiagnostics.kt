package com.bendey.restaurant.core.ui.diagnostics

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max
import kotlin.math.min

private const val LOG_TAG = "BendeyRenderDiag"

/** Padre actual en la jerarquía de sondas (para árbol Compose). */
val LocalBendeyProbeParent = compositionLocalOf<String?> { null }

data class BendeyLayoutProbeSnapshot(
    val tag: String,
    val parentTag: String?,
    val widthPx: Int,
    val heightPx: Int,
    val leftPx: Float,
    val topPx: Float,
    val compositionOrder: Int,
    val isAttached: Boolean,
) {
    val areaPx: Long get() = widthPx.toLong() * heightPx.toLong()
    val boundsInRoot: Rect get() = Rect(leftPx, topPx, leftPx + widthPx, topPx + heightPx)
}

data class BendeyLazyListRenderSnapshot(
    val screen: String,
    val stateItemCount: Int,
    val totalItemsCount: Int,
    val visibleItemsCount: Int,
    val firstVisibleIndex: Int?,
    val lastVisibleIndex: Int?,
    val viewportWidthPx: Int,
    val viewportHeightPx: Int,
    val viewportStartOffset: Int,
    val viewportEndOffset: Int,
    val listWidthPx: Int,
    val listHeightPx: Int,
    val parentTag: String?,
)

data class BendeyLazyGridRenderSnapshot(
    val screen: String,
    val stateItemCount: Int,
    val totalItemsCount: Int,
    val visibleItemsCount: Int,
    val firstVisibleIndex: Int?,
    val lastVisibleIndex: Int?,
    val viewportWidthPx: Int,
    val viewportHeightPx: Int,
    val gridWidthPx: Int,
    val gridHeightPx: Int,
    val parentTag: String?,
)

/** Registro global de mediciones en el dispositivo (solo debug). */
object BendeyLayoutProbeRegistry {
    private val probes = mutableStateMapOf<String, BendeyLayoutProbeSnapshot>()
    private var compositionCounter = 0

    fun nextCompositionOrder(): Int = compositionCounter++

    fun update(snapshot: BendeyLayoutProbeSnapshot) {
        if (BendeyUiDiagnostics.isActive) {
            probes[snapshot.tag] = snapshot
        }
    }

    fun remove(tag: String) {
        probes.remove(tag)
    }

    fun snapshots(): List<BendeyLayoutProbeSnapshot> =
        probes.values.sortedBy { it.compositionOrder }

    fun logTree(screen: String) {
        if (!BendeyUiDiagnostics.isActive) return
        val lines = snapshots().joinToString("\n") { snap ->
            "  probe tag=${snap.tag} parent=${snap.parentTag} size=${snap.widthPx}x${snap.heightPx} " +
                "pos=(${snap.leftPx.toInt()},${snap.topPx.toInt()}) order=${snap.compositionOrder}"
        }
        Log.d(LOG_TAG, "[$screen] PROBE_TREE\n$lines")
    }
}

fun Modifier.bendeyLayoutProbe(
    tag: String,
    parentTag: String? = null,
    probeColor: Color? = null,
): Modifier {
    if (!BendeyUiDiagnostics.isActive) return this

    val order = BendeyLayoutProbeRegistry.nextCompositionOrder()
    val color = probeColor ?: probeColorForTag(tag)

    return this
        .semantics { testTag = "bendey_probe_$tag" }
        .then(
            if (BendeyUiDiagnostics.showProbeBounds) {
                Modifier
                    .border(1.5.dp, color)
                    .background(color.copy(alpha = 0.07f))
            } else {
                Modifier
            },
        )
        .onGloballyPositioned { coordinates ->
            reportProbe(tag, parentTag, order, coordinates)
        }
}

private fun reportProbe(
    tag: String,
    parentTag: String?,
    order: Int,
    coordinates: LayoutCoordinates,
) {
    if (!coordinates.isAttached) {
        BendeyLayoutProbeRegistry.remove(tag)
        return
    }
    val bounds = coordinates.boundsInRoot()
    val snapshot = BendeyLayoutProbeSnapshot(
        tag = tag,
        parentTag = parentTag,
        widthPx = bounds.width.toInt().coerceAtLeast(0),
        heightPx = bounds.height.toInt().coerceAtLeast(0),
        leftPx = bounds.left,
        topPx = bounds.top,
        compositionOrder = order,
        isAttached = true,
    )
    BendeyLayoutProbeRegistry.update(snapshot)
}

@Composable
fun BendeyProbeScope(
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val parent = LocalBendeyProbeParent.current
    CompositionLocalProvider(LocalBendeyProbeParent provides tag) {
        Box(modifier = modifier.bendeyLayoutProbe(tag, parent)) {
            content()
        }
    }
}

fun Modifier.trackLazyListRender(
    screen: String,
    listTag: String,
    stateItemCount: Int,
    listState: LazyListState,
    parentTag: String? = null,
): Modifier = composed {
    if (!BendeyUiDiagnostics.isActive) return@composed this

    var listWidthPx by remember { mutableIntStateOf(0) }
    var listHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState, stateItemCount, listWidthPx, listHeightPx, listTag) {
        snapshotFlow {
            listState.layoutInfo to listState.layoutInfo.visibleItemsInfo.size
        }
            .distinctUntilChanged()
            .collect { (info, _) ->
                val visible = info.visibleItemsInfo
                val snap = BendeyLazyListRenderSnapshot(
                    screen = screen,
                    stateItemCount = stateItemCount,
                    totalItemsCount = info.totalItemsCount,
                    visibleItemsCount = visible.size,
                    firstVisibleIndex = visible.firstOrNull()?.index,
                    lastVisibleIndex = visible.lastOrNull()?.index,
                    viewportWidthPx = info.viewportSize.width,
                    viewportHeightPx = info.viewportSize.height,
                    viewportStartOffset = info.viewportStartOffset,
                    viewportEndOffset = info.viewportEndOffset,
                    listWidthPx = listWidthPx,
                    listHeightPx = listHeightPx,
                    parentTag = parentTag,
                )
                BendeyUiDiagnostics.lastLazyListSnapshot = snap
                BendeyUiDiagnostics.log(
                    screen,
                    "LAZY_LIST[$listTag] state=$stateItemCount total=${snap.totalItemsCount} " +
                        "visible=${snap.visibleItemsCount} first=${snap.firstVisibleIndex} " +
                        "last=${snap.lastVisibleIndex} viewport=${snap.viewportWidthPx}x${snap.viewportHeightPx} " +
                        "listNode=${snap.listWidthPx}x${snap.listHeightPx} parent=$parentTag",
                )
            }
    }

    this
        .semantics { testTag = "bendey_lazy_$listTag" }
        .bendeyLayoutProbe(listTag, parentTag)
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                val b = coords.boundsInRoot()
                listWidthPx = b.width.toInt()
                listHeightPx = b.height.toInt()
            }
        }
}

fun Modifier.trackLazyGridRender(
    screen: String,
    gridTag: String,
    stateItemCount: Int,
    gridState: LazyGridState,
    parentTag: String? = null,
): Modifier = composed {
    if (!BendeyUiDiagnostics.isActive) return@composed this

    var gridWidthPx by remember { mutableIntStateOf(0) }
    var gridHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(gridState, stateItemCount, gridWidthPx, gridHeightPx, gridTag) {
        snapshotFlow {
            gridState.layoutInfo to gridState.layoutInfo.visibleItemsInfo.size
        }
            .distinctUntilChanged()
            .collect { (info, _) ->
                val visible = info.visibleItemsInfo
                val snap = BendeyLazyGridRenderSnapshot(
                    screen = screen,
                    stateItemCount = stateItemCount,
                    totalItemsCount = info.totalItemsCount,
                    visibleItemsCount = visible.size,
                    firstVisibleIndex = visible.firstOrNull()?.index,
                    lastVisibleIndex = visible.lastOrNull()?.index,
                    viewportWidthPx = info.viewportSize.width,
                    viewportHeightPx = info.viewportSize.height,
                    gridWidthPx = gridWidthPx,
                    gridHeightPx = gridHeightPx,
                    parentTag = parentTag,
                )
                BendeyUiDiagnostics.lastLazyGridSnapshot = snap
                BendeyUiDiagnostics.log(
                    screen,
                    "LAZY_GRID[$gridTag] state=$stateItemCount total=${snap.totalItemsCount} " +
                        "visible=${snap.visibleItemsCount} first=${snap.firstVisibleIndex} " +
                        "last=${snap.lastVisibleIndex} viewport=${snap.viewportWidthPx}x${snap.viewportHeightPx} " +
                        "gridNode=${snap.gridWidthPx}x${snap.gridHeightPx} parent=$parentTag",
                )
            }
    }

    this
        .semantics { testTag = "bendey_grid_$gridTag" }
        .bendeyLayoutProbe(gridTag, parentTag)
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                val b = coords.boundsInRoot()
                gridWidthPx = b.width.toInt()
                gridHeightPx = b.height.toInt()
            }
        }
}

/**
 * Panel flotante con evidencia de renderizado.
 * Buscar en Layout Inspector: `bendey_render_diagnostics_panel` o sondas `bendey_probe_*`.
 */
@Composable
fun BendeyRenderDiagnosticsPanel(
    screen: String,
    stateItemCount: Int,
    listType: BendeyDiagnosticListType = BendeyDiagnosticListType.None,
    modifier: Modifier = Modifier,
) {
    if (!BendeyUiDiagnostics.isActive) return

    val density = LocalDensity.current
    val probes = BendeyLayoutProbeRegistry.snapshots()
    val lazyList = BendeyUiDiagnostics.lastLazyListSnapshot?.takeIf { it.screen == screen }
    val lazyGrid = BendeyUiDiagnostics.lastLazyGridSnapshot?.takeIf { it.screen == screen }

    LaunchedEffect(screen, probes.size, lazyList, lazyGrid, stateItemCount) {
        BendeyLayoutProbeRegistry.logTree(screen)
    }

    val listProbeTag = when (listType) {
        BendeyDiagnosticListType.LazyColumn -> "productos.lazy_column"
        BendeyDiagnosticListType.LazyColumnClientes -> "clientes.lazy_column"
        BendeyDiagnosticListType.LazyColumnVentas -> "ventas.lazy_column"
        BendeyDiagnosticListType.LazyGridMesas -> "mesas.lazy_grid"
        BendeyDiagnosticListType.None -> null
    }
    val listProbe = listProbeTag?.let { tag -> probes.find { it.tag == tag } }
    val parentProbe = listProbe?.parentTag?.let { parent -> probes.find { it.tag == parent } }
    val overlapping = findOverlappingProbes(listProbe, probes)
    val largestOccupier = probes
        .filter { it.tag != listProbeTag && it.heightPx > 0 }
        .maxByOrNull { it.areaPx }

    Column(
        modifier = modifier
            .semantics { testTag = "bendey_render_diagnostics_panel" }
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("RENDER DIAG · $screen", style = MaterialTheme.typography.labelMedium, color = Color.Cyan)
        Text("state ítems: $stateItemCount", style = MaterialTheme.typography.labelSmall, color = Color.White)

        lazyList?.let { info ->
            Text(
                "LazyColumn total=${info.totalItemsCount} visible=${info.visibleItemsCount} " +
                    "idx=${info.firstVisibleIndex}..${info.lastVisibleIndex}",
                style = MaterialTheme.typography.labelSmall,
                color = if (info.visibleItemsCount > 0) Color.Green else Color.Yellow,
            )
            Text(
                "viewport=${info.viewportWidthPx}x${info.viewportHeightPx}px " +
                    "listNode=${info.listWidthPx}x${info.listHeightPx}px parent=${info.parentTag}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
            with(density) {
                Text(
                    "viewport dp≈${info.viewportWidthPx.toDp()} x ${info.viewportHeightPx.toDp()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                )
            }
        }

        lazyGrid?.let { info ->
            Text(
                "LazyGrid total=${info.totalItemsCount} visible=${info.visibleItemsCount} " +
                    "idx=${info.firstVisibleIndex}..${info.lastVisibleIndex}",
                style = MaterialTheme.typography.labelSmall,
                color = if (info.visibleItemsCount > 0) Color.Green else Color.Yellow,
            )
            Text(
                "viewport=${info.viewportWidthPx}x${info.viewportHeightPx}px " +
                    "gridNode=${info.gridWidthPx}x${info.gridHeightPx}px",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

        listProbe?.let { probe ->
            Text(
                "lista bounds: ${probe.widthPx}x${probe.heightPx}px @(${probe.leftPx.toInt()},${probe.topPx.toInt()})",
                style = MaterialTheme.typography.labelSmall,
                color = if (probe.heightPx > 0) Color.Green else Color.Red,
            )
            Text(
                "composable superior: ${probe.parentTag ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

        parentProbe?.let { probe ->
            Text(
                "altura padre (${probe.tag}): ${probe.heightPx}px · ${probe.widthPx}x${probe.heightPx}",
                style = MaterialTheme.typography.labelSmall,
                color = if (probe.heightPx > 0) Color.Green else Color.Red,
            )
        }

        largestOccupier?.let { probe ->
            Text(
                "mayor área (excl. lista): ${probe.tag} ${probe.widthPx}x${probe.heightPx}px order=${probe.compositionOrder}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFAB40),
            )
        }

        if (overlapping.isNotEmpty()) {
            Text("SOLAPAN lista (orden composición ↑ = encima):", style = MaterialTheme.typography.labelSmall, color = Color.Red)
            overlapping.forEach { (probe, relation) ->
                Text(
                    " · ${probe.tag} ${probe.widthPx}x${probe.heightPx} $relation order=${probe.compositionOrder}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF8A80),
                )
            }
        }

        Text("— sondas (${probes.size}) —", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        probes.forEach { probe ->
            Text(
                "${probe.tag}: ${probe.widthPx}x${probe.heightPx} parent=${probe.parentTag} zOrder=${probe.compositionOrder}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

enum class BendeyDiagnosticListType {
    None,
    LazyColumn,
    LazyColumnClientes,
    LazyColumnVentas,
    LazyGridMesas,
}

private fun findOverlappingProbes(
    listProbe: BendeyLayoutProbeSnapshot?,
    probes: List<BendeyLayoutProbeSnapshot>,
): List<Pair<BendeyLayoutProbeSnapshot, String>> {
    if (listProbe == null || listProbe.heightPx <= 0) return emptyList()
    val listBounds = listProbe.boundsInRoot
    return probes
        .filter { it.tag != listProbe.tag && it.heightPx > 0 && it.widthPx > 0 }
        .mapNotNull { probe ->
            val intersection = intersectArea(listBounds, probe.boundsInRoot)
            if (intersection <= 0) return@mapNotNull null
            val relation = when {
                probe.compositionOrder > listProbe.compositionOrder -> "ENCIMA"
                probe.compositionOrder < listProbe.compositionOrder -> "DEBAJO"
                else -> "MISMO_ORDEN"
            }
            probe to "${relation} overlap=${intersection}px²"
        }
        .sortedByDescending { it.first.compositionOrder }
}

private fun intersectArea(a: Rect, b: Rect): Long {
    val left = max(a.left, b.left)
    val top = max(a.top, b.top)
    val right = min(a.right, b.right)
    val bottom = min(a.bottom, b.bottom)
    val w = (right - left).coerceAtLeast(0f)
    val h = (bottom - top).coerceAtLeast(0f)
    return (w * h).toLong()
}

private fun probeColorForTag(tag: String): Color {
    val hue = (tag.hashCode() and 0xFFFF) % 360
    return Color.hsv(hue.toFloat(), 0.65f, 0.95f)
}
