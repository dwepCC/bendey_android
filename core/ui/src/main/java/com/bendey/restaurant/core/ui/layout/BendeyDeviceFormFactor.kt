package com.bendey.restaurant.core.ui.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Detección teléfono vs tablet.
 *
 * - Activity / orientación: [smallestScreenWidthDp] ≥ 600 ([Android large screens](https://developer.android.com/develop/ui/compose/layouts/adaptive)).
 * - Layout Compose: [WindowWidthSizeClass] vía [currentWindowAdaptiveInfo].
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
    return rememberWindowWidthSizeClass() != WindowWidthSizeClass.COMPACT
}

/**
 * List-detail / two-pane manual: solo tablet física.
 * Evita panel vacío en teléfono apaisado (ancho MEDIUM pero smallestWidth &lt; 600).
 */
@Composable
fun rememberUseAdaptiveTwoPane(): Boolean = rememberIsTabletDevice()

/** Clase de ancho actual (Material 3 Adaptive). Una lectura por recomposición de pantalla. */
@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    return currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
}
