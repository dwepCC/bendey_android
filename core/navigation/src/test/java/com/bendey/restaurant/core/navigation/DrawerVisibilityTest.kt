package com.bendey.restaurant.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** R2a: matriz rol x entrada visible del drawer + nombres de salón unificados. */
class DrawerVisibilityTest {
    private val admin = listOf("s.m", "c.v", "p.u", "t.v", "t.o", "o.c", "o.ch", "k.v", "g.p", "d.v")
    private val cashier = listOf("c.v", "p.u", "t.v", "o.ch")
    private val waiter = listOf("t.v", "t.o", "o.c")
    private val cook = listOf("k.v", "k.u")
    private val driver = listOf("d.v")

    private fun visible(perms: List<String>, type: String) =
        visibleDrawerDestinations(perms, type).toSet()

    @Test
    fun `mozo no ve suscripcion ni administracion`() {
        val v = visible(waiter, "waiter")
        assertFalse(BendeyDrawerDestination.SUSCRIPCION in v)
        assertFalse(BendeyDrawerDestination.CONFIGURACION in v)
        assertFalse(BendeyDrawerDestination.COMPRAS in v)
        assertFalse(BendeyDrawerDestination.MESAS_ADMIN in v)
    }

    @Test
    fun `suscripcion exige s punto m tambien por ruta`() {
        assertTrue(BendeyDrawerDestination.SUSCRIPCION in visible(admin, "admin"))
        listOf(cashier to "cashier", waiter to "waiter", cook to "cook", driver to "driver").forEach { (p, t) ->
            assertFalse(BendeyDrawerDestination.SUSCRIPCION in visible(p, t), t)
            assertFalse(canAccessRoute(BendeyRoutes.SUSCRIPCION, p, t), "ruta $t")
        }
        assertTrue(canAccessRoute(BendeyRoutes.SUSCRIPCION, admin, "admin"))
    }

    @Test
    fun `admin ve todo el drawer`() {
        assertEquals(BendeyDrawerDestination.entries.toSet(), visible(admin, "admin"))
    }

    @Test
    fun `cajero ve caja y ventas pero no administracion`() {
        val v = visible(cashier, "cashier")
        assertTrue(BendeyDrawerDestination.CAJA in v && BendeyDrawerDestination.VENTAS in v)
        assertFalse(BendeyDrawerDestination.CONFIGURACION in v)
        assertFalse(BendeyDrawerDestination.SUSCRIPCION in v)
        assertFalse(BendeyDrawerDestination.COMPRAS in v)
    }

    @Test
    fun `cocina solo ve impresoras`() {
        // La ayuda (R10.7) la ve cualquier puesto.
        assertEquals(setOf(BendeyDrawerDestination.IMPRESORAS, BendeyDrawerDestination.AYUDA), visible(cook, "cook"))
    }

    @Test
    fun `repartidor ve repartidores e impresoras`() {
        assertEquals(
            setOf(BendeyDrawerDestination.REPARTIDORES, BendeyDrawerDestination.IMPRESORAS, BendeyDrawerDestination.AYUDA),
            visible(driver, "driver"),
        )
    }

    @Test
    fun `clientes repartidores y compras ya no cuelgan de Mi carta`() {
        assertEquals("Mi carta", BendeyDrawerGroup.CATALOG.title)
        val inCatalog = BendeyDrawerDestination.entries.filter { it.group == BendeyDrawerGroup.CATALOG }
        assertEquals(listOf(BendeyDrawerDestination.PRODUCTOS), inCatalog)
        // Siguen alcanzables: cada destino pertenece a un grupo que el drawer dibuja.
        BendeyDrawerDestination.entries.forEach {
            assertTrue(it.group in BendeyDrawerDestination.groupedOrder, it.name)
        }
        assertEquals(BendeyDrawerGroup.OPERATION, BendeyDrawerDestination.CLIENTES.group)
        assertEquals(BendeyDrawerGroup.OPERATION, BendeyDrawerDestination.REPARTIDORES.group)
        assertEquals(BendeyDrawerGroup.PURCHASES, BendeyDrawerDestination.COMPRAS.group)
    }

    @Test
    fun `un solo nombre de salon, operacion Mesas y configuracion Salon y mesas`() {
        assertEquals("Mesas", TopLevelDestination.MESAS.label)
        assertEquals("Mesas", TopLevelDestination.MESAS.shortLabel)
        assertEquals("Salón y mesas", BendeyDrawerDestination.MESAS_ADMIN.label)
    }

    @Test
    fun `ningun texto visible del drawer usa terminos retirados`() {
        val retired = Regex(
            "\\b(salas?|configurar mesas|gesti[oó]n de mesas|pisos?|ambientes?)\\b",
            RegexOption.IGNORE_CASE,
        )
        val texts = BendeyDrawerDestination.entries.map { it.label } +
            BendeyDrawerGroup.entries.map { it.title } +
            TopLevelDestination.entries.flatMap { listOf(it.label, it.shortLabel) }
        texts.forEach { assertFalse(retired.containsMatchIn(it), "término retirado en «$it»") }
    }

    @Test
    fun `la ayuda la ve cualquier puesto`() {
        for ((p, t) in listOf(listOf("t.o") to "waiter", listOf("c.v") to "cashier", listOf("k.v") to "cook", listOf("d.v") to "driver", listOf("s.m") to "admin")) {
            assertTrue(BendeyDrawerDestination.AYUDA in visible(p, t), t)
        }
    }
}
