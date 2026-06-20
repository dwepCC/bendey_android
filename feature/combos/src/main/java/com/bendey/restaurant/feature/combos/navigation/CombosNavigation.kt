package com.bendey.restaurant.feature.combos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.combos.CombosScreen

fun NavGraphBuilder.combosGraph(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenModificadores: () -> Unit = {},
) {
    composable(BendeyRoutes.COMBOS) {
        CombosScreen(
            onBack = onBack,
            onOpenProductos = onOpenProductos,
            onOpenModificadores = onOpenModificadores,
        )
    }
}
