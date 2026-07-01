package com.bendey.restaurant.core.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/**
 * Tokens visuales del header operativo tablet — no forman parte del Adaptive Core.
 * Solo consumidos por [BendeyOperationalTopBar].
 */
internal object OperationalTopBarTokens {

    fun barHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 48.dp
        BendeyAdaptiveProfile.MediumLandscape -> 48.dp
        BendeyAdaptiveProfile.Expanded -> 50.dp
        else -> 52.dp
    }

    fun horizontalPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.md
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.lg
        else -> BendeySpacing.sm
    }

    /** Separación entre grupos principales (identidad · nav · acciones). */
    fun groupGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.lg
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xl
        else -> BendeySpacing.md
    }

    fun brandLogoSize(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 30.dp
        BendeyAdaptiveProfile.MediumLandscape -> 32.dp
        BendeyAdaptiveProfile.Expanded -> 36.dp
        else -> 32.dp
    }

    val brandLogoTextGap: Dp = 6.dp

    fun brandMaxWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 148.dp
        BendeyAdaptiveProfile.MediumLandscape -> 208.dp
        BendeyAdaptiveProfile.Expanded -> 260.dp
        else -> Dp.Unspecified
    }

    fun navItemHeight(profile: BendeyAdaptiveProfile): Dp = 36.dp

    fun navShowLabels(profile: BendeyAdaptiveProfile): Boolean =
        profile != BendeyAdaptiveProfile.MediumPortrait

    fun navItemHorizontalPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        else -> BendeySpacing.md
    }

    fun navGap(profile: BendeyAdaptiveProfile): Dp = BendeySpacing.xxs
}
