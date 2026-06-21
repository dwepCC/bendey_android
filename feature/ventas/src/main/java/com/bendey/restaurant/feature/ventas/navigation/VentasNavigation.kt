package com.bendey.restaurant.feature.ventas.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.ventas.VentasScreen

fun NavGraphBuilder.ventasGraph(onShowMessage: (String) -> Unit = {}) {
    composable(BendeyRoutes.VENTAS) {
        VentasScreen(onShowMessage = onShowMessage)
    }
}
