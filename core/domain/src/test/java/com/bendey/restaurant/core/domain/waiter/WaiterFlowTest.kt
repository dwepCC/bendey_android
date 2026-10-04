package com.bendey.restaurant.core.domain.waiter

import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class WaiterFlowTest {

    private fun product(id: Int = 1) = PosProduct(
        id = id, code = "P$id", name = "Plato $id", salePrice = 10.0,
        categoryId = null, imageUrl = null, igvAffectationType = "10", priceIncludesIgv = true,
    )

    private fun line(id: Int = 1, qty: Int = 1, notes: String = "", kind: String = "product") =
        PosCartLine(product = product(id), quantity = qty, notes = notes, itemKind = kind)

    private fun comanda(cancelled: Boolean = false) = SessionComandaSummary(
        id = 1, productName = "Plato", quantity = 1.0, unitPrice = 10.0,
        status = ComandaStatus.PENDIENTE, notes = null,
        cancelledAt = if (cancelled) "2026-10-04T10:00:00" else null,
    )

    private fun session(
        tableName: String? = "Mesa 1",
        total: Double = 0.0,
        comandas: List<SessionComandaSummary> = emptyList(),
    ) = TableSessionDetail(
        id = 9, tableName = tableName, floorName = "Salon", waiterName = "Ana", guests = 2,
        orderCode = "A1", totalAmount = total,
        orders = if (comandas.isEmpty()) emptyList() else listOf(SessionOrderSummary(1, 1, comandas)),
    )

    // --- Guard de envio -------------------------------------------------------------------------

    @Test fun guard_segundoToqueNoPasa() {
        val guard = SyncGuard()
        assertTrue(guard.tryAcquire())
        assertFalse(guard.tryAcquire())
        guard.release()
        assertTrue(guard.tryAcquire())
    }

    @Test fun guard_conHilosConcurrentesSoloUnoGana() {
        val guard = SyncGuard()
        val winners = AtomicInteger(0)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        repeat(8) {
            pool.execute {
                start.await()
                if (guard.tryAcquire()) winners.incrementAndGet()
            }
        }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(1, winners.get())
    }

    // --- Idempotency-Key ------------------------------------------------------------------------

    @Test fun key_mismoCarritoReutilizaLaKeyEnReintentos() {
        var n = 0
        val holder = IdempotencyKeyHolder { "key-${++n}" }
        val sig = cartSignature(listOf(line(1, 2)))
        val first = holder.keyFor(sig)
        assertEquals(first, holder.keyFor(sig))
        assertEquals(first, holder.keyFor(cartSignature(listOf(line(1, 2)))))
        assertEquals(1, n)
    }

    @Test fun key_seRenuevaAlCambiarElCarrito() {
        var n = 0
        val holder = IdempotencyKeyHolder { "key-${++n}" }
        val a = holder.keyFor(cartSignature(listOf(line(1, 1))))
        val b = holder.keyFor(cartSignature(listOf(line(1, 2))))
        assertNotEquals(a, b)
        val c = holder.keyFor(cartSignature(listOf(line(1, 2, notes = "sin cebolla"))))
        assertNotEquals(b, c)
    }

    @Test fun key_seRenuevaTrasExito_aunConCarritoIdentico() {
        var n = 0
        val holder = IdempotencyKeyHolder { "key-${++n}" }
        val sig = cartSignature(listOf(line(1, 1)))
        val first = holder.keyFor(sig)
        holder.onSuccess()
        assertNotEquals(first, holder.keyFor(sig))
    }

    @Test fun key_porDefectoEsUuidV4() {
        val key = IdempotencyKeyHolder().keyFor("x")
        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(key))
    }

    // --- Estacion recordada ---------------------------------------------------------------------

    @Test fun estacion_seRecuerdaPorRestaurante() {
        assertNotEquals(StationMemory.prefKey("a"), StationMemory.prefKey("b"))
    }

    @Test fun estacion_ciclaYAdminNoSeRecuerda() {
        PinStation.entries.filter { it != PinStation.ADMIN }.forEach { st ->
            assertEquals(st, StationMemory.decode(StationMemory.encode(st)))
        }
        assertNull(StationMemory.encode(PinStation.ADMIN))
        assertNull(StationMemory.decode("admin"))
    }

    @Test fun estacion_valorIlegibleONuloNoSalta() {
        assertNull(StationMemory.decode(null))
        assertNull(StationMemory.decode(""))
        assertNull(StationMemory.decode("basura"))
    }

    // --- Auto-cerrar sesion vacia ---------------------------------------------------------------

    @Test fun autoCierre_sesionVaciaSeCierra() {
        assertTrue(shouldAutoCloseEmptySession(session(), cartSize = 0))
    }

    @Test fun autoCierre_nuncaConItemsOTotalOCarrito() {
        assertFalse(shouldAutoCloseEmptySession(session(comandas = listOf(comanda())), cartSize = 0))
        assertFalse(shouldAutoCloseEmptySession(session(total = 25.0), cartSize = 0))
        assertFalse(shouldAutoCloseEmptySession(session(), cartSize = 1))
        assertFalse(shouldAutoCloseEmptySession(session(), cartSize = 0, busy = true))
        assertFalse(shouldAutoCloseEmptySession(null, cartSize = 0))
    }

    @Test fun autoCierre_lineasAnuladasNoCuentan_yNoEsDeMesaNoSeCierra() {
        assertTrue(shouldAutoCloseEmptySession(session(comandas = listOf(comanda(cancelled = true))), cartSize = 0))
        assertFalse(shouldAutoCloseEmptySession(session(tableName = null), cartSize = 0))
    }

    // --- PATCH de comensales / nota -------------------------------------------------------------

    @Test fun patch_reenviaLoQueNoSeEdita() {
        val current = session().copy(
            customerName = "Luis", customerPhone = "999", deliveryAddress = "Av 1",
            deliveryReference = "ref", estimatedMinutes = 20, notes = "alergia",
        )
        val patch = buildSessionPatch(current, guests = 4)
        assertEquals("Luis", patch.customerName)
        assertEquals(20, patch.estimatedMinutes)
        assertEquals("alergia", patch.notes)
        assertEquals(4, patch.guests)
        val notePatch = buildSessionPatch(current, notes = "  sin sal ")
        assertEquals("sin sal", notePatch.notes)
        assertNull(notePatch.guests)
        assertEquals(1, buildSessionPatch(current, guests = 0).guests)
    }

    // --- Producto manual ------------------------------------------------------------------------

    @Test fun manual_seMarcaConQuienLoAgrego_ySoloLosManuales() {
        val cart = listOf(line(1), line(0, notes = "extra", kind = "manual"), line(0, kind = "manual"))
        val stamped = stampManualLines(cart, "Ana")
        assertEquals("", stamped[0].notes)
        assertEquals("Producto manual · agregado por Ana — extra", stamped[1].notes)
        assertEquals("Producto manual · agregado por Ana", stamped[2].notes)
        // no muta el carrito original
        assertEquals("extra", cart[1].notes)
    }

    @Test fun manual_noDuplicaLaMarca_ySinNombreQuedaGenerica() {
        val once = stampManualLines(listOf(line(0, kind = "manual")), "Ana")
        assertSame(once[0], stampManualLines(once, "Ana")[0])
        assertEquals("Producto manual", stampManualLines(listOf(line(0, kind = "manual")), "  ")[0].notes)
    }

    // --- Texto del toast ------------------------------------------------------------------------

    @Test fun toast_comandaEnviada() {
        assertEquals("Comanda #3 enviada · 4 ítems", comandaSentMessage(3, 4))
        assertEquals("Comanda #3 enviada · 1 ítem", comandaSentMessage(3, 1))
        assertEquals("Comanda enviada", comandaSentMessage(0, 0))
    }
}
