package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyBottomNavigationBar
import com.bendey.restaurant.core.ui.components.BendeyNavItem
import com.bendey.restaurant.core.ui.layout.BendeyRestaurantShell
import kotlinx.coroutines.launch

@Composable
fun BendeyNavigationSuite(
    currentRoute: String?,
    restaurantName: String,
    onNavigate: (TopLevelDestination) -> Unit,
    onDrawerNavigate: (BendeyDrawerDestination) -> Unit,
    onDisabledDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable (toggleDrawer: () -> Unit, drawerOpen: Boolean) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isCompact = widthClass == WindowWidthSizeClass.COMPACT
    val leftItems = TopLevelDestination.bottomBarLeft.map {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }
    val centerItem = TopLevelDestination.bottomBarCenter.let {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }
    val rightItems = TopLevelDestination.bottomBarRight.map {
        BendeyNavItem(it.route, it.label, it.shortLabel, it.icon)
    }
    val railDestinations = TopLevelDestination.bottomBarDestinations

    fun toggleDrawer() {
        scope.launch {
            if (drawerState.isOpen) drawerState.close() else drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            BendeyNavigationDrawerContent(
                currentRoute = currentRoute,
                restaurantName = restaurantName,
                onNavigate = { dest ->
                    scope.launch { drawerState.close() }
                    onDrawerNavigate(dest)
                },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
        modifier = modifier,
    ) {
        BendeyRestaurantShell(
            topBar = {
                topBar(::toggleDrawer, drawerState.isOpen)
            },
            showBottomBar = showBottomBar && isCompact,
            bottomBar = {
                if (showBottomBar && isCompact) {
                    BendeyBottomNavigationBar(
                        currentRoute = currentRoute,
                        leftItems = leftItems,
                        centerItem = centerItem,
                        rightItems = rightItems,
                        onNavigate = { item ->
                            TopLevelDestination.bottomBarDestinations
                                .firstOrNull { it.route == item.route }
                                ?.let(onNavigate)
                        },
                        onMoreClick = ::toggleDrawer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) { innerModifier ->
            if (!isCompact) {
                Row(modifier = innerModifier.fillMaxSize()) {
                    NavigationRail(containerColor = BendeyColors.Surface) {
                        railDestinations.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = { onNavigate(destination) },
                                icon = {
                                    androidx.compose.material3.Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.shortLabel) },
                            )
                        }
                    }
                    content(Modifier.weight(1f))
                }
            } else {
                content(innerModifier)
            }
        }
    }
}
