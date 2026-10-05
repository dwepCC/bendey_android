package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Barra de operación (R2b), con los verbos del salón y el mismo vocabulario que Tauri:
 * Hoy · Mesas · Vender · Cocina · Caja · Entregas. Qué ve cada rol lo decide [OperationNav].
 * Las rutas son las de siempre; solo cambian las etiquetas y dónde se muestra cada cosa.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean,
) {
    DASHBOARD(BendeyRoutes.DASHBOARD, "Hoy", "Hoy", Icons.Default.Home, true),
    MESAS(BendeyRoutes.MESAS, "Mesas", "Mesas", Icons.Default.GridView, true),
    POS(BendeyRoutes.POS, "Vender", "Vender", Icons.AutoMirrored.Filled.ReceiptLong, true),
    COCINA(BendeyRoutes.COCINA, "Cocina", "Cocina", Icons.AutoMirrored.Filled.Assignment, true),
    CAJA(BendeyRoutes.CAJA, "Caja", "Caja", Icons.Default.Wallet, true),
    ENTREGAS(BendeyRoutes.ENTREGAS, "Entregas", "Entregas", Icons.Default.DeliveryDining, true),
    VENTAS(BendeyRoutes.VENTAS, "Ventas", "Ventas", Icons.Default.ShoppingCart, false),
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", "Productos", Icons.Default.Inventory2, false),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", "Clientes", Icons.Default.People, false),
    ;

    companion object {
        /** Orden canónico de la barra de operación (el de Tauri). */
        val bottomBarDestinations = entries.filter { it.showInBottomBar }
        val managementDestinations = entries.filter { !it.showInBottomBar }
    }
}
