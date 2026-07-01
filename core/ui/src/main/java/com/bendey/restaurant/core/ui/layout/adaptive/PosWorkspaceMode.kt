package com.bendey.restaurant.core.ui.layout.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Modo workspace POS con orientación física real (no solo perfil adaptive).
 *
 * Corrige tablets grandes en vertical: perfil [BendeyAdaptiveProfile.Expanded] sigue siendo
 * Expanded en adaptive, pero el POS debe usar evolución móvil en portrait.
 */
@Composable
fun rememberPhysicalPortrait(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        configuration.screenHeightDp >= configuration.screenWidthDp
    }
}

@Composable
fun rememberPosWorkspaceMode(): BendeyPosWorkspaceMode {
    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    return remember(profile, physicalPortrait) {
        profile.toPosWorkspaceMode(physicalPortrait = physicalPortrait)
    }
}
