package com.bendey.restaurant.feature.mesas.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.mesas.MesaScreen
import com.bendey.restaurant.feature.mesas.MesasAdminScreen
import com.bendey.restaurant.feature.mesas.MesasScreen

fun NavGraphBuilder.mesasGraph(navController: NavHostController) {
    composable(BendeyRoutes.MESAS_ADMIN) {
        MesasAdminScreen(
            onOpenSession = { sessionId ->
                navController.navigate(BendeyRoutes.mesa(sessionId))
            },
        )
    }
    composable(BendeyRoutes.MESAS) {
        MesasScreen(
            onOpenSession = { sessionId ->
                navController.navigate(BendeyRoutes.mesa(sessionId))
            },
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
                navController.popBackStack(BendeyRoutes.MESAS, inclusive = false)
            },
        )
    }
}
