package com.bendey.restaurant.core.ui.layout.adaptive

/**
 * Políticas de presentación de navegación por perfil adaptive.
 *
 * No usa [androidx.compose.material3.adaptive.navigation.NavigationSuiteScaffold]:
 * Bendey conserva navegación personalizada (BottomBar / TopBar / Drawer).
 */
object BendeyAdaptiveNavigationPolicy {

    /**
     * Chrome de navegación móvil: teléfono o tablet en vertical (evolución móvil POS).
     * Bottom bar + header compacto; sin top bar operativa en header.
     */
    fun usesMobileNavigationChrome(
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean,
    ): Boolean = profile.isCompact || (physicalPortrait && !profile.isCompact)

    fun shouldShowBottomNavigationBar(
        showBottomBarForRoute: Boolean,
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean,
    ): Boolean {
        if (!showBottomBarForRoute) return false
        return usesMobileNavigationChrome(profile, physicalPortrait)
    }

    /**
     * Top bar operativa con navegación segmentada — tablet landscape (Medium/Expanded).
     */
    fun shouldShowOperationalTopBar(
        showsGlobalHeader: Boolean,
        profile: BendeyAdaptiveProfile,
        physicalPortrait: Boolean,
    ): Boolean = showsGlobalHeader && !usesMobileNavigationChrome(profile, physicalPortrait)

    /** Rail deshabilitado en Fase 2B: una sola navegación primaria (TopBar + Drawer). */
    fun shouldShowNavigationRail(
        showBottomBarForRoute: Boolean,
        profile: BendeyAdaptiveProfile,
        isOperationalRoute: Boolean,
    ): Boolean = false

    /** Padding inferior de listas cuando la bottom bar móvil está visible. */
    fun shouldIncludeBottomBarScrollPadding(
        profile: BendeyAdaptiveProfile,
        showBottomBarForRoute: Boolean,
        physicalPortrait: Boolean,
    ): Boolean = showBottomBarForRoute && usesMobileNavigationChrome(profile, physicalPortrait)
}
