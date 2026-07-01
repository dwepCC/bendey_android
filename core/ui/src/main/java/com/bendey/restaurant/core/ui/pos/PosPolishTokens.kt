package com.bendey.restaurant.core.ui.pos

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/**
 * Tokens visuales de pulido POS — no forman parte del Adaptive Core.
 * Compact y tablet vertical: una columna en modales; tablet horizontal: layout denso.
 */
object PosPolishTokens {

    fun isTabletProfile(profile: BendeyAdaptiveProfile): Boolean = !profile.isCompact

    /** Dos columnas / modal denso solo en tablet horizontal. */
    fun usesPosTabletDialogLayout(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean,
    ): Boolean = isTabletProfile(profile) && !physicalPortrait

    fun dialogWidthFraction(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Float = when {
        profile.isCompact || physicalPortrait -> 0.94f
        profile.isExpanded -> 0.58f
        else -> 0.72f
    }

    fun dialogMaxHeight(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Dp = when {
        profile.isCompact || physicalPortrait -> 720.dp
        profile.isExpanded -> 640.dp
        else -> 600.dp
    }

    fun dialogPadding(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Dp = if (profile.isCompact || physicalPortrait) BendeySpacing.lg else BendeySpacing.md

    fun dialogSectionGap(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Dp = if (profile.isCompact || physicalPortrait) BendeySpacing.md else BendeySpacing.sm

    fun dialogFieldGap(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Dp = if (profile.isCompact || physicalPortrait) BendeySpacing.sm else BendeySpacing.xs

    fun receiptModalMaxHeight(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean = false,
    ): Dp = if (profile.isCompact || physicalPortrait) 520.dp else 440.dp

    @Composable
    fun productNameStyle(profile: BendeyAdaptiveProfile): TextStyle = when {
        profile.isCompact -> MaterialTheme.typography.labelSmall
        profile == BendeyAdaptiveProfile.MediumPortrait ->
            MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.labelMedium
    }

    @Composable
    fun productNameLineHeight(profile: BendeyAdaptiveProfile): TextStyle =
        productNameStyle(profile).copy(
            lineHeight = when {
                profile.isCompact -> MaterialTheme.typography.labelSmall.lineHeight
                else -> MaterialTheme.typography.labelLarge.lineHeight
            },
        )
}
