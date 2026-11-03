package com.bendey.restaurant.core.domain.cash

import org.junit.Assert.assertEquals
import org.junit.Test

class CashMethodBalancesTest {

    @Test fun mandaSoloLoQueElCajeroLlenoConUnNumeroValido() {
        val inputs = methodBalanceInputs(
            linkedMapOf("yape" to "55", "plin" to "", "tarjeta" to "abc", "transferencia" to "-3"),
        )
        assertEquals(listOf(CashMethodBalanceInput("yape", 55.0)), inputs)
    }

    @Test fun aceptaComaDecimalYRedondeaACentimos() {
        assertEquals(listOf(CashMethodBalanceInput("yape", 12.35)), methodBalanceInputs(mapOf("yape" to " 12,345 ")))
    }

    @Test fun ceroEsUnMontoValido_rindioYAbreEnCero() {
        assertEquals(listOf(CashMethodBalanceInput("yape", 0.0)), methodBalanceInputs(mapOf("yape" to "0")))
    }

    @Test fun elSugeridoSeEscribeSinDecimalesSobrantes() {
        assertEquals("55", suggestedAmountText(55.0))
        assertEquals("12.5", suggestedAmountText(12.5))
        assertEquals("0.1", suggestedAmountText(0.1))
    }
}
