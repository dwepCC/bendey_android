package com.bendey.restaurant.core.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

object BendeyMotion {
    val NavSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
    val ChipColorSpring = spring<Color>(stiffness = Spring.StiffnessMedium)
    val ContentSpring = spring<Float>(stiffness = Spring.StiffnessLow)
    val FadeTween = tween<Float>(durationMillis = 180)
    val SheetTween = tween<Float>(durationMillis = 260)
}
