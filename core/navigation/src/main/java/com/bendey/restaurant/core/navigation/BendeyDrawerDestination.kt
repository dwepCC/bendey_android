package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.ui.graphics.vector.ImageVector

/** Secciones del drawer administrativo Bendey Resto. */
enum class BendeyDrawerGroup(val title: String) {
    OPERATION("Operación"),
    CATALOG("Catálogo"),
    CONFIGURATION("Configuración"),
}

/** Gestión — menú lateral exclusivamente administrativo. */
enum class BendeyDrawerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val group: BendeyDrawerGroup,
) {
    CAJA(BendeyRoutes.CAJA, "Caja", Icons.Default.Wallet, BendeyDrawerGroup.OPERATION),
    VENTAS(BendeyRoutes.VENTAS, "Ventas", Icons.Default.ShoppingCart, BendeyDrawerGroup.OPERATION),
    REPORTES(BendeyRoutes.REPORTES, "Reportes", Icons.Default.Assessment, BendeyDrawerGroup.OPERATION),
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", Icons.Default.Inventory2, BendeyDrawerGroup.CATALOG),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", Icons.Default.People, BendeyDrawerGroup.CATALOG),
    REPARTIDORES(BendeyRoutes.REPARTIDORES, "Repartidores", Icons.Default.DeliveryDining, BendeyDrawerGroup.CATALOG),
    MESAS_ADMIN(BendeyRoutes.MESAS_ADMIN, "Mesas", Icons.Default.Layers, BendeyDrawerGroup.CONFIGURATION),
    IMPRESORAS(BendeyRoutes.PRINTING_TEST, "Impresoras", Icons.Default.Print, BendeyDrawerGroup.CONFIGURATION),
    CONFIGURACION(BendeyRoutes.CONFIGURACION, "Configuración", Icons.Default.Settings, BendeyDrawerGroup.CONFIGURATION),
    ;

    companion object {
        val groupedOrder: List<BendeyDrawerGroup> = listOf(
            BendeyDrawerGroup.OPERATION,
            BendeyDrawerGroup.CATALOG,
            BendeyDrawerGroup.CONFIGURATION,
        )
    }
}
