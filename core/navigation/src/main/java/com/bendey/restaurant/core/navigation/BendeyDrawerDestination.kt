package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.ui.graphics.vector.ImageVector

/** Gestión — menú lateral (React `ResponsiveMenu` / dropdown Gestión). */
enum class BendeyDrawerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    CAJA(BendeyRoutes.CAJA, "Caja", Icons.Default.Wallet),
    VENTAS(BendeyRoutes.VENTAS, "Ventas", Icons.Default.ShoppingCart),
    MESAS_ADMIN(BendeyRoutes.MESAS_ADMIN, "Configurar mesas", Icons.Default.Layers),
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", Icons.Default.Inventory2),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", Icons.Default.People),
    CONFIGURACION(BendeyRoutes.CONFIGURACION, "Configuración", Icons.Default.Settings),
    REPARTIDORES(BendeyRoutes.REPARTIDORES, "Repartidores", Icons.Default.DeliveryDining),
    IMPRESORAS(BendeyRoutes.PRINTING_TEST, "Impresoras", Icons.Default.Print),
}
