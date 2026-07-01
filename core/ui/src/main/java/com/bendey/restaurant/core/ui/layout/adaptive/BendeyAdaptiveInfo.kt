package com.bendey.restaurant.core.ui.layout.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.bendey.restaurant.core.ui.layout.BendeyDeviceFormFactor

@Stable
object BendeyAdaptiveClassifier {

    fun classify(
        windowSizeClass: WindowSizeClass,
        screenWidthDp: Int,
        screenHeightDp: Int,
        smallestScreenWidthDp: Int,
    ): BendeyAdaptiveInfo {
        val isPhoneDevice = BendeyDeviceFormFactor.isPhone(smallestScreenWidthDp)
        val isPortrait = screenHeightDp >= screenWidthDp
        val widthTier = widthTier(windowSizeClass)

        val profile = when {
            isPhoneDevice -> if (isPortrait) {
                BendeyAdaptiveProfile.CompactPortrait
            } else {
                BendeyAdaptiveProfile.CompactLandscape
            }
            widthTier == BendeyWidthTier.Expanded -> BendeyAdaptiveProfile.Expanded
            widthTier == BendeyWidthTier.Medium -> if (isPortrait) {
                BendeyAdaptiveProfile.MediumPortrait
            } else {
                BendeyAdaptiveProfile.MediumLandscape
            }
            isPortrait -> BendeyAdaptiveProfile.CompactPortrait
            else -> BendeyAdaptiveProfile.CompactLandscape
        }

        return BendeyAdaptiveInfo(
            profile = profile,
            widthTier = widthTier,
            isPhoneDevice = isPhoneDevice,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
        )
    }

    private fun widthTier(windowSizeClass: WindowSizeClass): BendeyWidthTier = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            BendeyWidthTier.Expanded
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            BendeyWidthTier.Medium
        else -> BendeyWidthTier.Compact
    }
}

/** Perfil adaptive actual — API central para layouts, navegación y densidad. */
@Composable
fun rememberBendeyAdaptiveInfo(): BendeyAdaptiveInfo {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val configuration = LocalConfiguration.current
    return remember(
        windowSizeClass,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.smallestScreenWidthDp,
    ) {
        BendeyAdaptiveClassifier.classify(
            windowSizeClass = windowSizeClass,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        )
    }
}

@Composable
fun rememberBendeyAdaptiveProfile(): BendeyAdaptiveProfile =
    rememberBendeyAdaptiveInfo().profile
