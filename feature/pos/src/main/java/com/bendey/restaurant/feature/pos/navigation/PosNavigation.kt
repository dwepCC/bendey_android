package com.bendey.restaurant.feature.pos.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.CashCheckoutGateNoOp
import com.bendey.restaurant.feature.pos.PosScreen

fun NavGraphBuilder.posGraph(
    cashCheckoutGate: CashCheckoutGate = CashCheckoutGateNoOp,
    onShowMessage: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
) {
    composable(BendeyRoutes.POS) {
        PosScreen(
            cashCheckoutGate = cashCheckoutGate,
            onShowMessage = onShowMessage,
            onNavigateToSubscription = onNavigateToSubscription,
        )
    }
}
