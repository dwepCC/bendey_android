package com.bendey.restaurant.feature.ayuda.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.ayuda.AyudaScreen

fun NavGraphBuilder.ayudaGraph(onBack: () -> Unit = {}) {
    composable(BendeyRoutes.AYUDA) {
        AyudaScreen(onBack = onBack)
    }
}
