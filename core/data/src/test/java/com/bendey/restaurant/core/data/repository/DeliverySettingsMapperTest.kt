package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.network.dto.AssignDeliveryDriverRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryBoardDto
import com.bendey.restaurant.core.network.dto.DeliverySettingsDto
import com.bendey.restaurant.core.network.dto.DeliverySettingsUpdateRequestDto
import com.bendey.restaurant.core.network.dto.SessionDeliveryFeeRequestDto
import com.bendey.restaurant.core.network.dto.SessionDeliveryFeeResponseDto
import com.bendey.restaurant.core.network.serialization.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2.0: el JSON del contrato (D2_COMMON secciones 1, 2, 3, 4 y 7) -> dominio y el cuerpo de los PUT/POST. */
class DeliverySettingsMapperTest {
    private fun parse(json: String) = ApiJson.decodeFromString<DeliverySettingsDto>(json).toDomain()

    @Test fun parseaElContratoCompletoDeAjustes() {
        val s = parse(
            """{"fee_enabled":true,"delivery_fee":5.00,"fee_igv_affectation":"20",
                "cod_enabled":false,"manual_payment_enabled":false,"payment_review_minutes":15}""",
        )
        assertTrue(s.feeEnabled)
        assertEquals(5.0, s.deliveryFee, 0.0)
        assertEquals("20", s.feeIgvAffectation)
        assertFalse(s.codEnabled)
        assertFalse(s.manualPaymentEnabled)
        assertEquals(15, s.paymentReviewMinutes)
    }

    @Test fun leeCodEnabledEncendidoYSinDatoQuedaApagado() {
        assertTrue(parse("""{"cod_enabled":true}""").codEnabled)
        assertFalse(parse("""{"cod_enabled":null}""").codEnabled)
        assertFalse(parse("{}").codEnabled)
        assertTrue(parse("""{"data":{"cod_enabled":true}}""").codEnabled)
    }

    @Test fun sinFilaTodoPorDefectoYApagado() {
        val s = parse("{}")
        assertFalse(s.feeEnabled)
        assertEquals(0.0, s.deliveryFee, 0.0)
        assertEquals("10", s.feeIgvAffectation)
        assertEquals(15, s.paymentReviewMinutes)
    }

    @Test fun nulosYAfectacionDesconocidaVuelvenAlDefault() {
        val s = parse("""{"fee_enabled":null,"delivery_fee":null,"fee_igv_affectation":"99","payment_review_minutes":null}""")
        assertFalse(s.feeEnabled)
        assertEquals(0.0, s.deliveryFee, 0.0)
        assertEquals("10", s.feeIgvAffectation)
        assertEquals(15, s.paymentReviewMinutes)
    }

    @Test fun desenvuelveElEnvoltorioData() {
        val s = parse("""{"data":{"fee_enabled":true,"delivery_fee":3.5,"fee_igv_affectation":"30"}}""")
        assertTrue(s.feeEnabled)
        assertEquals(3.5, s.deliveryFee, 0.0)
        assertEquals("30", s.feeIgvAffectation)
    }

    @Test fun tarifaNegativaSeAcotaACero() {
        assertEquals(0.0, parse("""{"delivery_fee":-4}""").deliveryFee, 0.0)
    }

    // ---- tarjeta del tablero ----

    private fun card(extra: String) = ApiJson.decodeFromString<DeliveryBoardDto>(
        """{"unassigned":[{"session_id":41,"customer_name":"Ana","items_count":2,"total_amount":57.5$extra}]}""",
    ).toDomain().unassigned.single()

    @Test fun laTarjetaTraeLaTarifaDelContrato() {
        val c = card(""","delivery_fee":7.5""")
        assertEquals(7.5, c.deliveryFee!!, 0.0)
        assertEquals(57.5, c.totalAmount, 0.0)
    }

    @Test fun laTarjetaSinTarifaONulaOCeroNoLaMuestra() {
        assertNull(card("").deliveryFee)
        assertNull(card(""","delivery_fee":null""").deliveryFee)
        assertNull(card(""","delivery_fee":0""").deliveryFee)
    }

    // ---- cuerpos que se envian ----

    @Test fun elPutDeAjustesEsParcialYNoMandaNulos() {
        val body = ApiJson.encodeToString(
            DeliverySettingsUpdateRequestDto.serializer(),
            DeliverySettingsUpdateRequestDto(feeEnabled = true, deliveryFee = 5.0),
        )
        assertEquals("""{"fee_enabled":true,"delivery_fee":5.0}""", body)
    }

    @Test fun asignarSoloMandaDeliveryFeeSiSeCambio() {
        assertEquals(
            """{"driver_id":3}""",
            ApiJson.encodeToString(AssignDeliveryDriverRequestDto.serializer(), AssignDeliveryDriverRequestDto(3)),
        )
        assertEquals(
            """{"driver_id":3,"delivery_fee":7.5}""",
            ApiJson.encodeToString(AssignDeliveryDriverRequestDto.serializer(), AssignDeliveryDriverRequestDto(3, 7.5)),
        )
    }

    @Test fun laTarifaDeUnPedidoMandaAmountYLeeLaRespuesta() {
        assertEquals(
            """{"amount":7.5}""",
            ApiJson.encodeToString(SessionDeliveryFeeRequestDto.serializer(), SessionDeliveryFeeRequestDto(7.5)),
        )
        val r = ApiJson.decodeFromString<SessionDeliveryFeeResponseDto>("""{"session_id":41,"delivery_fee":7.5,"total_amount":57.5}""")
        assertEquals(41, r.sessionId)
        assertEquals(57.5, r.totalAmount!!, 0.0)
    }
}
