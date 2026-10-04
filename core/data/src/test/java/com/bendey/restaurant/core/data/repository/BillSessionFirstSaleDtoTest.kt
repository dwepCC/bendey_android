package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.network.dto.BillSessionResponseDto
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** R6: `first_sale` es booleano estricto y NUNCA rompe la deserializacion del cobro. */
class BillSessionFirstSaleDtoTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun parse(extra: String) = json.decodeFromString<BillSessionResponseDto>(
        """{"success":true,"data":{"id":1,"number":"B001-1","total":10.0}$extra}""",
    )

    @Test
    fun trueEsPrimeraVenta() = assertTrue(parse(""","first_sale":true""").isFirstSale)

    @Test
    fun falseAusenteNullTextoYNumeroNoLoSon() {
        assertFalse(parse(""","first_sale":false""").isFirstSale)
        assertFalse(parse("").isFirstSale)
        assertFalse(parse(""","first_sale":null""").isFirstSale)
        assertFalse(parse(""","first_sale":"true"""").isFirstSale)
        assertFalse(parse(""","first_sale":1""").isFirstSale)
    }

    @Test
    fun elCobroSigueLeyendose() = assertEquals("B001-1", parse(""","first_sale":"raro"""").data?.number)
}
