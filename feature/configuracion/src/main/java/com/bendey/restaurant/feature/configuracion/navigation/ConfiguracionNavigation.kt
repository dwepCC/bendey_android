package com.bendey.restaurant.feature.configuracion.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.configuracion.ConfiguracionScreen

fun NavGraphBuilder.configuracionGraph(
    onBack: () -> Unit = {},
    onOpenPrinting: () -> Unit = {},
) {
    composable(BendeyRoutes.CONFIGURACION) {
        ConfiguracionScreen(onBack = onBack, onOpenPrinting = onOpenPrinting)
    }
}
