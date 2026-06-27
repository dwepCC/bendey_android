package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Escala de espaciado Bendey — 4 · 8 · 12 · 16 · 24 · 32 dp */
object BendeySpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp

    /** Alias semánticos (documentación / APIs externas). */
    val ExtraExtraSmall get() = xxs
    val ExtraSmall get() = xs
    val Small get() = sm
    val Medium get() = md
    val Large get() = lg
    val ExtraLarge get() = xl

    val screenHorizontal = md
    val screenVertical = sm
    val cardPadding = md
    val sectionGap = sm
    val formFieldGap = sm
    val buttonHeight = 44.dp
    val touchTarget = 48.dp
}
