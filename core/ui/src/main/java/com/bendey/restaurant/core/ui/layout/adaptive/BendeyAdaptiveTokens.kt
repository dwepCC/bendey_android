package com.bendey.restaurant.core.ui.layout.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Design system adaptive Bendey — espaciado, dimensiones y grillas por [BendeyAdaptiveProfile].
 *
 * Usar estas APIs en lugar de breakpoints sueltos o `maxWidth` ad hoc en pantallas.
 */
object AdaptiveSpacing {

    fun screenHorizontal(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.screenHorizontal
        BendeyAdaptiveProfile.MediumPortrait,
        BendeyAdaptiveProfile.MediumLandscape,
        -> BendeySpacing.md
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.lg
    }

    fun sectionGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.sectionGap
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.md
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.lg
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.lg
    }

    fun cardPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.cardPadding
        BendeyAdaptiveProfile.MediumPortrait,
        BendeyAdaptiveProfile.MediumLandscape,
        -> BendeySpacing.md
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.lg
    }

    fun gridGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.xxs
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape,
        BendeyAdaptiveProfile.Expanded,
        -> BendeySpacing.sm
    }

    fun bottomBarScrollExtra(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.md
        else -> BendeySpacing.sm
    }

    fun topBarHorizontal(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.md
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.lg
        else -> BendeySpacing.xxs
    }

    fun topBarSectionGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape,
        BendeyAdaptiveProfile.Expanded,
        -> BendeySpacing.sm
        else -> BendeySpacing.xxs
    }

    fun drawerSectionGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> BendeySpacing.md
        else -> BendeySpacing.lg
    }
}

object AdaptiveDimensions {

    fun cartPanelWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> Dp.Unspecified
        BendeyAdaptiveProfile.MediumPortrait -> 360.dp
        BendeyAdaptiveProfile.MediumLandscape -> 320.dp
        BendeyAdaptiveProfile.Expanded -> 400.dp
    }

    fun categoryRailWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        BendeyAdaptiveProfile.MediumPortrait,
        -> 0.dp
        BendeyAdaptiveProfile.MediumLandscape -> 128.dp
        BendeyAdaptiveProfile.Expanded -> 152.dp
    }

    fun operationalTopBarHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> 52.dp
        else -> 56.dp
    }

    fun operationalNavSegmentHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 40.dp
        BendeyAdaptiveProfile.MediumLandscape -> 44.dp
        BendeyAdaptiveProfile.Expanded -> 48.dp
        else -> 0.dp
    }

    fun brandLogoSize(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 32.dp
        BendeyAdaptiveProfile.MediumLandscape -> 36.dp
        BendeyAdaptiveProfile.Expanded -> 40.dp
        else -> 0.dp
    }

    fun brandSectionMaxWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 180.dp
        BendeyAdaptiveProfile.MediumLandscape -> 220.dp
        BendeyAdaptiveProfile.Expanded -> 280.dp
        else -> Dp.Unspecified
    }

    fun productCardMinHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> 0.dp
        BendeyAdaptiveProfile.MediumPortrait -> 112.dp
        BendeyAdaptiveProfile.MediumLandscape -> 120.dp
        BendeyAdaptiveProfile.Expanded -> 128.dp
    }

    /** Ancho mínimo táctil recomendado para tarjetas POS en tablet. */
    fun productCardMinTouchWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait,
        BendeyAdaptiveProfile.CompactLandscape,
        -> 0.dp
            BendeyAdaptiveProfile.MediumPortrait -> 168.dp
        BendeyAdaptiveProfile.MediumLandscape -> 176.dp
        BendeyAdaptiveProfile.Expanded -> 184.dp
    }
}

object AdaptiveGrid {

    fun posProductColumns(profile: BendeyAdaptiveProfile, contentWidthDp: Int): Int {
        val minCell = when (profile) {
            BendeyAdaptiveProfile.CompactPortrait -> 140
            BendeyAdaptiveProfile.CompactLandscape -> 132
            BendeyAdaptiveProfile.MediumPortrait -> 150
            BendeyAdaptiveProfile.MediumLandscape -> 176
            BendeyAdaptiveProfile.Expanded -> 184
        }
        val gap = AdaptiveSpacing.gridGap(profile).value.toInt()
        val horizontal = AdaptiveSpacing.screenHorizontal(profile).value.toInt() * 2
        val available = (contentWidthDp - horizontal).coerceAtLeast(minCell)
        return (available / (minCell + gap)).coerceIn(2, maxPosColumns(profile))
    }

    fun catalogProductColumns(profile: BendeyAdaptiveProfile, contentWidthDp: Int): Int {
        if (profile.isCompact) {
            return when {
                contentWidthDp >= 600 -> 3
                else -> 2
            }
        }
        return posProductColumns(profile, contentWidthDp).coerceAtMost(5)
    }

    fun tableGridColumns(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait -> 3
        BendeyAdaptiveProfile.CompactLandscape -> 4
        BendeyAdaptiveProfile.MediumPortrait -> 4
        BendeyAdaptiveProfile.MediumLandscape -> 5
        BendeyAdaptiveProfile.Expanded -> 6
    }

    fun dashboardKpiColumns(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait -> 2
        BendeyAdaptiveProfile.CompactLandscape -> 4
        BendeyAdaptiveProfile.MediumPortrait -> 2
        BendeyAdaptiveProfile.MediumLandscape -> 4
        BendeyAdaptiveProfile.Expanded -> 4
    }

    private fun maxPosColumns(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.CompactPortrait -> 3
        BendeyAdaptiveProfile.CompactLandscape -> 4
        BendeyAdaptiveProfile.MediumPortrait -> 4
        BendeyAdaptiveProfile.MediumLandscape -> 4
        BendeyAdaptiveProfile.Expanded -> 5
    }
}

/** Alias semántico — agrupa tokens adaptive del design system. */
object AdaptiveTokens {
    val spacing get() = AdaptiveSpacing
    val dimensions get() = AdaptiveDimensions
    val grid get() = AdaptiveGrid
}
