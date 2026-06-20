package com.bendey.restaurant.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.auth.home.HomeScreen
import com.bendey.restaurant.feature.auth.login.EmailLoginScreen
import com.bendey.restaurant.feature.auth.pin.PinLoginScreen
import com.bendey.restaurant.feature.auth.ruc.RucScreen

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthenticated: (initialRoute: String) -> Unit,
) {
    composable(BendeyRoutes.RUC) {
        RucScreen(onBound = {
            navController.navigate(BendeyRoutes.HOME) {
                popUpTo(BendeyRoutes.RUC) { inclusive = true }
            }
        })
    }
    composable(BendeyRoutes.HOME) {
        HomeScreen(
            onPinStation = { station ->
                navController.navigate(BendeyRoutes.pin(station.routeKey))
            },
            onAdminLogin = { navController.navigate(BendeyRoutes.LOGIN) },
        )
    }
    composable(
        route = BendeyRoutes.PIN,
        arguments = listOf(navArgument("station") { type = NavType.StringType }),
    ) {
        PinLoginScreen(
            onBack = { navController.popBackStack() },
            onAuthenticated = onAuthenticated,
        )
    }
    composable(BendeyRoutes.LOGIN) {
        EmailLoginScreen(
            onBack = { navController.popBackStack() },
            onAuthenticated = onAuthenticated,
        )
    }
}
