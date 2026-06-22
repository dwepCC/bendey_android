package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Valores estándar de cards Bendey — borde sutil, sin sombra pesada. */
object BendeyCardDefaults {
    val shape = BendeyShapeTokens.lg
    val border = BorderStroke(1.dp, BendeyColors.Outline.copy(alpha = 0.65f))
    val dialogShape = BendeyShapeTokens.xl
    val sheetShape = BendeyShapeTokens.pill

    @Composable
    fun colors() = CardDefaults.cardColors(containerColor = BendeyColors.Surface)

    @Composable
    fun elevation() = CardDefaults.cardElevation(defaultElevation = 0.dp)

    @Composable
    fun variantColors() = CardDefaults.cardColors(containerColor = BendeyColors.SurfaceVariant.copy(alpha = 0.45f))

    fun Modifier.standardCardBorder(): Modifier = clip(shape)
}
