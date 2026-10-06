package com.bendey.restaurant.feature.repartidores.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.repartidores.EntregasScreen
import com.bendey.restaurant.feature.repartidores.RepartidoresScreen

fun NavGraphBuilder.repartidoresGraph(onBack: () -> Unit = {}, onShowMessage: (String) -> Unit = {}) {
    composable(BendeyRoutes.REPARTIDORES) {
        RepartidoresScreen(onBack = onBack, onShowMessage = onShowMessage)
    }
    composable(BendeyRoutes.ENTREGAS) {
        EntregasScreen(onShowMessage = onShowMessage)
    }
}
