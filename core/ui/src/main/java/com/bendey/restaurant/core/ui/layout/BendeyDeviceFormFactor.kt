package com.bendey.restaurant.core.ui.layout

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveInfo
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile

/**
 * Detección teléfono vs tablet.
 *
 * - Activity / orientación: [smallestScreenWidthDp] ≥ 600 ([Android large screens](https://developer.android.com/develop/ui/compose/layouts/adaptive)).
 * - Layout Compose: [BendeyAdaptiveProfile] vía [rememberBendeyAdaptiveInfo].
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

/**
 * Ancho distinto de Compact — derivado de [BendeyAdaptiveProfile].
 * Teléfonos siempre false (perfiles Compact*).
 */
@Composable
fun rememberIsExpandedWidth(): Boolean {
    val profile = rememberBendeyAdaptiveProfile()
    return !profile.isCompact
}

/**
 * List-detail / two-pane deshabilitado: gestión sidebar usa una columna + modales.
 *
 * @see com.bendey.restaurant.core.ui.layout.adaptive.rememberPosWorkspaceMode para POS/Mesas.
 */
@Composable
fun rememberUseAdaptiveTwoPane(): Boolean = false

/** Clase de tamaño de ventana actual (Material 3 Adaptive). */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    return currentWindowAdaptiveInfo().windowSizeClass
}
