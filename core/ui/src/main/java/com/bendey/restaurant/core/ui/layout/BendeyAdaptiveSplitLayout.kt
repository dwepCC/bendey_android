package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Split catálogo + panel lateral en tablet dentro de un [BendeyFlexibleContentSlot].
 *
 * Patrón compartido por POS, Mesa y pantallas administrativas two-pane.
 */
@Composable
fun BendeyAdaptiveSplitLayout(
    modifier: Modifier = Modifier,
    primaryWeight: Float = 0.58f,
    secondaryWeight: Float = 0.42f,
    primary: @Composable RowScope.(Modifier) -> Unit,
    secondary: @Composable RowScope.(Modifier) -> Unit,
) {
    Row(modifier = modifier) {
        primary(
            Modifier
                .weight(primaryWeight)
                .fillMaxHeight(),
        )
        secondary(
            Modifier
                .weight(secondaryWeight)
                .fillMaxHeight(),
        )
    }
}
