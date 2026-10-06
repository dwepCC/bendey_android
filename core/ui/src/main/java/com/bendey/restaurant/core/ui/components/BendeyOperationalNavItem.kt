package com.bendey.restaurant.core.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/** Destino de navegación operativa para [BendeyOperationalTopBar] (Inicio, POS, Mesas, Comandas). */
data class BendeyOperationalNavItem(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    /** D1: contador de pedidos por asignar sobre el ícono (null = sin globo). */
    val badge: BendeyNavBadge? = null,
)
