package com.bendey.restaurant.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * R2b: matriz rol x entradas de primer nivel, grupos de Mi negocio, quiosco de cocina y que ningun destino
 * antiguo del drawer se pierde. Mismos conjuntos de permisos que `restaurantNavigation.test.ts` de Tauri.
 */
class OperationNavTest {
    private val admin = listOf("s.m", "c.v", "p.u", "t.v", "t.o", "o.c", "o.ch", "k.v", "k.u", "g.p", "d.v")
    private val cashier = listOf("c.v", "p.u", "t.v", "o.ch", "k.v")
    private val waiter = listOf("t.v", "t.o", "o.c")
    private val cook = listOf("k.v", "k.u")
    private val driver = listOf("d.v")

    private fun bar(p: List<String>, t: String) = OperationNav.operationBar(p, t)

    // ---------- barra de operación ----------

    @Test
    fun `mozo ve 1 entrada, Mesas`() =
        assertEquals(listOf(TopLevelDestination.MESAS), bar(waiter, "waiter"))

    @Test
    fun `cocina no tiene menu, solo la pantalla de Cocina en modo quiosco`() {
        assertEquals(listOf(TopLevelDestination.COCINA), bar(cook, "cook"))
        assertTrue(OperationNav.isKitchenKiosk(cook, "cook"))
        assertFalse(OperationNav.showsMiNegocio(cook, "cook"))
    }

    @Test
    fun `repartidor ve 1 entrada, Entregas`() =
        assertEquals(listOf(TopLevelDestination.ENTREGAS), bar(driver, "driver"))

    @Test
    fun `cajero ve 5 en el orden Hoy Mesas Vender Cocina Caja`() {
        assertEquals(
            listOf(
                TopLevelDestination.DASHBOARD, TopLevelDestination.MESAS, TopLevelDestination.POS,
                TopLevelDestination.COCINA, TopLevelDestination.CAJA,
            ),
            bar(cashier, "cashier"),
        )
        assertFalse(OperationNav.showsMiNegocio(cashier, "cashier"))
        assertFalse(OperationNav.isKitchenKiosk(cashier, "cashier"))
    }

    @Test
    fun `admin ve las mismas 5 mas Mi negocio, sin Entregas en la barra`() {
        assertEquals(bar(cashier, "cashier"), bar(admin, "admin"))
        assertFalse(TopLevelDestination.ENTREGAS in bar(admin, "admin"))
        assertTrue(OperationNav.showsMiNegocio(admin, "admin"))
        assertFalse(OperationNav.isKitchenKiosk(admin, "admin"))
    }

    @Test
    fun `los permisos no cambian, cada entrada exige el mismo permiso que su ruta`() {
        TopLevelDestination.bottomBarDestinations.forEach { d ->
            listOf(admin to "admin", cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver")
                .forEach { (p, t) ->
                    if (d in bar(p, t)) assertTrue(canAccessRoute(d.route, p, t), "${d.name} para $t")
                }
        }
    }

    @Test
    fun `vocabulario de la barra identico al de Tauri`() {
        val labels = TopLevelDestination.bottomBarDestinations.associate { it.name to it.label }
        assertEquals(
            mapOf(
                "DASHBOARD" to "Hoy", "MESAS" to "Mesas", "POS" to "Vender",
                "COCINA" to "Cocina", "CAJA" to "Caja", "ENTREGAS" to "Entregas",
            ),
            labels,
        )
        val old = Regex("^(inicio|dashboard|pos|comandas)$", RegexOption.IGNORE_CASE)
        TopLevelDestination.entries.forEach {
            assertFalse(old.matches(it.label) || old.matches(it.shortLabel), "nombre antiguo en ${it.name}")
        }
    }

    @Test
    fun `Vender queda como boton central y el resto se reparte a los lados`() {
        val l = BottomBarLayout.of(bar(cashier, "cashier"))
        assertEquals(TopLevelDestination.POS, l.center)
        assertEquals(listOf(TopLevelDestination.DASHBOARD, TopLevelDestination.MESAS), l.left)
        assertEquals(listOf(TopLevelDestination.COCINA, TopLevelDestination.CAJA), l.right)
        val mozo = BottomBarLayout.of(bar(waiter, "waiter"))
        assertNull(mozo.center)
        assertEquals(listOf(TopLevelDestination.MESAS), mozo.left)
    }

    // ---------- Mi negocio ----------

    @Test
    fun `grupos de Mi negocio con los nombres y el orden de Tauri`() {
        assertEquals(
            listOf("Mi carta", "Compras e insumos", "Ventas y comprobantes", "Clientes", "Salón y mesas", "Configuración", "Mi cuenta"),
            MiNegocioGroup.inOrder.map { it.title },
        )
        val grouped = MiNegocioCard.visibleGrouped(admin, "admin")
        assertEquals(MiNegocioGroup.inOrder, grouped.map { it.first })
    }

    @Test
    fun `solo quien administra ve Mi negocio y la ruta lo rebota al resto`() {
        listOf(cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver").forEach { (p, t) ->
            assertTrue(MiNegocioCard.visible(p, t).isEmpty(), t)
            assertFalse(canAccessRoute(BendeyRoutes.MI_NEGOCIO, p, t), t)
        }
        assertTrue(canAccessRoute(BendeyRoutes.MI_NEGOCIO, admin, "admin"))
        assertTrue(canAccessRoute(BendeyRoutes.MI_NEGOCIO, listOf("c.v"), "supervisor"))
    }

    @Test
    fun `cada tarjeta tiene descripcion y respeta el permiso de su ruta`() {
        MiNegocioCard.entries.forEach { assertTrue(it.description.isNotBlank(), it.name) }
        // Admin solo por PIN-like (sin g.p ni c.v): no ve tarjetas cuyo permiso no tiene.
        val soloAdmin = listOf("s.m")
        val v = MiNegocioCard.visible(soloAdmin, "admin")
        assertFalse(MiNegocioCard.PRODUCTOS in v)
        assertTrue(MiNegocioCard.CONFIGURACION in v && MiNegocioCard.MI_PLAN in v && MiNegocioCard.AYUDA in v)
        assertEquals(MiNegocioCard.entries.toSet(), MiNegocioCard.visible(admin, "admin").toSet())
    }

    @Test
    fun `Importar Transferencias y Suscripcion no son entradas de primer nivel`() {
        val names = MiNegocioCard.entries.map { it.title.lowercase() }
        assertFalse(names.any { "importar" in it || "transferencia" in it || "suscripci" in it })
        assertEquals("Mi plan", MiNegocioCard.MI_PLAN.title)
    }

    @Test
    fun `Modificadores y Areas de preparacion viven dentro de Mi carta`() {
        assertEquals(MiNegocioGroup.CARTA, MiNegocioCard.MODIFICADORES.group)
        assertEquals(MiNegocioGroup.CARTA, MiNegocioCard.AREAS_PREPARACION.group)
        assertEquals(MiNegocioGroup.COMPRAS, MiNegocioCard.COMPRAS.group)
        assertEquals(MiNegocioGroup.SALON, MiNegocioCard.SALON_Y_MESAS.group)
        assertEquals("Salón y mesas", MiNegocioCard.SALON_Y_MESAS.title)
    }

    // ---------- Mi cuenta ----------

    @Test
    fun `Ayuda la ve cualquier puesto, Mi plan solo el administrador`() {
        listOf(admin to "admin", cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver")
            .forEach { (p, t) -> assertTrue(AccountMenuEntry.AYUDA in AccountMenuEntry.visible(p, t), t) }
        assertTrue(AccountMenuEntry.MI_PLAN in AccountMenuEntry.visible(admin, "admin"))
        listOf(cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver").forEach { (p, t) ->
            assertFalse(AccountMenuEntry.MI_PLAN in AccountMenuEntry.visible(p, t), t)
            assertFalse(canAccessRoute(BendeyRoutes.SUSCRIPCION, p, t), t)
        }
    }

    @Test
    fun `el cajero conserva Ventas Reportes y Clientes en su menu, el admin los tiene en Mi negocio`() {
        val c = AccountMenuEntry.visible(cashier, "cashier")
        assertTrue(AccountMenuEntry.VENTAS in c && AccountMenuEntry.REPORTES in c && AccountMenuEntry.CLIENTES in c)
        val a = AccountMenuEntry.visible(admin, "admin")
        assertFalse(AccountMenuEntry.VENTAS in a || AccountMenuEntry.CLIENTES in a)
        // Mozo/cocina/repartidor: ni atajos de gestion.
        listOf(waiter to "waiter", cook to "cook", driver to "driver").forEach { (p, t) ->
            val m = AccountMenuEntry.visible(p, t)
            assertFalse(AccountMenuEntry.VENTAS in m || AccountMenuEntry.REPORTES in m || AccountMenuEntry.CLIENTES in m, t)
        }
    }

    @Test
    fun `Impresoras queda en el perfil de todos los puestos`() {
        listOf(admin to "admin", cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver")
            .forEach { (p, t) -> assertTrue(AccountMenuEntry.IMPRESORAS in AccountMenuEntry.visible(p, t), t) }
    }

    // ---------- no se pierde ningun destino antiguo ----------

    /** Las 13 entradas del drawer de R2a: cada una sigue alcanzable por rol desde su nueva ubicacion. */
    @Test
    fun `ningun destino antiguo del drawer se pierde`() {
        val oldRoutes = listOf(
            BendeyRoutes.CAJA, BendeyRoutes.VENTAS, BendeyRoutes.REPORTES, BendeyRoutes.CLIENTES,
            BendeyRoutes.REPARTIDORES, BendeyRoutes.PRODUCTOS, BendeyRoutes.COMPRAS, BendeyRoutes.PROVEEDORES,
            BendeyRoutes.MESAS_ADMIN, BendeyRoutes.PRINTING_TEST, BendeyRoutes.CONFIGURACION,
            BendeyRoutes.SUSCRIPCION, BendeyRoutes.AYUDA,
        )
        val roles = listOf(admin to "admin", cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver")
        roles.forEach { (p, t) ->
            val reachable = OperationNav.operationBar(p, t).map { it.route }.toSet() +
                MiNegocioCard.visible(p, t).map { it.route } +
                AccountMenuEntry.visible(p, t).map { it.route }
            // El repartidor ya no ve la pantalla administrativa de altas: su vista de reparto es Entregas
            // (la ruta sigue resolviendo por deep link, con el mismo permiso d.v).
            oldRoutes.filter { canAccessRoute(it, p, t) }
                .filterNot { it == BendeyRoutes.REPARTIDORES && !OperationNav.isBusinessManager(p, t) }
                .forEach {
                assertTrue(it in reachable, "$it ya no es alcanzable para $t")
            }
        }
    }

    @Test
    fun `las rutas antiguas siguen resolviendo y las nuevas existen`() {
        assertEquals("pos", BendeyRoutes.POS)
        assertEquals("cocina", BendeyRoutes.COCINA)
        assertEquals("dashboard", BendeyRoutes.DASHBOARD)
        assertEquals("mesas", BendeyRoutes.MESAS)
        assertEquals("entregas", BendeyRoutes.ENTREGAS)
        // Deep links: Repartidores (admin) y Entregas (d.v) conviven con el mismo permiso.
        assertTrue(canAccessRoute(BendeyRoutes.REPARTIDORES, driver, "driver"))
        assertTrue(canAccessRoute(BendeyRoutes.ENTREGAS, driver, "driver"))
        assertFalse(canAccessRoute(BendeyRoutes.ENTREGAS, waiter, "waiter"))
    }

    @Test
    fun `un solo nombre de salon y sin terminos retirados`() {
        assertEquals("Mesas", TopLevelDestination.MESAS.label)
        val retired = Regex("\\b(salas?|configurar mesas|gesti[oó]n de mesas|pisos?|ambientes?)\\b", RegexOption.IGNORE_CASE)
        val texts = MiNegocioCard.entries.flatMap { listOf(it.title, it.description) } +
            MiNegocioGroup.entries.map { it.title } + AccountMenuEntry.entries.map { it.label } +
            TopLevelDestination.entries.flatMap { listOf(it.label, it.shortLabel) }
        texts.forEach { assertFalse(retired.containsMatchIn(it), "término retirado en «$it»") }
    }
}
