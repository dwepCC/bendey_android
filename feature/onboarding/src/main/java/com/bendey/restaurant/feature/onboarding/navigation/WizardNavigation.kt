package com.bendey.restaurant.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.designsystem.theme.BendeyExpressiveScope
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.onboarding.WizardScreen

fun NavGraphBuilder.wizardGraph(
    onExit: () -> Unit,
    onStartGuide: () -> Unit,
    onOpenProductos: () -> Unit,
) {
    composable(BendeyRoutes.WIZARD) {
        BendeyExpressiveScope {
            WizardScreen(
                onExit = onExit,
                onStartGuide = onStartGuide,
                onOpenProductos = onOpenProductos,
            )
        }
    }
}
