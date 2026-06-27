package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/** Card estándar Bendey — borde sutil, sin sombra pesada. Preferir [BendeyCard] en código nuevo. */
@Composable
fun BendeySurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = BendeyColors.Surface,
    contentPadding: PaddingValues = PaddingValues(BendeySpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    BendeyCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = contentPadding,
        content = content,
    )
}
