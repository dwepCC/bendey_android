package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navegación alineada con Capacitor:
 * - Barra inferior: operaciones diarias (Dashboard, POS, Mesas, Comandas)
 * - Gestión secundaria: Ventas, Caja, etc. desde Dashboard
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean,
) {
    DASHBOARD(BendeyRoutes.DASHBOARD, "Dashboard", "Inicio", Icons.Default.Home, true),
    POS(BendeyRoutes.POS, "POS", "POS", Icons.AutoMirrored.Filled.ReceiptLong, true),
    MESAS(BendeyRoutes.MESAS, "Mesas", "Mesas", Icons.Default.GridView, true),
    COCINA(BendeyRoutes.COCINA, "Comandas", "Comandas", Icons.AutoMirrored.Filled.Assignment, true),
    CAJA(BendeyRoutes.CAJA, "Caja", "Caja", Icons.Default.Wallet, false),
    VENTAS(BendeyRoutes.VENTAS, "Ventas", "Ventas", Icons.Default.ShoppingCart, false),
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", "Productos", Icons.Default.Inventory2, false),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", "Clientes", Icons.Default.People, false),
    ;

    companion object {
    val bottomBarDestinations = entries.filter { it.showInBottomBar }
    val bottomBarLeft = listOf(DASHBOARD, POS)
    val bottomBarCenter = MESAS
    val bottomBarRight = listOf(COCINA)
    val managementDestinations = entries.filter { !it.showInBottomBar }
    }
}
