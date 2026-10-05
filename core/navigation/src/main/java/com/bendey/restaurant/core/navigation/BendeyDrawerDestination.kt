package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/** Secciones del drawer administrativo Bendey Resto. */
enum class BendeyDrawerGroup(val title: String) {
    OPERATION("Operación"),
    CATALOG("Mi carta"),
    PURCHASES("Compras e insumos"),
    CONFIGURATION("Configuración"),
    HELP("Ayuda"),
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
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", Icons.Default.People, BendeyDrawerGroup.OPERATION),
    REPARTIDORES(BendeyRoutes.REPARTIDORES, "Repartidores", Icons.Default.DeliveryDining, BendeyDrawerGroup.OPERATION),
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", Icons.Default.Inventory2, BendeyDrawerGroup.CATALOG),
    COMPRAS(BendeyRoutes.COMPRAS, "Compras", Icons.Default.ShoppingBag, BendeyDrawerGroup.PURCHASES),
    PROVEEDORES(BendeyRoutes.PROVEEDORES, "Proveedores", Icons.Default.LocalShipping, BendeyDrawerGroup.PURCHASES),
    MESAS_ADMIN(BendeyRoutes.MESAS_ADMIN, "Salón y mesas", Icons.Default.Layers, BendeyDrawerGroup.CONFIGURATION),
    IMPRESORAS(BendeyRoutes.PRINTING_TEST, "Impresoras", Icons.Default.Print, BendeyDrawerGroup.CONFIGURATION),
    CONFIGURACION(BendeyRoutes.CONFIGURACION, "Configuración", Icons.Default.Settings, BendeyDrawerGroup.CONFIGURATION),
    SUSCRIPCION(BendeyRoutes.SUSCRIPCION, "Suscripción", Icons.Default.WorkspacePremium, BendeyDrawerGroup.CONFIGURATION),
    AYUDA(BendeyRoutes.AYUDA, "Ayuda", Icons.AutoMirrored.Filled.HelpOutline, BendeyDrawerGroup.HELP),
    ;

    companion object {
        val groupedOrder: List<BendeyDrawerGroup> = listOf(
            BendeyDrawerGroup.OPERATION,
            BendeyDrawerGroup.CATALOG,
            BendeyDrawerGroup.PURCHASES,
            BendeyDrawerGroup.CONFIGURATION,
            BendeyDrawerGroup.HELP,
        )
    }
}
