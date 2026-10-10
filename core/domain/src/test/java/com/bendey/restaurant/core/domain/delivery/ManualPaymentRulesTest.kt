package com.bendey.restaurant.core.domain.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2a: reglas puras del pago con captura. */
class ManualPaymentRulesTest {

    @Test fun losMinutosValidosVanDe5a120() {
        assertNull(ManualPaymentRules.parseReviewMinutes("4"))
        assertEquals(5, ManualPaymentRules.parseReviewMinutes("5"))
        assertEquals(120, ManualPaymentRules.parseReviewMinutes(" 120 "))
        assertNull(ManualPaymentRules.parseReviewMinutes("121"))
        assertNull(ManualPaymentRules.parseReviewMinutes(""))
        assertNull(ManualPaymentRules.parseReviewMinutes("abc"))
    }

    @Test fun elMotivoDelRechazoPideTresCaracteresSinContarEspacios() {
        assertFalse(ManualPaymentRules.isRejectReasonValid("  ab "))
        assertTrue(ManualPaymentRules.isRejectReasonValid(" abc "))
    }

    @Test fun elEfectivoNuncaEsUnMedioOfrecible() {
        assertFalse(ManualPaymentRules.isOfferableCashbankMethod("cash", "bank", true))
        assertFalse(ManualPaymentRules.isOfferableCashbankMethod("efectivo", "cash", true))
        assertFalse(ManualPaymentRules.isOfferableCashbankMethod("yape", "bank", false))
        assertFalse(ManualPaymentRules.isOfferableCashbankMethod("", "bank", true))
        assertTrue(ManualPaymentRules.isOfferableCashbankMethod("yape", "bank", true))
    }

    @Test fun elFormularioPideCodigoYNombre() {
        assertNotNull(ManualPaymentRules.validateMethod(PayMethodInput("", "Yape")))
        assertNotNull(ManualPaymentRules.validateMethod(PayMethodInput("yape", "   ")))
        assertNull(ManualPaymentRules.validateMethod(PayMethodInput("yape", "Yape")))
    }

    @Test fun losMinutosRestantesSuben() {
        val now = 1_000_000L
        assertNull(ManualPaymentRules.minutesLeft(null, now))
        assertEquals(0, ManualPaymentRules.minutesLeft(now - 5, now))
        assertEquals(1, ManualPaymentRules.minutesLeft(now + 1, now))
        assertEquals(1, ManualPaymentRules.minutesLeft(now + 60_000, now))
        assertEquals(2, ManualPaymentRules.minutesLeft(now + 60_001, now))
    }

    @Test fun laRutaRelativaDeLaCapturaSeResuelveContraElOrigen() {
        assertEquals("https://api.bendey.cloud/storage/a.jpg", ManualPaymentRules.resolveAssetUrl("https://api.bendey.cloud/api", "/storage/a.jpg"))
        assertEquals("https://x.test/p.png", ManualPaymentRules.resolveAssetUrl("https://api.bendey.cloud", "https://x.test/p.png"))
    }
}
