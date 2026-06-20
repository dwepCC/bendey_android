package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

/** Espacio reservado para la barra inferior + FAB flotante. */
val BendeyBottomBarInset = 76.dp

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
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(BendeyColors.Surface),
        ) {
            val contentBottomPad = if (showBottomBar) BendeyBottomBarInset else 0.dp
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
