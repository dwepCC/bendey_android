package com.bendey.restaurant.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.navigation.BendeyDrawerDestination
import com.bendey.restaurant.core.navigation.BendeyNavigationSuite
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.TopLevelDestination
import com.bendey.restaurant.core.navigation.canAccessRoute
import com.bendey.restaurant.core.navigation.navigateToBottomBarDestination
import com.bendey.restaurant.core.navigation.navigateToDrawerDestination
import com.bendey.restaurant.core.navigation.routeRequiredFeature
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bendey.restaurant.core.ui.components.BendeyAppHeader
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.BuildConfig
import com.bendey.restaurant.core.ui.cash.OpenCashSessionDialog
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
    val isTenantBound by appViewModel.isTenantBound.collectAsStateWithLifecycle()
    val isAuthenticated by appViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val rootNavController = rememberNavController()

    val tenantBound = isTenantBound
    val authenticated = isAuthenticated

    LaunchedEffect(authenticated, tenantBound) {
        if (tenantBound == true && authenticated == false) {
            rootNavController.navigate(BendeyRoutes.HOME) {
                popUpTo(rootNavController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val sessionReady = tenantBound != null && (tenantBound == false || authenticated != null)

    val startDestination = when {
        tenantBound == null -> null
        tenantBound == false -> BendeyRoutes.RUC
        authenticated == null -> null
        authenticated == false -> BendeyRoutes.HOME
        else -> BendeyRoutes.MAIN
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!sessionReady || startDestination == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BendeyColors.Rest900),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BendeyColors.OnPrimary)
            }
        } else {
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
                    val sessionKey by appViewModel.sessionKey.collectAsStateWithLifecycle()
                    key(sessionKey) {
                        MainShell(onShowMessage = onShowMessage)
                    }
                }
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
    cashSessionViewModel: AppCashSessionViewModel = hiltViewModel(),
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val headerState by headerViewModel.headerState.collectAsStateWithLifecycle()
    val permContext by sessionViewModel.permissionContext.collectAsStateWithLifecycle()
    val sessionKey by sessionViewModel.sessionKey.collectAsStateWithLifecycle()
    val cashState by cashSessionViewModel.state.collectAsStateWithLifecycle()

    if (permContext == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BendeyColors.Rest900),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = BendeyColors.OnPrimary)
        }
        return
    }

    val permissions = permContext ?: return

    val visibleBottomBar = remember(permissions.permissions, permissions.employeeType) {
        TopLevelDestination.bottomBarDestinations.filter {
            canAccessRoute(it.route, permissions.permissions, permissions.employeeType)
        }
    }
    val visibleDrawer = remember(permissions.permissions, permissions.employeeType) {
        BendeyDrawerDestination.entries.filter {
            canAccessRoute(it.route, permissions.permissions, permissions.employeeType)
        }
    }

    val mainStartRoute = remember(permissions.permissions, permissions.employeeType) {
        RestaurantPermissions.defaultRoute(permissions.permissions, permissions.employeeType)
    }

    LaunchedEffect(sessionKey) {
        if (sessionKey.isNullOrBlank() || permissions.permissions.isEmpty()) return@LaunchedEffect
        val start = RestaurantPermissions.defaultRoute(permissions.permissions, permissions.employeeType)
        mainNavController.navigate(start) {
            popUpTo(mainNavController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(currentRoute, permissions.permissions, permissions.employeeType) {
        if (permissions.permissions.isEmpty()) return@LaunchedEffect
        val route = currentRoute ?: return@LaunchedEffect
        if (!canAccessRoute(route, permissions.permissions, permissions.employeeType)) {
            mainNavController.navigate(mainStartRoute) {
                popUpTo(mainNavController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            BendeyRoutes.POS, BendeyRoutes.MESAS -> {
                if (routeRequiredFeature(currentRoute) != null) {
                    cashSessionViewModel.requireOpenSessionForOperation()
                }
            }
        }
        if (currentRoute?.startsWith("mesa/") == true) {
            cashSessionViewModel.requireOpenSessionForOperation()
        }
    }

    val cashCheckoutGate = remember(cashSessionViewModel) {
        CashCheckoutGate { cashSessionViewModel.ensureForCheckout() }
    }

    if (cashState.showOpenModal) {
        OpenCashSessionDialog(
            form = cashState.openForm,
            loading = cashState.opening,
            mandatory = cashState.mandatoryModal,
            error = cashState.error,
            onDismiss = cashSessionViewModel::dismissOpenModal,
            onConfirm = cashSessionViewModel::confirmOpenSession,
            onFormChange = cashSessionViewModel::setOpenForm,
        )
    }

    if (permissions.permissions.isEmpty()) {
        RestaurantNoAccessScreen(onLogout = { sessionViewModel.logout {} })
        return
    }

    BendeyNavigationSuite(
        currentRoute = currentRoute ?: mainStartRoute,
        appVersion = BuildConfig.VERSION_NAME,
        showBottomBar = BendeyRoutes.showsBottomBar(currentRoute),
        visibleBottomBarDestinations = visibleBottomBar,
        visibleDrawerDestinations = visibleDrawer,
        topBar = { toggleDrawer, drawerOpen ->
            if (BendeyRoutes.showsGlobalHeader(currentRoute)) {
                BendeyAppHeader(
                    state = headerState,
                    isDrawerOpen = drawerOpen,
                    onMenuClick = toggleDrawer,
                    onLogout = { sessionViewModel.logout {} },
                )
            }
        },
        onNavigate = { destination ->
            if (canAccessRoute(destination.route, permissions.permissions, permissions.employeeType)) {
                mainNavController.navigateToBottomBarDestination(destination.route)
            } else {
                onShowMessage("No tienes permiso para acceder a ${destination.label}")
            }
        },
        onDrawerNavigate = { destination ->
            if (canAccessRoute(destination.route, permissions.permissions, permissions.employeeType)) {
                mainNavController.navigateToDrawerDestination(destination.route)
            } else {
                onShowMessage("No tienes permiso para acceder a ${destination.label}")
            }
        },
        onDisabledDestinationClick = { destination ->
            onShowMessage("No tienes permiso para acceder a ${destination.label}")
        },
    ) { contentModifier ->
        NavHost(
            navController = mainNavController,
            startDestination = mainStartRoute,
            modifier = contentModifier.fillMaxSize(),
        ) {
            dashboardGraph(
                onOpenMesas = {
                    if (canAccessRoute(BendeyRoutes.MESAS, permissions.permissions, permissions.employeeType)) {
                        mainNavController.navigateToBottomBarDestination(BendeyRoutes.MESAS)
                    }
                },
                onOpenVentas = {
                    if (canAccessRoute(BendeyRoutes.VENTAS, permissions.permissions, permissions.employeeType)) {
                        mainNavController.navigateToDrawerDestination(BendeyRoutes.VENTAS)
                    }
                },
            )
            printingGraph(onBack = { mainNavController.popBackStack() })
            posGraph(
                cashCheckoutGate = cashCheckoutGate,
                onShowMessage = onShowMessage,
            )
            mesasGraph(
                navController = mainNavController,
                cashCheckoutGate = cashCheckoutGate,
                onShowMessage = onShowMessage,
            )
            cocinaGraph(onShowMessage = onShowMessage)
            cajaGraph(onShowMessage = onShowMessage)
            ventasGraph(onShowMessage = onShowMessage)
            productosGraph(
                onOpenModificadores = {
                    mainNavController.navigate(BendeyRoutes.MODIFICADORES) { launchSingleTop = true }
                },
                onOpenCombos = {
                    mainNavController.navigate(BendeyRoutes.COMBOS) { launchSingleTop = true }
                },
                onShowMessage = onShowMessage,
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
            clientesGraph(onShowMessage = onShowMessage)
        }
    }
}

@Composable
private fun RestaurantNoAccessScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BendeyColors.Rest900)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sin acceso al módulo restaurante",
            style = MaterialTheme.typography.titleLarge,
            color = BendeyColors.OnPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No tienes un perfil operativo asignado. Contacta al administrador para configurar tu tipo de empleado en el restaurante.",
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnPrimary.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BendeyPrimaryButton(
            text = "Cerrar sesión",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
