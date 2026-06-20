package com.bendey.restaurant.feature.printing.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bendey.restaurant.core.navigation.BendeyRoutes
import com.bendey.restaurant.feature.printing.PrinterTestScreen

fun NavGraphBuilder.printingGraph(onBack: () -> Unit) {
    composable(BendeyRoutes.PRINTING_TEST) {
        PrinterTestScreen(onBack = onBack)
    }
}
