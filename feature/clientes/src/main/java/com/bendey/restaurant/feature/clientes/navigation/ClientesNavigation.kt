package com.bendey.restaurant.feature.clientes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.clientes.ClientesScreen

fun NavGraphBuilder.clientesGraph() {
    composable(BendeyRoutes.CLIENTES) {
        ClientesScreen()
    }
}
