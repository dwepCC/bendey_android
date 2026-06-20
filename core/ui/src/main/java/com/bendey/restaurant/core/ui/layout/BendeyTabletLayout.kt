package com.bendey.restaurant.core.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tokens tablet-first para grids operativos (8" / 10" / 12"). */
object BendeyTabletTokens {
    /** Columnas POS: mínimo 2 en móvil */
    fun productGridColumns(maxWidth: Dp): Int = when {
        maxWidth >= 1200.dp -> 5
        maxWidth >= 840.dp -> 4
        maxWidth >= 600.dp -> 3
        else -> 2
    }

    /** @deprecated usar [productGridColumns] */
    fun productGridMinSize(maxWidth: Dp): Dp = when {
        maxWidth >= 840.dp -> 148.dp
        maxWidth >= 600.dp -> 160.dp
        else -> 172.dp
    }

    /** Mesas operativas: 3 cols móvil, más en tablet (React SalasPage) */
    fun tableGridColumns(maxWidth: Dp): Int = when {
        maxWidth >= 1200.dp -> 5
        maxWidth >= 900.dp -> 4
        maxWidth >= 600.dp -> 3
        maxWidth >= 380.dp -> 3
        else -> 2
    }

    fun dashboardKpiColumns(maxWidth: Dp): Int = if (maxWidth >= 600.dp) 4 else 2

    fun dashboardActionColumns(maxWidth: Dp): Int = when {
        maxWidth >= 840.dp -> 3
        maxWidth >= 600.dp -> 3
        else -> 2
    }
}
