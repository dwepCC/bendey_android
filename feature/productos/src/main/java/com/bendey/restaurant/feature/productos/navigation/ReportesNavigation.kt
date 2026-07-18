package com.bendey.restaurant.feature.productos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.productos.reportes.ReportesScreen

fun NavGraphBuilder.reportesGraph(
    onShowMessage: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
) {
    composable(BendeyRoutes.REPORTES) {
        ReportesScreen(onShowMessage = onShowMessage, onNavigateToSubscription = onNavigateToSubscription)
    }
}
