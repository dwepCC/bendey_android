package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object BendeyShapeTokens {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(14.dp)
    val xl = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val pill = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(20.dp)
    /** Barras de progreso / mini indicadores */
    val bar = RoundedCornerShape(4.dp)
    /** Punto de estado (mesas, badges) */
    val dot = RoundedCornerShape(2.dp)
}

val BendeyShapes = Shapes(
    extraSmall = BendeyShapeTokens.xs,
    small = BendeyShapeTokens.sm,
    medium = BendeyShapeTokens.md,
    large = BendeyShapeTokens.lg,
    extraLarge = BendeyShapeTokens.xl,
)
