package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens

/** Altura visible de la barra inferior (sin clearance del FAB). */
val BendeyBottomBarHeight = 56.dp

/** Espacio superior reservado para el FAB central flotante. */
val BendeyBottomBarFabOverlap = 20.dp

/** Espacio total reservado en el contenedor (barra + FAB flotante). */
val BendeyBottomBarInset = BendeyBottomBarHeight + BendeyBottomBarFabOverlap

/**
 * Shell operativo: barra de estado tomate; contenido blanco a ancho completo
 * con esquinas superiores redondeadas (como React `RestaurantLayout`).
 * El FAB de la barra inferior puede superponerse al contenido sin recortarse.
 */
@Composable
fun BendeyRestaurantShell(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Rest900)
            .bendeyStatusBarsPadding(),
    ) {
        topBar()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(BendeyShapeTokens.sheet)
                .background(BendeyColors.Surface),
        ) {
            val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val contentBottomPad = if (showBottomBar) BendeyBottomBarHeight + navigationBarPadding else 0.dp
            content(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPad),
            )
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    bottomBar()
                }
            }
        }
    }
}
