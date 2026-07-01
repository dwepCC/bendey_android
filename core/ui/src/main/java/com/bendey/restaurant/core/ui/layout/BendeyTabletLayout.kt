package com.bendey.restaurant.core.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptiveGrid
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/**
 * Tokens tablet-first para grids operativos.
 * Delega en [AdaptiveGrid] — pasar siempre [BendeyAdaptiveProfile] desde [rememberBendeyAdaptiveProfile].
 */
object BendeyTabletTokens {

    fun posProductGridColumns(profile: BendeyAdaptiveProfile, maxWidth: Dp): Int =
        AdaptiveGrid.posProductColumns(profile, maxWidth.value.toInt())

    fun productGridColumns(profile: BendeyAdaptiveProfile, maxWidth: Dp): Int =
        AdaptiveGrid.catalogProductColumns(profile, maxWidth.value.toInt())

    fun tableGridColumns(profile: BendeyAdaptiveProfile): Int =
        AdaptiveGrid.tableGridColumns(profile)

    fun dashboardKpiColumns(profile: BendeyAdaptiveProfile): Int =
        AdaptiveGrid.dashboardKpiColumns(profile)

    /** @deprecated usar [productGridColumns] con perfil adaptive */
    fun productGridMinSize(maxWidth: Dp): Dp = when {
        maxWidth >= 840.dp -> 148.dp
        maxWidth >= 600.dp -> 160.dp
        else -> 172.dp
    }

    fun dashboardActionColumns(maxWidth: Dp): Int = when {
        maxWidth >= 840.dp -> 3
        maxWidth >= 600.dp -> 3
        else -> 2
    }
}
