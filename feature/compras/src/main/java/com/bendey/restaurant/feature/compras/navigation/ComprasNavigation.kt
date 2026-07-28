package com.bendey.restaurant.feature.compras.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.compras.ComprasScreen

fun NavGraphBuilder.comprasGraph(onShowMessage: (String) -> Unit = {}) {
    composable(BendeyRoutes.COMPRAS) {
        ComprasScreen(onShowMessage = onShowMessage)
    }
}
