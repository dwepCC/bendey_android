package com.bendey.restaurant.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import com.bendey.restaurant.core.ui.components.BendeySnackbarHost
import com.bendey.restaurant.core.ui.layout.rememberBendeySnackbarBottomPadding
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
import androidx.compose.runtime.SideEffect
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
import com.bendey.restaurant.core.navigation.AccountMenuEntry
import com.bendey.restaurant.core.navigation.MiNegocioCard
import com.bendey.restaurant.core.navigation.MiNegocioScreen
import com.bendey.restaurant.core.navigation.OperationNav
import com.bendey.restaurant.core.navigation.BendeyNavigationSuite
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.TopLevelDestination
import com.bendey.restaurant.core.navigation.canAccessRoute
import com.bendey.restaurant.core.navigation.navigateToBottomBarDestination
import com.bendey.restaurant.core.navigation.navigateToDrawerDestination
import com.bendey.restaurant.core.navigation.routeRequiredFeature
import com.bendey.restaurant.core.navigation.showsOperationalTopBar
import com.bendey.restaurant.core.navigation.toOperationalNavItems
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bendey.restaurant.core.ui.components.BendeyAppHeader
import com.bendey.restaurant.core.ui.components.BendeyKioskHeader
import com.bendey.restaurant.core.ui.components.BendeyUserMenuItem
import com.bendey.restaurant.core.ui.components.BendeyOperationalTopBar
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySplashScreen
import com.bendey.restaurant.BuildConfig
import com.bendey.restaurant.core.ui.cash.CashClosedBanner
import com.bendey.restaurant.core.ui.cash.CashStatusChip
import com.bendey.restaurant.core.ui.cash.CashStatusSheet
import com.bendey.restaurant.core.ui.cash.OpenCashSessionDialog
import com.bendey.restaurant.core.domain.cash.CashChipState
import com.bendey.restaurant.core.domain.cash.shouldShowClosedBanner
import java.text.NumberFormat
import java.util.Locale
import com.bendey.restaurant.feature.auth.navigation.authGraph
import com.bendey.restaurant.feature.caja.navigation.cajaGraph
import com.bendey.restaurant.feature.cocina.navigation.cocinaGraph
import com.bendey.restaurant.feature.dashboard.navigation.dashboardGraph
import com.bendey.restaurant.feature.mesas.navigation.mesasGraph
import com.bendey.restaurant.core.domain.onboarding.NextActionId
import com.bendey.restaurant.core.domain.onboarding.OnboardingDestination
import com.bendey.restaurant.feature.onboarding.FirstSaleNextSheet
import com.bendey.restaurant.feature.onboarding.WizardCoachPanel
import com.bendey.restaurant.feature.onboarding.WizardGateViewModel
import com.bendey.restaurant.feature.onboarding.navigation.wizardGraph
import com.bendey.restaurant.core.domain.onboarding.wizard.CoachTarget
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCoach
import com.bendey.restaurant.feature.pos.navigation.posGraph
import com.bendey.restaurant.feature.printing.navigation.printingGraph
import com.bendey.restaurant.feature.clientes.navigation.clientesGraph
import com.bendey.restaurant.feature.proveedores.navigation.proveedoresGraph
import com.bendey.restaurant.feature.compras.navigation.comprasGraph
import com.bendey.restaurant.feature.combos.navigation.combosGraph
import com.bendey.restaurant.feature.ayuda.navigation.ayudaGraph
import com.bendey.restaurant.feature.configuracion.navigation.configuracionGraph
import com.bendey.restaurant.feature.configuracion.navigation.perfilGraph
import com.bendey.restaurant.feature.areaspreparacion.navigation.areasPreparacionGraph
import com.bendey.restaurant.feature.modificadores.navigation.modificadoresGraph
import com.bendey.restaurant.feature.productos.navigation.productosGraph
import com.bendey.restaurant.feature.productos.navigation.reportesGraph
import com.bendey.restaurant.feature.repartidores.navigation.repartidoresGraph
import com.bendey.restaurant.feature.subscription.navigation.subscriptionGraph
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

    val registering by appViewModel.registrationInFlight.collectAsStateWithLifecycle()
    val tenantBound = isTenantBound
    val authenticated = isAuthenticated

    LaunchedEffect(authenticated, tenantBound, registering) {
        // Mientras se crea el restaurante (queda vinculado antes de iniciar sesión) no se cambia de pantalla.
        if (!registering && tenantBound == true && authenticated == false) {
            rootNavController.navigate(BendeyRoutes.HOME) {
                popUpTo(rootNavController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val sessionReady = tenantBound != null && (tenantBound == false || authenticated != null)

    val computedStart = when {
        tenantBound == null -> null
        tenantBound == false -> BendeyRoutes.WELCOME
        authenticated == null -> null
        authenticated == false -> BendeyRoutes.HOME
        else -> BendeyRoutes.MAIN
    }
    // Registro en curso: se conserva la raíz anterior hasta que termine (crear → entrar), porque
    // `key(startDestination)` destruiría la pantalla y su ViewModel a mitad del proceso.
    val lastStable = remember { arrayOfNulls<String>(1) }
    val startDestination = if (registering && lastStable[0] != null) lastStable[0] else computedStart
    SideEffect { if (!registering) lastStable[0] = computedStart }

    Box(modifier = modifier.fillMaxSize()) {
        if (!sessionReady || startDestination == null) {
            BendeySplashScreen(modifier = Modifier.fillMaxSize())
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
                        MainShell(
                            onShowMessage = onShowMessage,
                            snackbarHostState = snackbarHostState,
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MainShell(
    onShowMessage: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    headerViewModel: AppHeaderViewModel = hiltViewModel(),
    sessionViewModel: AppSessionViewModel = hiltViewModel(),
    cashSessionViewModel: AppCashSessionViewModel = hiltViewModel(),
    wizardGate: WizardGateViewModel = hiltViewModel(),
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val headerState by headerViewModel.headerState.collectAsStateWithLifecycle()
    val permContext by sessionViewModel.permissionContext.collectAsStateWithLifecycle()
    val sessionKey by sessionViewModel.sessionKey.collectAsStateWithLifecycle()
    val cashState by cashSessionViewModel.state.collectAsStateWithLifecycle()
    val showWizard by wizardGate.showWizard.collectAsStateWithLifecycle()

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

    // R2b: barra de operación por rol (mozo 1, cocina 0 = quiosco, repartidor 1, cajero 5, admin 5 + Mi negocio).
    val visibleBottomBar = remember(permissions.permissions, permissions.employeeType) {
        OperationNav.operationBar(permissions.permissions, permissions.employeeType)
    }
    val kioskMode = remember(permissions.permissions, permissions.employeeType) {
        OperationNav.isKitchenKiosk(permissions.permissions, permissions.employeeType)
    }
    val showMiNegocio = remember(permissions.permissions, permissions.employeeType) {
        OperationNav.showsMiNegocio(permissions.permissions, permissions.employeeType)
    }
    val accountEntries = remember(permissions.permissions, permissions.employeeType) {
        AccountMenuEntry.visible(permissions.permissions, permissions.employeeType)
    }

    val mainStartRoute = remember(permissions.permissions, permissions.employeeType) {
        RestaurantPermissions.defaultRoute(permissions.permissions, permissions.employeeType)
    }

    // Suscripción exige s.m (administrador). Avisos de plan visibles para todos no deben rebotar
    // en silencio contra el guard de ruta: explican por qué no se abre.
    // R10.1/R10.2: la campana abre la cola de pedidos del cliente por revisar (solo quien puede verla).
    var showPendingSheet by remember { mutableStateOf(false) }
    val canSeePending = PendingApprovalLogic.canView(permissions.permissions)
    val onBellClick: (() -> Unit)? = if (canSeePending) ({ showPendingSheet = true }) else null
    LaunchedEffect(canSeePending) {
        if (!canSeePending) return@LaunchedEffect
        headerViewModel.pendingArrivals.collect { table -> onShowMessage(PendingApprovalLogic.arrivedMessage(table)) }
    }
    if (showPendingSheet && canSeePending) {
        PendingApprovalSheet(
            permissions = permissions.permissions,
            onDismiss = { showPendingSheet = false },
            onShowMessage = onShowMessage,
        )
    }

    val goToSubscription: () -> Unit = {
        if (canAccessRoute(BendeyRoutes.SUSCRIPCION, permissions.permissions, permissions.employeeType)) {
            mainNavController.navigate(BendeyRoutes.SUSCRIPCION) { launchSingleTop = true }
        } else {
            onShowMessage("Solo el administrador puede ver la suscripción")
        }
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

    // R9: entrar a POS/Mesas/Mesa/Caja solo REFRESCA la lectura de caja (chip al día). Ya no abre un
    // diálogo obligatorio: la caja cerrada no impide armar pedidos, enviar comandas ni consultar.
    LaunchedEffect(currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        if (route == BendeyRoutes.POS || route == BendeyRoutes.MESAS || route == BendeyRoutes.CAJA ||
            route.startsWith("mesa/")
        ) {
            cashSessionViewModel.requireOpenSessionForOperation()
        }
    }

    val cashCurrency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val cashChipContent: (@Composable () -> Unit)? =
        if (cashState.chip == CashChipState.Hidden) {
            null
        } else {
            {
                CashStatusChip(
                    chip = cashState.chip,
                    soldNet = cashState.report?.totalNetSales,
                    currency = cashCurrency,
                    onClick = cashSessionViewModel::onChipClick,
                )
            }
        }
    val goToCash: () -> Unit = {
        cashSessionViewModel.closeSheet()
        if (canAccessRoute(BendeyRoutes.CAJA, permissions.permissions, permissions.employeeType)) {
            mainNavController.navigateToBottomBarDestination(BendeyRoutes.CAJA)
        } else {
            onShowMessage("No tienes permiso para acceder a Caja")
        }
    }
    if (cashState.sheetOpen) {
        CashStatusSheet(
            session = cashState.session,
            report = cashState.report,
            loading = cashState.reportLoading,
            error = cashState.reportError,
            currency = cashCurrency,
            onClose = goToCash,
            onPartialCount = goToCash,
            onDismiss = cashSessionViewModel::closeSheet,
            onRetry = cashSessionViewModel::loadReport,
        )
    }

    // Restaurante nuevo: el wizard se abre solo, una vez por sesión (el servidor decide).
    LaunchedEffect(showWizard) {
        if (showWizard) {
            wizardGate.consume()
            mainNavController.navigate(BendeyRoutes.WIZARD) { launchSingleTop = true }
        }
    }

    val cashCheckoutGate = remember(cashSessionViewModel) {
        CashCheckoutGate { cashSessionViewModel.ensureForCheckout() }
    }

    if (cashState.showOpenModal) {
        OpenCashSessionDialog(
            form = cashState.openForm,
            loading = cashState.opening,
            mandatory = false,
            error = cashState.error,
            onDismiss = cashSessionViewModel::dismissOpenModal,
            onConfirm = cashSessionViewModel::confirmOpenSession,
            onFormChange = cashSessionViewModel::setOpenForm,
            prefillAmount = cashState.openPrefill?.let { cashCurrency.format(it) },
            onOpenZero = cashSessionViewModel::openWithZero,
        )
    }

    if (permissions.permissions.isEmpty()) {
        RestaurantNoAccessScreen(onLogout = { sessionViewModel.logout {} })
        return
    }

    val adaptiveProfile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val showOperationalTopBar = BendeyRoutes.showsOperationalTopBar(
        currentRoute,
        adaptiveProfile,
        physicalPortrait,
    )
    val operationalNavItems = remember(visibleBottomBar) {
        visibleBottomBar.toOperationalNavItems()
    }
    val snackbarBottomPadding = rememberBendeySnackbarBottomPadding(
        currentRoute = currentRoute,
        showBottomBar = BendeyRoutes.showsBottomBar(currentRoute),
        profile = adaptiveProfile,
        physicalPortrait = physicalPortrait,
    )

    val openMiNegocio: (() -> Unit)? = if (showMiNegocio) {
        { mainNavController.navigate(BendeyRoutes.MI_NEGOCIO) { launchSingleTop = true } }
    } else {
        null
    }
    val openAyuda: () -> Unit = { mainNavController.navigate(BendeyRoutes.AYUDA) { launchSingleTop = true } }
    // Mi cuenta: Ayuda para TODOS los puestos, Impresoras, Mi plan (solo s.m) y los atajos del cajero.
    val userMenuItems = remember(accountEntries) {
        accountEntries.map { entry ->
            BendeyUserMenuItem(entry.label, entry.icon) {
                mainNavController.navigate(entry.route) { launchSingleTop = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    BendeyNavigationSuite(
        currentRoute = currentRoute ?: mainStartRoute,
        showBottomBar = BendeyRoutes.showsBottomBar(currentRoute) && !kioskMode,
        visibleBottomBarDestinations = visibleBottomBar,
        topBar = {
            when {
                // Cocina en modo quiosco: cabecera mínima (restaurante, conexión, Salir), sin menú ni avatar.
                kioskMode && BendeyRoutes.showsGlobalHeader(currentRoute) -> {
                    BendeyKioskHeader(
                        state = headerState,
                        onLogout = { sessionViewModel.logout {} },
                        onHelp = if (currentRoute == BendeyRoutes.AYUDA) null else openAyuda,
                    )
                }
                showOperationalTopBar -> {
                    BendeyOperationalTopBar(
                        state = headerState,
                        currentRoute = currentRoute,
                        operationalDestinations = operationalNavItems,
                        onMenuClick = openMiNegocio,
                        userMenuItems = userMenuItems,
                        onOperationalNavigate = { item ->
                            visibleBottomBar
                                .firstOrNull { it.route == item.route }
                                ?.let { destination ->
                                    if (canAccessRoute(
                                            destination.route,
                                            permissions.permissions,
                                            permissions.employeeType,
                                        )
                                    ) {
                                        mainNavController.navigateToBottomBarDestination(destination.route)
                                    } else {
                                        onShowMessage("No tienes permiso para acceder a ${destination.label}")
                                    }
                                }
                        },
                        onNotificationsClick = onBellClick,
                        onOpenProfile = { mainNavController.navigate(BendeyRoutes.PERFIL) { launchSingleTop = true } },
                        onLogout = { sessionViewModel.logout {} },
                        leadingActions = cashChipContent,
                    )
                }
                BendeyRoutes.showsGlobalHeader(currentRoute) -> {
                    BendeyAppHeader(
                        state = headerState,
                        onMenuClick = openMiNegocio,
                        userMenuItems = userMenuItems,
                        onNotificationsClick = onBellClick,
                        onOpenProfile = { mainNavController.navigate(BendeyRoutes.PERFIL) { launchSingleTop = true } },
                        onLogout = { sessionViewModel.logout {} },
                        leadingActions = cashChipContent,
                    )
                }
            }
        },
        onNavigate = { destination ->
            if (canAccessRoute(destination.route, permissions.permissions, permissions.employeeType)) {
                mainNavController.navigateToBottomBarDestination(destination.route)
            } else {
                onShowMessage("No tienes permiso para acceder a ${destination.label}")
            }
        },
    ) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
        // Franja de caja cerrada (solo con la caja CONFIRMADA cerrada y solo para quien puede abrirla):
        // avisa y ofrece [Abrir caja], pero no bloquea nada.
        if (shouldShowClosedBanner(cashState.chip) &&
            (currentRoute == BendeyRoutes.POS || currentRoute == BendeyRoutes.MESAS ||
                currentRoute?.startsWith("mesa/") == true)
        ) {
            CashClosedBanner(onOpen = cashSessionViewModel::requestOpen)
        }
        NavHost(
            navController = mainNavController,
            startDestination = mainStartRoute,
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
                onNavigateToSubscription = {
                    goToSubscription()
                },
                onOnboardingNavigate = { destination ->
                    mainNavController.navigateToOnboardingDestination(
                        destination,
                        permissions.permissions,
                        permissions.employeeType,
                        onShowMessage,
                    )
                },
            )
            printingGraph(onBack = { mainNavController.popBackStack() })
            wizardGraph(
                onExit = { mainNavController.popBackStack() },
                onStartGuide = {
                    WizardCoach.state.value.current?.target?.let { target ->
                        mainNavController.navigateToCoachTarget(
                            target, permissions.permissions, permissions.employeeType, onShowMessage,
                        )
                    }
                },
                onOpenProductos = {
                    mainNavController.navigateToDrawerDestination(BendeyRoutes.PRODUCTOS)
                },
            )
            posGraph(
                cashCheckoutGate = cashCheckoutGate,
                onShowMessage = onShowMessage,
                onNavigateToSubscription = {
                    goToSubscription()
                },
            )
            mesasGraph(
                navController = mainNavController,
                cashCheckoutGate = cashCheckoutGate,
                onShowMessage = onShowMessage,
            )
            cocinaGraph(onShowMessage = onShowMessage)
            cajaGraph(
                onShowMessage = onShowMessage,
                onNavigateToSubscription = {
                    goToSubscription()
                },
            )
            ventasGraph(
                onShowMessage = onShowMessage,
                onNavigateToSubscription = {
                    goToSubscription()
                },
                onGoToSell = {
                    mainNavController.navigate(BendeyRoutes.POS) { launchSingleTop = true }
                },
            )
            reportesGraph(
                onShowMessage = onShowMessage,
                onNavigateToSubscription = {
                    goToSubscription()
                },
            )
            productosGraph(
                onOpenModificadores = {
                    mainNavController.navigate(BendeyRoutes.MODIFICADORES) { launchSingleTop = true }
                },
                onOpenAreasPreparacion = {
                    mainNavController.navigate(BendeyRoutes.AREAS_PREPARACION) { launchSingleTop = true }
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
                onOpenAreasPreparacion = {
                    mainNavController.navigate(BendeyRoutes.AREAS_PREPARACION) { launchSingleTop = true }
                },
                onOpenCombos = {
                    mainNavController.navigate(BendeyRoutes.COMBOS) { launchSingleTop = true }
                },
            )
            areasPreparacionGraph(
                onBack = { mainNavController.popBackStack() },
                onOpenProductos = {
                    mainNavController.navigate(TopLevelDestination.PRODUCTOS.route) { launchSingleTop = true }
                },
                onOpenModificadores = {
                    mainNavController.navigate(BendeyRoutes.MODIFICADORES) { launchSingleTop = true }
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
                onOpenAreasPreparacion = {
                    mainNavController.navigate(BendeyRoutes.AREAS_PREPARACION) { launchSingleTop = true }
                },
            )
            configuracionGraph(
                onBack = { mainNavController.popBackStack() },
                onOpenPrinting = { mainNavController.navigate(BendeyRoutes.PRINTING_TEST) },
                onNavigateToSubscription = {
                    goToSubscription()
                },
                onOpenHelp = { mainNavController.navigate(BendeyRoutes.AYUDA) { launchSingleTop = true } },
            )
            ayudaGraph(onBack = { mainNavController.popBackStack() })
            perfilGraph(
                onBack = { mainNavController.popBackStack() },
                onShowMessage = onShowMessage,
            )
            repartidoresGraph(onBack = { mainNavController.popBackStack() })
            composable(BendeyRoutes.MI_NEGOCIO) {
                val groups = remember(permissions.permissions, permissions.employeeType) {
                    MiNegocioCard.visibleGrouped(permissions.permissions, permissions.employeeType)
                }
                MiNegocioScreen(
                    groups = groups,
                    onOpen = { card ->
                        mainNavController.navigate(card.route) { launchSingleTop = true }
                    },
                    onBack = { mainNavController.popBackStack() },
                )
            }
            clientesGraph(onShowMessage = onShowMessage)
            proveedoresGraph(onShowMessage = onShowMessage)
            comprasGraph(onShowMessage = onShowMessage)
            subscriptionGraph(
                onBack = { mainNavController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }
        }
    }
    WizardCoachPanel(
        onNavigate = { target ->
            mainNavController.navigateToCoachTarget(
                target, permissions.permissions, permissions.employeeType, onShowMessage,
            )
        },
        onOpenCash = cashSessionViewModel::openWithZero,
        onFinished = { onShowMessage("Hiciste tu primera venta. Ya puedes seguir con tu lista de primeros pasos.") },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = snackbarBottomPadding),
    )
    // R6: hoja "Lo que sigue" tras la primera venta (no se monta dentro del wizard).
    FirstSaleNextSheet(
        suppressed = currentRoute == BendeyRoutes.WIZARD,
        onAction = { id ->
            when (id) {
                NextActionId.TABLE_SALE -> mainNavController.navigateToCoachTarget(
                    CoachTarget.MESAS, permissions.permissions, permissions.employeeType, onShowMessage,
                )
                else -> mainNavController.navigateToOnboardingDestination(
                    when (id) {
                        NextActionId.PRINTER -> OnboardingDestination.IMPRESORAS
                        NextActionId.QR_MENU -> OnboardingDestination.CONFIG_MENU_DIGITAL
                        else -> OnboardingDestination.CONFIG_OPERACION
                    },
                    permissions.permissions,
                    permissions.employeeType,
                    onShowMessage,
                )
            }
        },
        onShowMessage = onShowMessage,
    )
    BendeySnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = snackbarBottomPadding),
    )
    // Aviso de vencimiento del plan: se monta a nivel de shell para que aparezca esté donde esté
    // el usuario, no solo si entra a la pantalla de suscripción.
    SubscriptionExpiryDialog(
        onGoToSubscription = { goToSubscription() },
    )
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
