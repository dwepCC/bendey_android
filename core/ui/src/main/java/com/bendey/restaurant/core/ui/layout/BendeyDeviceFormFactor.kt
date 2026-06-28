package com.bendey.restaurant.core.ui.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass

/**
 * Detección teléfono vs tablet.
 *
 * - Activity / orientación: [smallestScreenWidthDp] ≥ 600 ([Android large screens](https://developer.android.com/develop/ui/compose/layouts/adaptive)).
 * - Layout Compose: [WindowSizeClass] vía [currentWindowAdaptiveInfo].
 */
@Stable
object BendeyDeviceFormFactor {
    /** Umbral oficial WindowSizeClass: Compact &lt; 600dp. */
    const val TABLET_SMALLEST_WIDTH_DP = 600

    fun isTablet(smallestScreenWidthDp: Int): Boolean =
        smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP

    fun isPhone(smallestScreenWidthDp: Int): Boolean = !isTablet(smallestScreenWidthDp)
}

@Composable
fun rememberIsTabletDevice(): Boolean {
    val smallestWidth = LocalConfiguration.current.smallestScreenWidthDp
    return BendeyDeviceFormFactor.isTablet(smallestWidth)
}

/** Ancho distinto de Compact — API [currentWindowAdaptiveInfo] (Material 3 Adaptive). */
@Composable
fun rememberIsExpandedWidth(): Boolean {
    return rememberWindowSizeClass()
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
}

/**
 * List-detail / two-pane manual: solo tablet física.
 * Evita panel vacío en teléfono apaisado (ancho MEDIUM pero smallestWidth &lt; 600).
 */
@Composable
fun rememberUseAdaptiveTwoPane(): Boolean = rememberIsTabletDevice()

/** Clase de tamaño de ventana actual (Material 3 Adaptive). */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    return currentWindowAdaptiveInfo().windowSizeClass
}
