package com.bendey.restaurant.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bendey.restaurant.core.navigation.BendeyDrawerDestination
import com.bendey.restaurant.core.navigation.BendeyNavigationSuite
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.TopLevelDestination
import com.bendey.restaurant.core.ui.components.BendeyAppHeader
import com.bendey.restaurant.feature.auth.navigation.authGraph
import com.bendey.restaurant.feature.caja.navigation.cajaGraph
import com.bendey.restaurant.feature.cocina.navigation.cocinaGraph
import com.bendey.restaurant.feature.dashboard.navigation.dashboardGraph
import com.bendey.restaurant.feature.mesas.navigation.mesasGraph
import com.bendey.restaurant.feature.pos.navigation.posGraph
import com.bendey.restaurant.feature.printing.navigation.printingGraph
import com.bendey.restaurant.feature.clientes.navigation.clientesGraph
import com.bendey.restaurant.feature.combos.navigation.combosGraph
import com.bendey.restaurant.feature.configuracion.navigation.configuracionGraph
import com.bendey.restaurant.feature.modificadores.navigation.modificadoresGraph
import com.bendey.restaurant.feature.productos.navigation.productosGraph
import com.bendey.restaurant.feature.repartidores.navigation.repartidoresGraph
import com.bendey.restaurant.feature.ventas.navigation.ventasGraph

@Composable
fun BendeyAppNavHost(
    snackbarHostState: SnackbarHostState,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    appViewModel: AppSessionViewModel = hiltViewModel(),
) {
    val isTenantBound by appViewModel.isTenantBound.collectAsStateWithLifecycle(false)
    val isAuthenticated by appViewModel.isAuthenticated.collectAsStateWithLifecycle(false)
    val rootNavController = rememberNavController()

    val startDestination = when {
        !isTenantBound -> BendeyRoutes.RUC
        !isAuthenticated -> BendeyRoutes.HOME
        else -> BendeyRoutes.MAIN
    }

    Box(modifier = modifier.fillMaxSize()) {
        key(startDestination) {
            NavHost(
                navController = rootNavController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
                authGraph(
                    navController = rootNavController,
                    onAuthenticated = {
                        rootNavController.navigate(BendeyRoutes.MAIN) {
                            popUpTo(BendeyRoutes.HOME) { inclusive = true }
                        }
                    },
                )
                composable(BendeyRoutes.MAIN) {
                    MainShell(onShowMessage = onShowMessage)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun MainShell(
    onShowMessage: (String) -> Unit,
    headerViewModel: AppHeaderViewModel = hiltViewModel(),
    sessionViewModel: AppSessionViewModel = hiltViewModel(),
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val headerState by headerViewModel.headerState.collectAsStateWithLifecycle()

    BendeyNavigationSuite(
        currentRoute = currentRoute ?: BendeyRoutes.DASHBOARD,
        restaurantName = headerState.restaurantName,
        showBottomBar = BendeyRoutes.showsBottomBar(currentRoute),
        topBar = { toggleDrawer, drawerOpen ->
            if (BendeyRoutes.showsGlobalHeader(currentRoute)) {
                BendeyAppHeader(
                    state = headerState,
                    isDrawerOpen = drawerOpen,
                    onMenuClick = toggleDrawer,
                    onSwitchUser = { sessionViewModel.logout {} },
                    onLogout = { sessionViewModel.logout {} },
                )
            }
        },
        onNavigate = { destination ->
            mainNavController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(BendeyRoutes.DASHBOARD) { saveState = true }
            }
        },
        onDrawerNavigate = { destination ->
            mainNavController.navigate(destination.route) {
                launchSingleTop = true
            }
        },
        onDisabledDestinationClick = { destination ->
            onShowMessage("${destination.label} estará disponible próximamente")
        },
    ) { contentModifier ->
        NavHost(
            navController = mainNavController,
            startDestination = BendeyRoutes.DASHBOARD,
            modifier = contentModifier.fillMaxSize(),
        ) {
            dashboardGraph(
                onOpenMesas = {
                    mainNavController.navigate(BendeyRoutes.MESAS) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(BendeyRoutes.DASHBOARD) { saveState = true }
                    }
                },
                onOpenVentas = {
                    mainNavController.navigate(BendeyRoutes.VENTAS) { launchSingleTop = true }
                },
            )
            printingGraph(onBack = { mainNavController.popBackStack() })
            posGraph()
            mesasGraph(navController = mainNavController)
            cocinaGraph()
            cajaGraph()
            ventasGraph()
            productosGraph(
                onOpenModificadores = {
                    mainNavController.navigate(BendeyRoutes.MODIFICADORES) { launchSingleTop = true }
                },
                onOpenCombos = {
                    mainNavController.navigate(BendeyRoutes.COMBOS) { launchSingleTop = true }
                },
            )
            modificadoresGraph(
                onBack = { mainNavController.popBackStack() },
                onOpenProductos = {
                    mainNavController.navigate(TopLevelDestination.PRODUCTOS.route) { launchSingleTop = true }
                },
                onOpenCombos = {
                    mainNavController.navigate(BendeyRoutes.COMBOS) { launchSingleTop = true }
                },
            )
            combosGraph(
                onBack = { mainNavController.popBackStack() },
                onOpenProductos = {
                    mainNavController.navigate(TopLevelDestination.PRODUCTOS.route) { launchSingleTop = true }
                },
                onOpenModificadores = {
                    mainNavController.navigate(BendeyRoutes.MODIFICADORES) { launchSingleTop = true }
                },
            )
            configuracionGraph(
                onBack = { mainNavController.popBackStack() },
                onOpenPrinting = { mainNavController.navigate(BendeyRoutes.PRINTING_TEST) },
            )
            repartidoresGraph(onBack = { mainNavController.popBackStack() })
            clientesGraph()
        }
    }
}
