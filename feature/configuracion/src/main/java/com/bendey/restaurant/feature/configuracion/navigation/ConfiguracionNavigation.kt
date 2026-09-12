package com.bendey.restaurant.feature.configuracion.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.configuracion.ConfiguracionScreen
import com.bendey.restaurant.feature.configuracion.perfil.PerfilScreen

fun NavGraphBuilder.configuracionGraph(
    onBack: () -> Unit = {},
    onOpenPrinting: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
) {
    composable(BendeyRoutes.CONFIGURACION) {
        ConfiguracionScreen(
            onBack = onBack,
            onOpenPrinting = onOpenPrinting,
            onNavigateToSubscription = onNavigateToSubscription,
        )
    }
}

fun NavGraphBuilder.perfilGraph(
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    composable(BendeyRoutes.PERFIL) {
        PerfilScreen(
            onBack = onBack,
            onShowMessage = onShowMessage,
        )
    }
}
