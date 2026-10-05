package com.bendey.restaurant.core.domain.pendingapproval

import com.bendey.restaurant.core.domain.kitchen.shouldPlayNewOrderSound
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingApprovalLogicTest {
    private val ev = PendingApprovalLogic.EVENT

    @Test fun permisosVerAprobarRechazar() {
        assertTrue(PendingApprovalLogic.canView(listOf("t.v")))
        assertFalse(PendingApprovalLogic.canApprove(listOf("t.v")))
        assertTrue(PendingApprovalLogic.canApprove(listOf("k.v")))
        assertFalse(PendingApprovalLogic.canReject(listOf("k.v")))
        assertTrue(PendingApprovalLogic.canReject(listOf("t.o")))
        assertTrue(PendingApprovalLogic.canReject(listOf("o.ch")))
        assertTrue(PendingApprovalLogic.canApprove(listOf("s.m")))
        assertFalse(PendingApprovalLogic.canView(emptyList()))
        assertFalse(PendingApprovalLogic.canView(null))
        assertFalse(PendingApprovalLogic.canView(listOf("g.p")))
    }

    @Test fun sonidoSoloPendingApprovalYSoloQuienRevisa() {
        assertTrue(PendingApprovalLogic.shouldPlaySound(ev, listOf("o.ch")))
        assertFalse(PendingApprovalLogic.shouldPlaySound(ev, listOf("g.p")))
        assertFalse(PendingApprovalLogic.shouldPlaySound("menu.order.created", listOf("s.m")))
        assertFalse(PendingApprovalLogic.shouldPlaySound("restaurant.order.created", listOf("s.m")))
    }

    @Test fun menuOrderCreatedNoSuena() = assertFalse(shouldPlayNewOrderSound("menu.order.created"))

    @Test fun soloElEventoRefrescaLaCola() {
        assertTrue(PendingApprovalLogic.shouldRefreshOnEvent(ev))
        assertFalse(PendingApprovalLogic.shouldRefreshOnEvent("menu.order.created"))
    }

    @Test fun badge() {
        assertEquals("", PendingApprovalLogic.badgeLabel(0))
        assertEquals("3", PendingApprovalLogic.badgeLabel(3))
        assertEquals("99+", PendingApprovalLogic.badgeLabel(100))
        assertEquals(0, PendingApprovalLogic.visibleCount(listOf("g.p"), 5))
        assertEquals(5, PendingApprovalLogic.visibleCount(listOf("k.v"), 5))
        assertEquals("Pedidos del cliente", PendingApprovalLogic.badgeContentDescription(0))
        assertEquals("1 pedido del cliente por revisar", PendingApprovalLogic.badgeContentDescription(1))
        assertEquals("2 pedidos del cliente por revisar", PendingApprovalLogic.badgeContentDescription(2))
    }

    @Test fun textosIgualesATauri() {
        assertEquals("Pedido del cliente por revisar · Mesa 4", PendingApprovalLogic.arrivedMessage("4"))
        assertEquals("Pedido del cliente por revisar", PendingApprovalLogic.arrivedMessage(" "))
        assertEquals("Mesa 4 · Comanda #12", PendingApprovalLogic.orderTitle("4", 12))
        assertEquals("Pedido sin mesa · Comanda #12", PendingApprovalLogic.orderTitle("", 12))
    }

    @Test fun esperaYTipo() {
        assertEquals(3, PendingApprovalLogic.waitingMinutes(0, 3 * 60_000L + 5))
        assertEquals(0, PendingApprovalLogic.waitingMinutes(10, 0))
        assertNull(PendingApprovalLogic.waitingMinutes(null, 5))
        assertEquals("Hace 5 min", PendingApprovalLogic.waitingLabel(5))
        assertEquals("Para llevar", PendingApprovalLogic.orderTypeLabel("takeaway"))
        assertNull(PendingApprovalLogic.orderTypeLabel("dine_in"))
    }

    @Test fun enumPorAprobar() {
        assertEquals(ComandaStatus.POR_APROBAR, ComandaStatus.fromBackend("por_aprobar"))
        assertEquals("Por revisar", ComandaStatus.POR_APROBAR.label)
        assertNull(ComandaStatus.next("por_aprobar"))
        assertEquals(ComandaStatus.PENDIENTE, ComandaStatus.fromBackend("otra_cosa"))
        assertEquals(ComandaStatus.PREPARACION, ComandaStatus.next("pendiente"))
    }
}
