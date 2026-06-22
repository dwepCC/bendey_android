package com.bendey.restaurant.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.auth.home.HomeScreen
import com.bendey.restaurant.feature.auth.login.EmailLoginScreen
import com.bendey.restaurant.feature.auth.pin.PinLoginScreen
import com.bendey.restaurant.feature.auth.register.RegisterScreen
import com.bendey.restaurant.feature.auth.register.RegisterSuccessScreen
import com.bendey.restaurant.feature.auth.welcome.WelcomeScreen

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthenticated: (initialRoute: String) -> Unit,
) {
    composable(BendeyRoutes.WELCOME) {
        WelcomeScreen(
            onBound = {
                navController.navigate(BendeyRoutes.HOME) {
                    popUpTo(BendeyRoutes.WELCOME) { inclusive = true }
                }
            },
            onCreateRestaurant = {
                navController.navigate(BendeyRoutes.REGISTER)
            },
        )
    }
    composable(BendeyRoutes.REGISTER) {
        RegisterScreen(
            onBack = { navController.popBackStack() },
            onRegistered = { restaurantName ->
                navController.navigate(BendeyRoutes.registerSuccess(restaurantName)) {
                    popUpTo(BendeyRoutes.WELCOME) { inclusive = false }
                }
            },
        )
    }
    composable(
        route = BendeyRoutes.REGISTER_SUCCESS,
        arguments = listOf(
            navArgument("restaurantName") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { entry ->
        RegisterSuccessScreen(
            restaurantName = entry.arguments?.getString("restaurantName").orEmpty(),
            onContinueToLogin = {
                navController.navigate(BendeyRoutes.HOME) {
                    popUpTo(BendeyRoutes.WELCOME) { inclusive = true }
                }
            },
        )
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
