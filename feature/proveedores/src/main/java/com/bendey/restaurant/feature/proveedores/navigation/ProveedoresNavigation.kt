package com.bendey.restaurant.feature.proveedores.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.proveedores.ProveedoresScreen

fun NavGraphBuilder.proveedoresGraph(onShowMessage: (String) -> Unit = {}) {
    composable(BendeyRoutes.PROVEEDORES) {
        ProveedoresScreen(onShowMessage = onShowMessage)
    }
}
