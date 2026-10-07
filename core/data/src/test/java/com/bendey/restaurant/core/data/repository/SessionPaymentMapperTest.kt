package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.network.dto.DeliveryBoardDto
import com.bendey.restaurant.core.network.dto.DeliverySettingsUpdateRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryStatusRequestDto
import com.bendey.restaurant.core.network.dto.SessionDetailDto
import com.bendey.restaurant.core.network.dto.SessionPaymentRequestDto
import com.bendey.restaurant.core.network.dto.SessionPaymentResponseDto
import com.bendey.restaurant.core.network.serialization.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2b: el objeto `payment` del CONTRATO (D2B_COMMON) -> dominio, con y sin `payment`, nulos y envoltorio `{data}`. */
class SessionPaymentMapperTest {
    private val contractPayment = """
        {"mode":"cash_on_delivery","status":"pending_collection","expected_amount":83.0,
         "tendered_amount":100.0,"change_amount":17.0,"tendered_insufficient":false,
         "collected_at":null,"collected_by":null}
    """.trimIndent()

    private fun board(card: String): DeliveryBoardData =
        ApiJson.decodeFromString<DeliveryBoardDto>("""{"in_transit":[$card]}""").toDomain()

    @Test fun parseaElPaymentDelContratoEnLaTarjeta() {
        val c = board("""{"session_id":41,"assignment_id":17,"total_amount":83.0,"payment":$contractPayment}""").inTransit.single()
        val p = c.payment!!
        assertEquals("cash_on_delivery", p.mode)
        assertEquals("pending_collection", p.status)
        assertEquals(83.0, p.expectedAmount!!, 0.0)
        assertEquals(100.0, p.tenderedAmount!!, 0.0)
        assertEquals(17.0, p.changeAmount!!, 0.0)
        assertFalse(p.tenderedInsufficient)
        assertNull(p.collectedAt)
        assertNull(p.collectedBy)
        assertTrue(p.isPendingCollection)
    }

    @Test fun sinPaymentONuloLaTarjetaNoTienePago() {
        assertNull(board("""{"session_id":41,"assignment_id":17}""").inTransit.single().payment)
        assertNull(board("""{"session_id":41,"assignment_id":17,"payment":null}""").inTransit.single().payment)
    }

    @Test fun parseaUnPagoCobradoConQuienLoCobro() {
        val c = board(
            """{"session_id":41,"assignment_id":17,"payment":{"mode":"cash_on_delivery","status":"collected",
                "expected_amount":83.0,"tendered_amount":null,"change_amount":null,"tendered_insufficient":false,
                "collected_at":"2026-10-06T14:32:00-05:00","collected_by":{"id":3,"name":"Luis","kind":"driver"}}}""",
        ).inTransit.single()
        val p = c.payment!!
        assertTrue(p.isCollected)
        assertNull(p.tenderedAmount)
        assertEquals("2026-10-06T14:32:00-05:00", p.collectedAt)
        assertEquals("Luis", p.collectedBy?.name)
        assertEquals("driver", p.collectedBy?.kind)
    }

    @Test fun camposNulosOAusentesNoRompenElParseo() {
        val p = board(
            """{"session_id":41,"payment":{"mode":null,"status":null,"expected_amount":null,"tendered_insufficient":null,
                "collected_by":{"id":0,"name":null}}}""",
        ).inTransit.single().payment!!
        assertEquals("", p.mode)
        assertEquals("", p.status)
        assertNull(p.expectedAmount)
        assertFalse(p.tenderedInsufficient)
        assertNull("un cobrador sin id válido se descarta", p.collectedBy)
        assertFalse(p.isCashOnDelivery)
        // Objeto vacío y campos desconocidos tampoco rompen.
        assertNotNull(board("""{"session_id":41,"payment":{"futuro":1}}""").inTransit.single().payment)
    }

    @Test fun elTotalQueSubioMarcaPagoInsuficiente() {
        val p = board(
            """{"session_id":41,"payment":{"mode":"cash_on_delivery","status":"pending_collection","expected_amount":120.0,
                "tendered_amount":100.0,"change_amount":null,"tendered_insufficient":true}}""",
        ).inTransit.single().payment!!
        assertTrue(p.tenderedInsufficient)
        assertNull(p.changeAmount)
    }

    // ---- respuestas de PUT /payment y POST /collect ----

    @Test fun laRespuestaDelPutDePagoTraeElPayment() {
        val r = ApiJson.decodeFromString<SessionPaymentResponseDto>("""{"session_id":41,"payment":$contractPayment}""")
        assertEquals(83.0, r.toPayment()!!.expectedAmount!!, 0.0)
    }

    @Test fun laRespuestaDeCobradoTraeAssignmentIdYPayment() {
        val r = ApiJson.decodeFromString<SessionPaymentResponseDto>(
            """{"assignment_id":17,"payment":{"mode":"cash_on_delivery","status":"collected","expected_amount":83.0}}""",
        )
        assertEquals(17, r.assignmentId)
        assertTrue(r.toPayment()!!.isCollected)
    }

    @Test fun quitarElPagoDevuelveNull() {
        assertNull(ApiJson.decodeFromString<SessionPaymentResponseDto>("""{"session_id":41,"payment":null}""").toPayment())
        assertNull(ApiJson.decodeFromString<SessionPaymentResponseDto>("""{"session_id":41}""").toPayment())
    }

    @Test fun desenvuelveElEnvoltorioData() {
        val r = ApiJson.decodeFromString<SessionPaymentResponseDto>("""{"data":{"session_id":41,"payment":$contractPayment}}""")
        assertTrue(r.toPayment()!!.isPendingCollection)
    }

    // ---- detalle de sesión ----

    @Test fun elDetalleDeSesionTraeElPaymentONo() {
        val con = ApiJson.decodeFromString<SessionDetailDto>("""{"id":41,"order_type":"delivery","payment":$contractPayment}""")
        assertEquals(100.0, con.payment!!.toDomain().tenderedAmount!!, 0.0)
        val sin = ApiJson.decodeFromString<SessionDetailDto>("""{"id":41,"order_type":"delivery"}""")
        assertNull(sin.payment)
    }

    // ---- cuerpos de las peticiones (explicitNulls = false: lo null no se envía) ----

    @Test fun elPutDePagoMandaModoYMontoOSoloElModo() {
        assertEquals(
            """{"mode":"cash_on_delivery","cash_tendered":100.0}""",
            ApiJson.encodeToString(SessionPaymentRequestDto.serializer(), SessionPaymentRequestDto("cash_on_delivery", 100.0)),
        )
        assertEquals(
            """{"mode":"cash_on_delivery"}""",
            ApiJson.encodeToString(SessionPaymentRequestDto.serializer(), SessionPaymentRequestDto("cash_on_delivery", null)),
        )
        assertEquals(
            """{"mode":"none"}""",
            ApiJson.encodeToString(SessionPaymentRequestDto.serializer(), SessionPaymentRequestDto("none")),
        )
    }

    @Test fun elPutDeAjustesMandaSoloCodEnabled() {
        assertEquals(
            """{"cod_enabled":true}""",
            ApiJson.encodeToString(DeliverySettingsUpdateRequestDto.serializer(), DeliverySettingsUpdateRequestDto(codEnabled = true)),
        )
        // El PUT de la tarifa de siempre no toca cod_enabled.
        assertEquals(
            """{"fee_enabled":true,"delivery_fee":5.0,"fee_igv_affectation":"10"}""",
            ApiJson.encodeToString(
                DeliverySettingsUpdateRequestDto.serializer(),
                DeliverySettingsUpdateRequestDto(true, 5.0, "10"),
            ),
        )
    }

    @Test fun elCambioDeEstadoMandaForceReasonSoloSiHay() {
        assertEquals(
            """{"status":"delivered","force_reason":"El cliente pagó en caja"}""",
            ApiJson.encodeToString(
                DeliveryStatusRequestDto.serializer(),
                DeliveryStatusRequestDto("delivered", null, "El cliente pagó en caja"),
            ),
        )
        assertEquals(
            """{"status":"delivered"}""",
            ApiJson.encodeToString(DeliveryStatusRequestDto.serializer(), DeliveryStatusRequestDto("delivered")),
        )
    }
}
