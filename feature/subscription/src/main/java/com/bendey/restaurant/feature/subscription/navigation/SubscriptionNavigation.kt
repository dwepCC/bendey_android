package com.bendey.restaurant.feature.subscription.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.subscription.SubscriptionScreen

fun NavGraphBuilder.subscriptionGraph(
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    composable(BendeyRoutes.SUSCRIPCION) {
        SubscriptionScreen(onBack = onBack, onShowMessage = onShowMessage)
    }
}
