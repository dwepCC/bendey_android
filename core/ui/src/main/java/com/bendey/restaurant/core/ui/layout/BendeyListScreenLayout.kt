package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Slot flexible bajo headers fijos en una [Column].
 *
 * **Causa raíz del height=0:** `Modifier.weight()` solo reparte altura cuando el padre
 * [Column] tiene altura acotada. Este slot debe ser hijo **directo** de
 * `Column(Modifier.fillMaxSize())` y envolver el scroll (LazyColumn/LazyGrid) con
 * `Modifier.fillMaxSize()` — nunca anidar otro `Column { … weight(1f) }` sin
 * `fillMaxSize()` entre PullToRefresh y la lista.
 */
@Composable
fun ColumnScope.BendeyFlexibleContentSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    Box(
        modifier = modifier
            .weight(1f, fill = true)
            .fillMaxWidth(),
    ) {
        content(Modifier.fillMaxSize())
    }
}

/**
 * Layout estándar para pantallas con headers fijos + lista/grid scrollable.
 *
 * Patrón correcto (equivalente al Dashboard que sí renderiza):
 * ```
 * Column(fillMaxSize) → headers → weight slot → PullToRefresh → Lazy*(fillMaxSize)
 * ```
 *
 * Evita el anti-patrón introducido en la migración adaptive:
 * `PullToRefresh { Column { headers; Box(weight) { Lazy* } } }` sin altura acotada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendeyListScreenLayout(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        header()
        BendeyFlexibleContentSlot { innerModifier ->
            if (onRefresh != null) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = innerModifier,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(Modifier.fillMaxSize())
                    }
                }
            } else {
                content(innerModifier)
            }
        }
    }
}
