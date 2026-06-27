package com.bendey.restaurant.feature.dashboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.designsystem.theme.BendeyExpressiveScope
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.dashboard.DashboardScreen

fun NavGraphBuilder.dashboardGraph(
    onOpenMesas: () -> Unit = {},
    onOpenVentas: () -> Unit = {},
) {
    composable(BendeyRoutes.DASHBOARD) {
        BendeyExpressiveScope {
            DashboardScreen(
                onOpenMesas = onOpenMesas,
                onOpenVentas = onOpenVentas,
            )
        }
    }
}
