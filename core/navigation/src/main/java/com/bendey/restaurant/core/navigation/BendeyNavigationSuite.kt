package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
import com.bendey.restaurant.core.ui.components.BendeyBottomNavigationBar
import com.bendey.restaurant.core.ui.components.BendeyNavItem
import com.bendey.restaurant.core.ui.components.BendeyScrollHintProvider
import com.bendey.restaurant.core.ui.layout.BendeyRestaurantShell
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveNavigationPolicy
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import kotlinx.coroutines.launch

@Composable
fun BendeyNavigationSuite(
    currentRoute: String?,
    appVersion: String,
    headerState: BendeyAppHeaderState,
    onNavigate: (TopLevelDestination) -> Unit,
    onDrawerNavigate: (BendeyDrawerDestination) -> Unit,
    onDisabledDestinationClick: (TopLevelDestination) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    visibleBottomBarDestinations: List<TopLevelDestination> = TopLevelDestination.bottomBarDestinations,
    visibleDrawerDestinations: List<BendeyDrawerDestination> = BendeyDrawerDestination.entries,
    topBar: @Composable (toggleDrawer: () -> Unit, drawerOpen: Boolean) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val showBottomNavigation = BendeyAdaptiveNavigationPolicy.shouldShowBottomNavigationBar(
        showBottomBarForRoute = showBottomBar,
        profile = profile,
        physicalPortrait = physicalPortrait,
    )
    val leftItems = visibleBottomBarDestinations.filter { it in TopLevelDestination.bottomBarLeft }.map {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }
    val centerDest = visibleBottomBarDestinations.firstOrNull { it == TopLevelDestination.bottomBarCenter }
    val showPosFab = centerDest != null && TopLevelDestination.POS in visibleBottomBarDestinations
    val centerItem = centerDest?.let {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }
    val rightItems = visibleBottomBarDestinations.filter { it in TopLevelDestination.bottomBarRight }.map {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }

    fun toggleDrawer() {
        scope.launch {
            if (drawerState.isOpen) drawerState.close() else drawerState.open()
        }
    }

    BendeyScrollHintProvider {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                BendeyNavigationDrawerContent(
                    currentRoute = currentRoute,
                    appVersion = appVersion,
                    headerState = headerState,
                    destinations = visibleDrawerDestinations,
                    onNavigate = { dest ->
                        scope.launch { drawerState.close() }
                        onDrawerNavigate(dest)
                    },
                    onClose = { scope.launch { drawerState.close() } },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                )
            },
            modifier = modifier,
        ) {
            BendeyRestaurantShell(
                topBar = {
                    topBar(::toggleDrawer, drawerState.isOpen)
                },
                showBottomBar = showBottomNavigation,
                bottomBar = {
                    if (showBottomNavigation && visibleBottomBarDestinations.isNotEmpty()) {
                        BendeyBottomNavigationBar(
                            currentRoute = currentRoute,
                            leftItems = leftItems,
                            centerItem = centerItem,
                            showCenterFab = showPosFab,
                            rightItems = rightItems,
                            onNavigate = { item ->
                                visibleBottomBarDestinations
                                    .firstOrNull { it.route == item.route }
                                    ?.let(onNavigate)
                            },
                            onMoreClick = ::toggleDrawer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            ) { innerModifier ->
                content(innerModifier)
            }
        }
    }
}
