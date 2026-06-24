package com.bendey.restaurant.feature.modificadores.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.modificadores.ModificadoresScreen

fun NavGraphBuilder.modificadoresGraph(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenAreasPreparacion: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
) {
    composable(BendeyRoutes.MODIFICADORES) {
        ModificadoresScreen(
            onBack = onBack,
            onOpenProductos = onOpenProductos,
            onOpenAreasPreparacion = onOpenAreasPreparacion,
            onOpenCombos = onOpenCombos,
        )
    }
}
