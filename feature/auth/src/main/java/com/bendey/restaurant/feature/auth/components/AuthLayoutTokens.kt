package com.bendey.restaurant.feature.auth.components

import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/** Tokens de layout compartidos del módulo auth (evita valores mágicos dispersos). */
internal object AuthLayoutTokens {
    val contentMaxWidthCompact = 480.dp
    val contentMaxWidthExpanded = 520.dp
    val homeContentMaxWidth = 720.dp
    val loginFormMaxWidth = 420.dp
    val pinFormMaxWidth = 360.dp
    val stationGridMinCell = 168.dp

    val iconContainerProminent = BendeySpacing.touchTarget + 8.dp
    val iconContainerStandard = BendeySpacing.touchTarget + 4.dp
    val iconProminent = 28.dp
    val iconStandard = 26.dp
    val iconNav = 22.dp
    val iconBadge = 16.dp
    val successIconSize = 56.dp

    val cardElevationRest = 1.dp
    val cardElevationRaised = 2.dp
    val cardElevationProminent = 3.dp
    val cardElevationPressed = 4.dp

    val logoHeightWelcome = 56.dp
    val logoHeightHomeHeader = 44.dp
    val logoHeightHomeHeaderCompact = 36.dp
    val logoHeightLogin = 52.dp
    val logoHeightPin = 48.dp
}
