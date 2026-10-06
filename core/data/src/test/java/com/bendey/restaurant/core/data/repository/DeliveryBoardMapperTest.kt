package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.network.dto.DeliveryBoardDto
import com.bendey.restaurant.core.network.serialization.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D1: el JSON del contrato (D_COMMON.md §1) -> dominio, y que un campo ausente o nulo NUNCA rompe el parseo. */
class DeliveryBoardMapperTest {
    private fun parse(json: String): DeliveryBoardData = ApiJson.decodeFromString<DeliveryBoardDto>(json).toDomain()

    private val fullCard = """
        {"session_id":41,"assignment_id":17,"source":"marketplace","customer_name":"Ana","customer_phone":"999000111",
         "address":"Av. Sol 123","reference":"frente al banco","items_count":3,"total_amount":54.50,
         "order_status":"preparing","created_at":"2026-10-05T22:00:00-05:00","sent_to_kitchen_at":"2026-10-05T22:01:00-05:00",
         "ready_at":null,"estimated_minutes":30,"driver":{"id":3,"name":"Luis","phone":"999111222"},
         "assignment_status":"assigned","assigned_at":"2026-10-05T22:05:00-05:00","accepted_at":null,"picked_up_at":null,
         "on_the_way_at":null,"delivered_at":null,"incident":null,"paid":true}
    """.trimIndent()

    @Test fun parseaElContratoCompleto() {
        val board = parse(
            """
            {"generated_at":"2026-10-05T22:10:00-05:00",
             "counts":{"unassigned":2,"assigned":1,"in_transit":3,"incidents":1,"delivered_today":7},
             "unassigned":[],"assigned":[$fullCard],"in_transit":[],
             "incidents":[{"session_id":50,"assignment_id":18,"source":"staff","customer_name":"Beto","customer_phone":"",
               "address":"Jr. Luna 5","reference":"","items_count":1,"total_amount":10,"order_status":"ready",
               "created_at":"2026-10-05T21:00:00-05:00","driver":null,"assignment_status":"failed",
               "incident":{"kind":"failed","reason":"cliente no contesta","at":"2026-10-05T21:30:00-05:00"},"paid":false}],
             "delivered_today":[],
             "drivers":[{"id":3,"name":"Luis","phone":"999111222","vehicle_type":"moto","is_available":true,"active_count":2}]}
            """.trimIndent(),
        )
        assertEquals("2026-10-05T22:10:00-05:00", board.generatedAt)
        assertEquals(2, board.counts.unassigned)
        assertEquals(7, board.counts.deliveredToday)
        val c = board.assigned.single()
        assertEquals(41, c.sessionId)
        assertEquals(17, c.assignmentId)
        assertEquals("marketplace", c.source)
        assertEquals(54.5, c.totalAmount, 0.0001)
        assertEquals(30, c.estimatedMinutes)
        assertEquals("Luis", c.driver?.name)
        assertEquals("999111222", c.driver?.phone)
        assertEquals("assigned", c.assignmentStatus)
        assertNull(c.readyAt)
        assertTrue(c.paid)
        val inc = board.incidents.single()
        assertEquals("failed", inc.incident?.kind)
        assertEquals("cliente no contesta", inc.incident?.reason)
        assertNull(inc.driver)
        assertFalse(inc.paid)
        assertEquals(1, board.drivers.size)
        assertEquals(2, board.drivers.single().activeCount)
        assertTrue(board.drivers.single().isAvailable)
    }

    @Test fun unCuerpoVacioDaUnTableroVacio() {
        val board = parse("{}")
        assertTrue(board.unassigned.isEmpty() && board.assigned.isEmpty() && board.inTransit.isEmpty())
        assertTrue(board.incidents.isEmpty() && board.deliveredToday.isEmpty() && board.drivers.isEmpty())
        assertEquals(0, board.counts.unassigned)
    }

    @Test fun camposNulosOAusentesNoRompenNada() {
        val board = parse(
            """
            {"counts":null,"unassigned":[{"session_id":9,"customer_name":null,"address":null,"items_count":null,
              "total_amount":null,"order_status":null,"created_at":null,"driver":null,"paid":null}],
             "assigned":null,"in_transit":null,"drivers":null}
            """.trimIndent(),
        )
        val c = board.unassigned.single()
        assertEquals("", c.customerName)
        assertEquals("", c.address)
        assertEquals(0, c.itemsCount)
        assertEquals(0.0, c.totalAmount, 0.0)
        assertEquals("", c.orderStatus)
        assertNull(c.createdAt)
        assertNull(c.driver)
        assertNull(c.assignmentId)
        assertFalse(c.paid)
        // Sin `counts` se usan las longitudes de las listas.
        assertEquals(1, board.counts.unassigned)
    }

    @Test fun ignoraCamposDesconocidosYDescartaTarjetasSinSesion() {
        val board = parse(
            """{"futuro":123,"unassigned":[{"session_id":0,"x":1},{"session_id":5,"campo_nuevo":"hola"}]}""",
        )
        assertEquals(listOf(5), board.unassigned.map { it.sessionId })
    }

    @Test fun desenvuelveUnPosibleDataExterior() {
        val board = parse("""{"data":{"unassigned":[$fullCard],"drivers":[{"id":1,"name":"Ana"}]}}""")
        assertEquals(1, board.unassigned.size)
        assertEquals(1, board.counts.unassigned)
        // Un repartidor sin is_available se asume disponible (el backend valida al asignar).
        assertTrue(board.drivers.single().isAvailable)
        assertEquals(0, board.drivers.single().activeCount)
    }

    @Test fun elRepartidorSinIdNoCuentaComoRepartidor() {
        val board = parse("""{"unassigned":[{"session_id":3,"driver":{"id":0,"name":"?"}}],"drivers":[{"id":0,"name":"fantasma"}]}""")
        assertNull(board.unassigned.single().driver)
        assertTrue(board.drivers.isEmpty())
    }

    @Test fun lasFechasSeConservanTalCual() {
        val c = parse("""{"assigned":[$fullCard]}""").assigned.single()
        assertNotNull(c.createdAt)
        assertEquals("2026-10-05T22:05:00-05:00", c.assignedAt)
    }
}
