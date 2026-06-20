package com.bendey.restaurant.feature.cocina.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.cocina.CocinaScreen

fun NavGraphBuilder.cocinaGraph() {
    composable(BendeyRoutes.COCINA) {
        CocinaScreen()
    }
}
