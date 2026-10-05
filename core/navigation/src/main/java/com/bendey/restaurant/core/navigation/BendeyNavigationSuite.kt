package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.ui.components.BendeyBottomNavigationBar
import com.bendey.restaurant.core.ui.components.BendeyNavItem
import com.bendey.restaurant.core.ui.components.BendeyScrollHintProvider
import com.bendey.restaurant.core.ui.layout.BendeyRestaurantShell
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveNavigationPolicy
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait

/**
 * Cascarón de navegación (R2b): barra de operación por rol + contenido. Ya no hay drawer: la gestión vive
 * en la pantalla «Mi negocio». En teléfono la barra inferior lleva hasta 5 entradas (Vender como botón
 * central); en tablet la misma lista va en la barra superior ([BendeyRoutes.showsOperationalTopBar]).
 */
@Composable
fun BendeyNavigationSuite(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    visibleBottomBarDestinations: List<TopLevelDestination> = TopLevelDestination.bottomBarDestinations,
    topBar: @Composable () -> Unit = {},
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val showBottomNavigation = BendeyAdaptiveNavigationPolicy.shouldShowBottomNavigationBar(
        showBottomBarForRoute = showBottomBar,
        profile = profile,
        physicalPortrait = physicalPortrait,
    )
    val layout = BottomBarLayout.of(visibleBottomBarDestinations)
    fun TopLevelDestination.toItem() = BendeyNavItem(route, label, shortLabel, icon)

    BendeyScrollHintProvider {
        BendeyRestaurantShell(
            topBar = topBar,
            showBottomBar = showBottomNavigation,
            modifier = modifier,
            bottomBar = {
                if (showBottomNavigation && visibleBottomBarDestinations.isNotEmpty()) {
                    BendeyBottomNavigationBar(
                        currentRoute = currentRoute,
                        leftItems = layout.left.map { it.toItem() },
                        centerItem = layout.center?.toItem(),
                        showCenterFab = layout.center != null,
                        rightItems = layout.right.map { it.toItem() },
                        onNavigate = { item ->
                            visibleBottomBarDestinations
                                .firstOrNull { it.route == item.route }
                                ?.let(onNavigate)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) { innerModifier ->
            content(innerModifier)
        }
    }
}

/**
 * Reparte las entradas visibles en el hueco izquierdo, el botón central (Vender) y el derecho, respetando el
 * orden canónico Hoy · Mesas · Vender · Cocina · Caja · Entregas. Sin Vender (mozo, repartidor) todas van a
 * la izquierda y no hay botón central.
 */
internal data class BottomBarLayout(
    val left: List<TopLevelDestination>,
    val center: TopLevelDestination?,
    val right: List<TopLevelDestination>,
) {
    companion object {
        fun of(visible: List<TopLevelDestination>): BottomBarLayout {
            val center = visible.firstOrNull { it == TopLevelDestination.POS }
            if (center == null) return BottomBarLayout(visible, null, emptyList())
            val order = TopLevelDestination.bottomBarDestinations
            val posIndex = order.indexOf(center)
            return BottomBarLayout(
                left = visible.filter { order.indexOf(it) < posIndex },
                center = center,
                right = visible.filter { order.indexOf(it) > posIndex },
            )
        }
    }
}
