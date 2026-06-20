package com.bendey.restaurant.feature.productos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.productos.ProductosScreen

fun NavGraphBuilder.productosGraph(
    onOpenModificadores: () -> Unit = {},
    onOpenCombos: () -> Unit = {},
) {
    composable(BendeyRoutes.PRODUCTOS) {
        ProductosScreen(
            onOpenModificadores = onOpenModificadores,
            onOpenCombos = onOpenCombos,
        )
    }
}
