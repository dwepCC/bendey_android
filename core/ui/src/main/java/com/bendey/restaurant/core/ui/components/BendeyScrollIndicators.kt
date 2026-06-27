package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

private val HintIconSize = 22.dp
private val HintAlpha = 0.42f
private val EdgeInset = 6.dp

private val LocalScrollHintBounce = compositionLocalOf { 0f }

/**
 * Provee una única [rememberInfiniteTransition] compartida para todas las flechas de scroll.
 * Montar una vez en [com.bendey.restaurant.core.ui.layout.BendeyRestaurantShell].
 */
@Composable
fun BendeyScrollHintProvider(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "bendey_scroll_hint_bounce")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bendey_scroll_hint_offset",
    )
    CompositionLocalProvider(LocalScrollHintBounce provides bounce) {
        content()
    }
}

private fun ScrollState.canScrollDown(): Boolean = maxValue > 0 && value < maxValue
private fun ScrollState.canScrollUp(): Boolean = value > 0
private fun ScrollState.canScrollEnd(): Boolean = maxValue > 0 && value < maxValue
private fun ScrollState.canScrollStart(): Boolean = value > 0

private fun LazyListState.canScrollDown(): Boolean {
    val info = layoutInfo
    if (info.totalItemsCount == 0) return false
    val last = info.visibleItemsInfo.lastOrNull() ?: return false
    return last.index < info.totalItemsCount - 1 ||
        last.offset + last.size > info.viewportEndOffset + 1
}

private fun LazyListState.canScrollUp(): Boolean {
    val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return false
    return first.index > 0 || first.offset < layoutInfo.viewportStartOffset - 1
}

private fun LazyGridState.canScrollDown(): Boolean {
    val info = layoutInfo
    if (info.totalItemsCount == 0) return false
    val last = info.visibleItemsInfo.lastOrNull() ?: return false
    return last.index < info.totalItemsCount - 1 ||
        last.offset.y + last.size.height > info.viewportEndOffset + 1
}

private fun LazyGridState.canScrollUp(): Boolean {
    val first = layoutInfo.visibleItemsInfo.firstOrNull() ?: return false
    return first.index > 0 || first.offset.y < layoutInfo.viewportStartOffset - 1
}

@Composable
private fun ScrollHintIcon(
    modifier: Modifier = Modifier,
    bounceAxis: BounceAxis = BounceAxis.None,
    icon: @Composable () -> Unit,
) {
    val bounce = LocalScrollHintBounce.current
    val offsetModifier = when (bounceAxis) {
        BounceAxis.VerticalDown -> Modifier.offset(y = bounce.dp)
        BounceAxis.VerticalUp -> Modifier.offset(y = (-bounce).dp)
        BounceAxis.HorizontalEnd -> Modifier.offset(x = bounce.dp)
        BounceAxis.HorizontalStart -> Modifier.offset(x = (-bounce).dp)
        BounceAxis.None -> Modifier
    }
    Box(
        modifier = modifier
            .then(offsetModifier)
            .alpha(HintAlpha),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

private enum class BounceAxis {
    None, VerticalDown, VerticalUp, HorizontalEnd, HorizontalStart,
}

@Composable
fun BendeyHorizontalScrollRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    showScrollHints: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
        if (showScrollHints) {
            BendeyHorizontalScrollHints(scrollState)
        }
    }
}

@Composable
fun BendeyVerticalScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    showScrollHints: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        if (showScrollHints) {
            BendeyVerticalScrollHints(scrollState)
        }
    }
}

@Composable
fun BendeyLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    showScrollHints: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
        if (showScrollHints) {
            BendeyLazyListScrollHints(listState = state)
        }
    }
}

@Composable
fun BendeyLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    showScrollHints: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    Box(modifier = modifier) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = columns,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
        if (showScrollHints) {
            BendeyLazyGridScrollHints(gridState = state)
        }
    }
}

@Composable
fun BoxScope.BendeyVerticalScrollHints(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val canDown by remember(scrollState) { derivedStateOf { scrollState.canScrollDown() } }
    val canUp by remember(scrollState) { derivedStateOf { scrollState.canScrollUp() } }
    if (canDown) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = EdgeInset),
            bounceAxis = BounceAxis.VerticalDown,
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Más contenido abajo",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
    if (canUp) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.TopCenter)
                .padding(top = EdgeInset),
            bounceAxis = BounceAxis.VerticalUp,
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Más contenido arriba",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
}

@Composable
fun BoxScope.BendeyHorizontalScrollHints(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val canEnd by remember(scrollState) { derivedStateOf { scrollState.canScrollEnd() } }
    val canStart by remember(scrollState) { derivedStateOf { scrollState.canScrollStart() } }
    if (canEnd) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.CenterEnd)
                .padding(end = EdgeInset),
            bounceAxis = BounceAxis.HorizontalEnd,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Más contenido a la derecha",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
    if (canStart) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.CenterStart)
                .padding(start = EdgeInset),
            bounceAxis = BounceAxis.HorizontalStart,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Más contenido a la izquierda",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
}

@Composable
fun BoxScope.BendeyLazyListScrollHints(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val canDown by remember(listState) { derivedStateOf { listState.canScrollDown() } }
    val canUp by remember(listState) { derivedStateOf { listState.canScrollUp() } }
    if (canDown) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = EdgeInset),
            bounceAxis = BounceAxis.VerticalDown,
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Más contenido abajo",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
    if (canUp) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.TopCenter)
                .padding(top = EdgeInset),
            bounceAxis = BounceAxis.VerticalUp,
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Más contenido arriba",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
}

@Composable
fun BoxScope.BendeyLazyGridScrollHints(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val canDown by remember(gridState) { derivedStateOf { gridState.canScrollDown() } }
    val canUp by remember(gridState) { derivedStateOf { gridState.canScrollUp() } }
    if (canDown) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = EdgeInset),
            bounceAxis = BounceAxis.VerticalDown,
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Más contenido abajo",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
    if (canUp) {
        ScrollHintIcon(
            modifier = modifier
                .align(Alignment.TopCenter)
                .padding(top = EdgeInset),
            bounceAxis = BounceAxis.VerticalUp,
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Más contenido arriba",
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(HintIconSize),
            )
        }
    }
}
