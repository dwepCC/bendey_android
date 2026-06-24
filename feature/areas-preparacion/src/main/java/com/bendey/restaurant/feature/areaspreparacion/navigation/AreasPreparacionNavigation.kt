package com.bendey.restaurant.feature.areaspreparacion.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.areaspreparacion.AreasPreparacionScreen

fun NavGraphBuilder.areasPreparacionGraph(
    onBack: () -> Unit = {},
    onOpenProductos: () -> Unit = {},
    onOpenModificadores: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
) {
    composable(BendeyRoutes.AREAS_PREPARACION) {
        AreasPreparacionScreen(
            onBack = onBack,
            onOpenProductos = onOpenProductos,
            onOpenModificadores = onOpenModificadores,
            onOpenCombos = onOpenCombos,
        )
    }
}
