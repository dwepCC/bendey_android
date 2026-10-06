package com.bendey.restaurant.core.navigation

import com.bendey.restaurant.core.ui.components.BendeyNavBadge
import com.bendey.restaurant.core.ui.components.BendeyOperationalNavItem
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveNavigationPolicy
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

fun TopLevelDestination.toOperationalNavItem(badge: BendeyNavBadge? = null): BendeyOperationalNavItem =
    BendeyOperationalNavItem(
        route = route,
        label = label,
        shortLabel = shortLabel,
        icon = icon,
        badge = badge,
    )

fun List<TopLevelDestination>.toOperationalNavItems(badges: Map<String, BendeyNavBadge> = emptyMap()): List<BendeyOperationalNavItem> =
    map { it.toOperationalNavItem(badges[it.route]) }

fun BendeyRoutes.showsOperationalTopBar(
    route: String?,
    profile: BendeyAdaptiveProfile,
    physicalPortrait: Boolean,
): Boolean = BendeyAdaptiveNavigationPolicy.shouldShowOperationalTopBar(
    showsGlobalHeader = showsGlobalHeader(route),
    profile = profile,
    physicalPortrait = physicalPortrait,
)
