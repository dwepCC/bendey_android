package com.bendey.restaurant.feature.repartidores.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.repartidores.RepartidoresScreen

fun NavGraphBuilder.repartidoresGraph(onBack: () -> Unit = {}) {
    composable(BendeyRoutes.REPARTIDORES) {
        RepartidoresScreen(onBack = onBack)
    }
}
