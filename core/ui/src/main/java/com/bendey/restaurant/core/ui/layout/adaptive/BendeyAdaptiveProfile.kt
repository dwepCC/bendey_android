package com.bendey.restaurant.core.ui.layout.adaptive

/**
 * Perfil UX global Bendey Resto — derivado de [WindowSizeClass] + orientación + forma del dispositivo.
 *
 * Teléfonos (smallestWidth &lt; 600) siempre mapean a [CompactPortrait] o [CompactLandscape]
 * para preservar paridad visual con la experiencia móvil actual.
 */
enum class BendeyAdaptiveProfile {
    CompactPortrait,
    CompactLandscape,
    MediumPortrait,
    MediumLandscape,
    Expanded,
    ;

    val isCompact: Boolean
        get() = this == CompactPortrait || this == CompactLandscape

    val isMedium: Boolean
        get() = this == MediumPortrait || this == MediumLandscape

    val isExpanded: Boolean
        get() = this == Expanded

    val isPortrait: Boolean
        get() = this == CompactPortrait || this == MediumPortrait

    val isLandscape: Boolean
        get() = this == CompactLandscape || this == MediumLandscape
}

enum class BendeyWidthTier {
    Compact,
    Medium,
    Expanded,
}

data class BendeyAdaptiveInfo(
    val profile: BendeyAdaptiveProfile,
    val widthTier: BendeyWidthTier,
    val isPhoneDevice: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
)
