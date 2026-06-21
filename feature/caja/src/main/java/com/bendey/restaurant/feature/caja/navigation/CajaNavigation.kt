package com.bendey.restaurant.feature.caja.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.caja.CajaScreen

fun NavGraphBuilder.cajaGraph(onShowMessage: (String) -> Unit = {}) {
    composable(BendeyRoutes.CAJA) {
        CajaScreen(onShowMessage = onShowMessage)
    }
}
