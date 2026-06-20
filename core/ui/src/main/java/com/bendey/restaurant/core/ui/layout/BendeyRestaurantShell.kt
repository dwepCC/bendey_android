package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

/**
 * Shell operativo: barra de estado tomate; contenido blanco a ancho completo
 * con esquinas superiores redondeadas (como React `RestaurantLayout`).
 */
@Composable
fun BendeyRestaurantShell(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Rest900)
            .bendeyStatusBarsPadding(),
    ) {
        topBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(BendeyColors.Surface),
        ) {
            content(Modifier.weight(1f))
            bottomBar()
        }
    }
}
