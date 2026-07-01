package com.bendey.restaurant.core.navigation

import com.bendey.restaurant.core.ui.components.BendeyOperationalNavItem
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveNavigationPolicy
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

fun TopLevelDestination.toOperationalNavItem(): BendeyOperationalNavItem =
    BendeyOperationalNavItem(
        route = route,
        label = label,
        shortLabel = shortLabel,
        icon = icon,
    )

fun List<TopLevelDestination>.toOperationalNavItems(): List<BendeyOperationalNavItem> =
    map { it.toOperationalNavItem() }

fun BendeyRoutes.showsOperationalTopBar(
    route: String?,
    profile: BendeyAdaptiveProfile,
    physicalPortrait: Boolean,
): Boolean = BendeyAdaptiveNavigationPolicy.shouldShowOperationalTopBar(
    showsGlobalHeader = showsGlobalHeader(route),
    profile = profile,
    physicalPortrait = physicalPortrait,
)
