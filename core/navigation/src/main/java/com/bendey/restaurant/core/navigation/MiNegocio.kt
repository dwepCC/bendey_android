package com.bendey.restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/** Grupos de "Mi negocio" (R2b), con los mismos nombres y orden que Tauri. */
enum class MiNegocioGroup(val title: String) {
    CARTA("Mi carta"),
    COMPRAS("Compras e insumos"),
    VENTAS("Ventas y comprobantes"),
    CLIENTES("Clientes"),
    SALON("Salón y mesas"),
    CONFIGURACION("Configuración"),
    CUENTA("Mi cuenta"),
    ;

    companion object {
        val inOrder: List<MiNegocioGroup> = entries.toList()
    }
}

/**
 * Tarjetas del índice "Mi negocio": reemplazan al drawer plano. Cada una abre una ruta que ya existía; el
 * permiso es el de la ruta ([canAccessRoute]), así que no cambia quién puede ver qué.
 * Importar y Transferencias no existen en Android; Suscripción solo vive en "Mi plan" (s.m).
 */
enum class MiNegocioCard(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val group: MiNegocioGroup,
) {
    PRODUCTOS(BendeyRoutes.PRODUCTOS, "Productos", "Platos y bebidas con su precio, categoría y stock.", Icons.Default.Inventory2, MiNegocioGroup.CARTA),
    COMBOS(BendeyRoutes.COMBOS, "Combos", "Menús y promociones armados con varios productos.", Icons.Default.ViewModule, MiNegocioGroup.CARTA),
    MODIFICADORES(BendeyRoutes.MODIFICADORES, "Modificadores", "Extras y opciones: término de cocción, sin cebolla, adicionales.", Icons.Default.Tune, MiNegocioGroup.CARTA),
    AREAS_PREPARACION(BendeyRoutes.AREAS_PREPARACION, "Áreas de preparación", "A qué cocina o barra va cada comanda.", Icons.Default.Restaurant, MiNegocioGroup.CARTA),
    COMPRAS(BendeyRoutes.COMPRAS, "Compras", "Registra lo que compras y sube el stock.", Icons.Default.ShoppingBag, MiNegocioGroup.COMPRAS),
    PROVEEDORES(BendeyRoutes.PROVEEDORES, "Proveedores", "Tus proveedores y sus datos de contacto.", Icons.Default.LocalShipping, MiNegocioGroup.COMPRAS),
    VENTAS(BendeyRoutes.VENTAS, "Ventas", "Boletas, facturas, notas de crédito y devoluciones.", Icons.Default.ShoppingCart, MiNegocioGroup.VENTAS),
    REPORTES(BendeyRoutes.REPORTES, "Reportes", "Ventas, cobros, productos y anulaciones.", Icons.Default.Assessment, MiNegocioGroup.VENTAS),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", "Tu libreta de clientes con documento y datos de contacto.", Icons.Default.People, MiNegocioGroup.CLIENTES),
    REPARTIDORES(BendeyRoutes.REPARTIDORES, "Repartidores y entregas", "Entregas en curso y altas de repartidores y empresas de delivery.", Icons.Default.DeliveryDining, MiNegocioGroup.CLIENTES),
    SALON_Y_MESAS(BendeyRoutes.MESAS_ADMIN, "Salón y mesas", "Crea y ordena las zonas y mesas de tu local.", Icons.Default.Layers, MiNegocioGroup.SALON),
    CONFIGURACION(BendeyRoutes.CONFIGURACION, "Configuración", "Personal y PINs, series, menú digital, caja, sucursales y más.", Icons.Default.Settings, MiNegocioGroup.CONFIGURACION),
    IMPRESORAS(BendeyRoutes.PRINTING_TEST, "Impresoras", "Conecta y prueba la impresora de tickets de este equipo.", Icons.Default.Print, MiNegocioGroup.CONFIGURACION),
    MI_PLAN(BendeyRoutes.SUSCRIPCION, "Mi plan", "Tu plan, comprobantes disponibles y pagos.", Icons.Default.WorkspacePremium, MiNegocioGroup.CUENTA),
    AYUDA(BendeyRoutes.AYUDA, "Ayuda", "Guías paso a paso y preguntas frecuentes.", Icons.AutoMirrored.Filled.HelpOutline, MiNegocioGroup.CUENTA),
    ;

    companion object {
        /** Tarjetas que el usuario puede abrir (mismo guard que la ruta). Vacío si no administra. */
        fun visible(permissions: List<String>, employeeType: String?): List<MiNegocioCard> {
            if (!OperationNav.showsMiNegocio(permissions, employeeType)) return emptyList()
            return entries.filter { canAccessRoute(it.route, permissions, employeeType) }
        }

        /** Agrupadas en el orden de [MiNegocioGroup], sin grupos vacíos. */
        fun visibleGrouped(
            permissions: List<String>,
            employeeType: String?,
        ): List<Pair<MiNegocioGroup, List<MiNegocioCard>>> {
            val cards = visible(permissions, employeeType)
            return MiNegocioGroup.inOrder.mapNotNull { g ->
                cards.filter { it.group == g }.takeIf { it.isNotEmpty() }?.let { g to it }
            }
        }
    }
}

/**
 * Menú del avatar ("Mi cuenta" para todos los puestos): Ayuda para TODOS, Impresoras en su perfil, Mi plan solo
 * para quien administra (s.m) y, para quien opera sin administrar (el cajero), los atajos que antes vivían en
 * el drawer: Ventas, Reportes y Clientes según su permiso.
 */
enum class AccountMenuEntry(val route: String, val label: String, val icon: ImageVector) {
    VENTAS(BendeyRoutes.VENTAS, "Ventas", Icons.Default.ShoppingCart),
    REPORTES(BendeyRoutes.REPORTES, "Reportes", Icons.Default.Assessment),
    CLIENTES(BendeyRoutes.CLIENTES, "Clientes", Icons.Default.People),
    IMPRESORAS(BendeyRoutes.PRINTING_TEST, "Impresoras", Icons.Default.Print),
    MI_PLAN(BendeyRoutes.SUSCRIPCION, "Mi plan", Icons.Default.WorkspacePremium),
    AYUDA(BendeyRoutes.AYUDA, "Ayuda", Icons.AutoMirrored.Filled.HelpOutline),
    ;

    companion object {
        fun visible(permissions: List<String>, employeeType: String?): List<AccountMenuEntry> {
            val manager = OperationNav.isBusinessManager(permissions, employeeType)
            return entries.filter { e ->
                when (e) {
                    VENTAS, REPORTES, CLIENTES ->
                        !manager && canAccessRoute(e.route, permissions, employeeType)
                    else -> canAccessRoute(e.route, permissions, employeeType)
                }
            }
        }
    }
}
