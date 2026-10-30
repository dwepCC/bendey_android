package com.bendey.restaurant.core.network.dto

import com.bendey.restaurant.core.network.serialization.ApiJson
import org.junit.Assert.assertTrue
import org.junit.Test

/** El backend exige el país en el registro (paquete de país): sin el campo el alta responde 400. */
class PublicRegisterRequestDtoTest {

    @Test
    fun `el registro envia el pais`() {
        val body = ApiJson.encodeToString(
            PublicRegisterRequestDto.serializer(),
            PublicRegisterRequestDto(name = "Sabores", ruc = "20123456789", email = "a@b.pe", password = "x", country = "PE"),
        )
        assertTrue(body, body.contains("\"country\":\"PE\""))
    }
}
