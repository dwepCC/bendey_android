package com.bendey.restaurant.feature.pos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.pos.PosScreen

fun NavGraphBuilder.posGraph() {
    composable(BendeyRoutes.POS) {
        PosScreen()
    }
}
