package com.bendey.restaurant.core.ui.layout.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Tokens POS workspace — densidad y productividad del cajero.
 * No modifica políticas adaptive ni layout del workspace.
 */
object AdaptivePos {

    fun panelGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xxs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xs
        else -> 0.dp
    }

    fun categoryRailWidth(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 0.dp
        BendeyAdaptiveProfile.MediumLandscape -> 128.dp
        BendeyAdaptiveProfile.Expanded -> AdaptiveDimensions.categoryRailWidth(profile)
        else -> 0.dp
    }

    fun cartPanelWidth(profile: BendeyAdaptiveProfile): Dp = AdaptiveDimensions.cartPanelWidth(profile)

    fun workspacePadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xxs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xs
        else -> 0.dp
    }

    fun peekBarHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 68.dp
        else -> 0.dp
    }

    /** Altura barra flotante pedidos/carrito en tablet portrait (evolución móvil). */
    fun portraitFloatingBarHeight(): Dp = 96.dp

    /** Tamaño táctil de chips pedidos/carrito en tablet portrait. */
    fun portraitFloatingActionChipSize(): Dp = 92.dp

    /** Fracción de altura del bottom sheet de carrito en tablet portrait (70–85 %). */
    fun portraitCartSheetHeightFraction(): Float = 0.78f

    fun portraitMobileCategoryChipGap(): Dp = BendeySpacing.sm

    fun portraitMobileGridGap(): Dp = 10.dp

    fun portraitMobileCatalogHorizontalPadding(): Dp = BendeySpacing.md

    fun portraitMobileSearchHeight(): Dp = 44.dp

    fun panelElevation(profile: BendeyAdaptiveProfile): Dp = 0.dp

    fun cartPanelElevation(profile: BendeyAdaptiveProfile): Dp = 1.dp

    fun searchBarMinHeight(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 40.dp
        BendeyAdaptiveProfile.MediumLandscape -> 42.dp
        BendeyAdaptiveProfile.Expanded -> 42.dp
        else -> 38.dp
    }

    fun searchBarQrSize(profile: BendeyAdaptiveProfile): Dp = searchBarMinHeight(profile)

    /** Padding izquierdo del catálogo — reducido para ganar columnas visibles. */
    fun catalogHorizontalPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape -> 6.dp
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xs
        else -> 12.dp
    }

    /** Espacio vertical entre controles del catálogo. Portrait: mínimo (prioriza grid). */
    fun catalogSectionGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 2.dp
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xxs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xxs
        else -> BendeySpacing.xxs
    }

    fun catalogGridGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.Expanded -> 8.dp
        BendeyAdaptiveProfile.MediumPortrait,
        BendeyAdaptiveProfile.MediumLandscape,
        -> 6.dp
        else -> 6.dp
    }

    fun cartPanelPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.sm
        else -> BendeySpacing.sm
    }

    fun cartHeaderDividerPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        else -> BendeySpacing.xxs
    }

    fun cartFooterTopPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        else -> BendeySpacing.xs
    }

    /** Landscape: líneas más compactas → más ítems visibles. Portrait: aire moderado. */
    fun cartLineGap(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xs
        else -> BendeySpacing.xxs
    }

    fun cartLinePadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        else -> BendeySpacing.xxs
    }

    fun cartSummaryPadding(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.xs
        BendeyAdaptiveProfile.MediumLandscape -> BendeySpacing.xxs
        BendeyAdaptiveProfile.Expanded -> BendeySpacing.xxs
        else -> BendeySpacing.sm
    }

    fun cartSummaryAmountStyle(profile: BendeyAdaptiveProfile): CartSummaryAmountStyle = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> CartSummaryAmountStyle.HeadlineLarge
        else -> CartSummaryAmountStyle.HeadlineMedium
    }

    fun cartLineStepperSize(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 44.dp
        BendeyAdaptiveProfile.MediumLandscape,
        BendeyAdaptiveProfile.Expanded,
        -> 40.dp
        else -> 40.dp
    }

    fun cartActionGridSpacing(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> BendeySpacing.sm
        else -> BendeySpacing.xs
    }

    fun cartHeaderIconSize(profile: BendeyAdaptiveProfile): Dp = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 40.dp
        else -> 36.dp
    }

    fun searchBarFocusedElevation(profile: BendeyAdaptiveProfile): Dp = 0.dp

    fun orderTypeStripMinHeight(profile: BendeyAdaptiveProfile): Dp = 0.dp

    /** Celdas más amplias → menos columnas, mayor comodidad táctil. */
    fun productMinCellDp(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 150
        BendeyAdaptiveProfile.MediumLandscape -> 200
        BendeyAdaptiveProfile.Expanded -> 208
        else -> 140
    }

    fun maxProductColumns(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.MediumPortrait -> 4
        BendeyAdaptiveProfile.MediumLandscape -> 4
        BendeyAdaptiveProfile.Expanded -> 4
        else -> 3
    }

    fun productImageAspectRatio(profile: BendeyAdaptiveProfile): Float = 1.05f

    fun productGridColumns(profile: BendeyAdaptiveProfile, contentWidthDp: Int): Int {
        val minCell = productMinCellDp(profile)
        val gapForColumns = catalogGridGap(profile).value.toInt()
        val available = contentWidthDp.coerceAtLeast(minCell)
        return (available / (minCell + gapForColumns)).coerceIn(2, maxProductColumns(profile))
    }

    /** Columnas catálogo evolución móvil — tablet portrait únicamente. */
    fun portraitMobileProductGridColumns(contentWidthDp: Int): Int {
        val minCell = productMinCellDp(BendeyAdaptiveProfile.MediumPortrait)
        val gap = portraitMobileGridGap().value.toInt()
        val horizontal = portraitMobileCatalogHorizontalPadding().value.toInt() * 2
        val available = (contentWidthDp - horizontal).coerceAtLeast(minCell)
        return (available / (minCell + gap)).coerceIn(2, maxProductColumns(BendeyAdaptiveProfile.MediumPortrait))
    }

    /** Catálogo POS en teléfono — 3 columnas portrait (mínimo histórico), 4 landscape. */
    fun compactPosProductGridColumns(profile: BendeyAdaptiveProfile): Int = when (profile) {
        BendeyAdaptiveProfile.CompactLandscape -> 4
        BendeyAdaptiveProfile.CompactPortrait -> 3
        else -> 3
    }
}

enum class CartSummaryAmountStyle {
    HeadlineMedium,
    HeadlineLarge,
}

enum class BendeyPosWorkspaceMode {
    Compact,
    MediumPortrait,
    MediumLandscape,
    Expanded,
}

/**
 * Modo de workspace POS según perfil + orientación física de la pantalla.
 *
 * Tablets en vertical (incl. perfil [BendeyAdaptiveProfile.Expanded]) usan evolución móvil.
 * Solo landscape tablet mantiene catálogo + carrito permanente.
 */
fun BendeyAdaptiveProfile.toPosWorkspaceMode(physicalPortrait: Boolean): BendeyPosWorkspaceMode = when {
    isCompact -> BendeyPosWorkspaceMode.Compact
    physicalPortrait -> BendeyPosWorkspaceMode.MediumPortrait
    isExpanded -> BendeyPosWorkspaceMode.Expanded
    else -> BendeyPosWorkspaceMode.MediumLandscape
}

/** @deprecated Preferir [toPosWorkspaceMode] con orientación física vía [rememberPosWorkspaceMode]. */
fun BendeyAdaptiveProfile.toPosWorkspaceMode(): BendeyPosWorkspaceMode = when (this) {
    BendeyAdaptiveProfile.CompactPortrait,
    BendeyAdaptiveProfile.CompactLandscape,
    -> BendeyPosWorkspaceMode.Compact
    BendeyAdaptiveProfile.MediumPortrait -> BendeyPosWorkspaceMode.MediumPortrait
    BendeyAdaptiveProfile.MediumLandscape -> BendeyPosWorkspaceMode.MediumLandscape
    BendeyAdaptiveProfile.Expanded -> BendeyPosWorkspaceMode.Expanded
}
