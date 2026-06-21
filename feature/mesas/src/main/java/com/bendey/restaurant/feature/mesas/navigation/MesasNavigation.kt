package com.bendey.restaurant.feature.mesas.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.CashCheckoutGateNoOp
import com.bendey.restaurant.feature.mesas.MesaScreen
import com.bendey.restaurant.feature.mesas.MesasAdminScreen
import com.bendey.restaurant.feature.mesas.MesasScreen
import com.bendey.restaurant.feature.mesas.MesasViewModel
import androidx.hilt.navigation.compose.hiltViewModel

private const val MESAS_REFRESH_KEY = "mesas_refresh"

fun NavGraphBuilder.mesasGraph(
    navController: NavHostController,
    cashCheckoutGate: CashCheckoutGate = CashCheckoutGateNoOp,
    onShowMessage: (String) -> Unit = {},
) {
    composable(BendeyRoutes.MESAS_ADMIN) {
        MesasAdminScreen(
            onOpenSession = { sessionId ->
                navController.navigate(BendeyRoutes.mesa(sessionId))
            },
        )
    }
    composable(BendeyRoutes.MESAS) { backStackEntry ->
        val viewModel: MesasViewModel = hiltViewModel()
        val shouldRefresh by backStackEntry.savedStateHandle
            .getStateFlow(MESAS_REFRESH_KEY, false)
            .collectAsStateWithLifecycle()
        LaunchedEffect(shouldRefresh) {
            if (shouldRefresh) {
                viewModel.refresh()
                backStackEntry.savedStateHandle[MESAS_REFRESH_KEY] = false
            }
        }
        MesasScreen(
            onOpenSession = { sessionId ->
                navController.navigate(BendeyRoutes.mesa(sessionId))
            },
            onShowMessage = onShowMessage,
            viewModel = viewModel,
        )
    }
    composable(
        route = BendeyRoutes.MESA,
        arguments = listOf(
            navArgument("sessionId") { type = NavType.IntType },
        ),
    ) {
        MesaScreen(
            onBack = { navController.popBackStack() },
            onCheckoutSuccess = {
                runCatching {
                    navController.getBackStackEntry(BendeyRoutes.MESAS)
                        .savedStateHandle[MESAS_REFRESH_KEY] = true
                }
                navController.popBackStack(BendeyRoutes.MESAS, inclusive = false)
            },
            cashCheckoutGate = cashCheckoutGate,
            onShowMessage = onShowMessage,
        )
    }
}
