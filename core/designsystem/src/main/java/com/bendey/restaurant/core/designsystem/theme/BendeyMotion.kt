package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

/** Tokens de animación Bendey — única capa para motion (incl. futuro Expressive). */
object BendeyMotion {
    val NavSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
    val ChipColorSpring = spring<Color>(stiffness = Spring.StiffnessMedium)
    val ContentSpring = spring<Float>(stiffness = Spring.StiffnessLow)
    val FadeTween = tween<Float>(durationMillis = 180)
    val SheetTween = tween<Float>(durationMillis = 260)

    /** Experience Layer — alineado con motion expresivo M3 (springs discretos). */
    val ExpressiveSpatialSpring = spring<Float>(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )
    val ExpressiveEffectsTween = tween<Float>(
        durationMillis = 220,
        easing = FastOutSlowInEasing,
    )
}
