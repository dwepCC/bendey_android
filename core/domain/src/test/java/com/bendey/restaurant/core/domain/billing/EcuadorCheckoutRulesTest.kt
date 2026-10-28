package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeFormResult
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeRules
import com.bendey.restaurant.core.domain.delivery.validateDeliveryFeeForm
import com.bendey.restaurant.core.domain.locale.IdentityTypeOption
import com.bendey.restaurant.core.domain.locale.TaxRateOption
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de cobro por país: en Ecuador la factura admite cédula y consumidor final hasta 50 USD, y el
 * impuesto de la línea sale de `tax_rates`. Perú queda exactamente como antes (RUC para factura, S/ 700).
 */
class EcuadorCheckoutRulesTest {

    private val ec = TenantLocale(
        country = "EC", currency = "USD", currencySymbol = "$", symbolSpacing = false,
        taxName = "IVA", taxAuthority = "SRI",
        identityTypes = listOf(
            IdentityTypeOption("04", "tax_id", "RUC", 13, 13, true),
            IdentityTypeOption("05", "national_id", "Cédula", 10, 10, true),
            IdentityTypeOption("06", "passport", "Pasaporte", 1, 20),
            IdentityTypeOption("08", "foreign_id", "Identificación del exterior", 1, 20),
            IdentityTypeOption("07", "other", "Consumidor final", 13, 13, true),
        ),
        invoiceFinalConsumerMax = 50.0,
        taxRates = listOf(
            TaxRateOption("4", "IVA 15%", 15.0, isDefault = true),
            TaxRateOption("5", "IVA 5%", 5.0),
            TaxRateOption("0", "IVA 0%", 0.0),
            TaxRateOption("6", "No objeto de IVA", 0.0),
            TaxRateOption("7", "Exento de IVA", 0.0),
        ),
    )

    private fun contact(docType: String, doc: String = "") = ContactBrief(1, docType, doc, "Cliente", true)

    @After fun restore() = TenantLocaleHolder.reset()

    @Test fun peruKeepsSunatRules() {
        assertFalse(checkoutContactIsValid(contact("1"), "FACTURA", "01"))
        assertTrue(checkoutContactIsValid(contact("6"), "FACTURA", "01"))
        assertTrue(exceedsSunatMaxMontoSinRuc(701.0, contact("0"), "03"))
        assertNull(countryBuyerError(contact("0"), 9999.0))
        assertFalse(isFinalConsumerSale(""))
        assertEquals(18.0, effectiveRate("10", 18.0), 0.0)
        assertEquals("10", defaultAffectation(null))
        assertEquals(listOf("0", "1", "6", "4", "7"), ContactDocType.options().map { it.code })
    }

    @Test fun ecuadorInvoiceAcceptsCedulaAndFinalConsumerUpTo50() {
        TenantLocaleHolder.set(ec)
        assertTrue(checkoutContactIsValid(contact("05"), "FACTURA", "01"))
        assertFalse(exceedsSunatMaxMontoSinRuc(701.0, contact("0"), "03"))
        assertNull(countryBuyerError(contact("07"), 50.0))
        assertNotNull(countryBuyerError(contact("07"), 50.01))
        assertNotNull(countryBuyerError(contact("0"), 80.0))
        assertNull(countryBuyerError(contact("05"), 500.0))
        assertTrue(isFinalConsumerSale("9999999999999"))
        assertTrue(isFinalConsumerSale("99999999"))
        assertFalse(isFinalConsumerSale("1712345678"))
    }

    @Test fun ecuadorLineTaxUsesTheSriCode() {
        TenantLocaleHolder.set(ec)
        assertEquals(15.0, effectiveRate("4", 15.0), 0.0)
        assertEquals(0.0, effectiveRate("7", 15.0), 0.0)
        assertEquals(15.0, effectiveRate("", 15.0), 0.0)
        assertEquals("4", defaultAffectation(null))
        val item = calcItem(11.5, 1.0, 0.0, "4", true, 15.0)
        assertEquals(10.0, item.subtotal, 0.001)
        assertEquals(1.5, item.taxAmount, 0.001)
    }

    @Test fun ecuadorDocTypeOptionsComeFromTheLocale() {
        TenantLocaleHolder.set(ec)
        assertEquals(listOf("04", "05", "06", "08", "07"), ContactDocType.options().map { it.code })
        assertEquals(listOf("04", "05", "06", "08"), ContactDocType.options(includeFinalConsumer = false).map { it.code })
        assertEquals(ContactDocType.EC_CEDULA, ContactDocType.defaultForCustomer())
        assertEquals(ContactDocType.EC_RUC, ContactDocType.defaultForSupplier())
        assertEquals("Cédula", ContactDocType.EC_CEDULA.label)
    }

    @Test fun tarifaDeDeliveryUsaElIvaDelPais() {
        assertEquals(listOf("10", "20", "30"), DeliveryFeeRules.affectations.map { it.first })
        assertEquals("10", DeliveryFeeRules.defaultAffectation)

        TenantLocaleHolder.set(ec)
        assertEquals(listOf("4", "5", "0", "6", "7"), DeliveryFeeRules.affectations.map { it.first })
        assertEquals("4", DeliveryFeeRules.defaultAffectation)
        assertTrue(validateDeliveryFeeForm(true, "2.50", "0") is DeliveryFeeFormResult.Ok)
        // «20» (exonerado en Perú) no es una tarifa del SRI.
        assertTrue(validateDeliveryFeeForm(true, "2.50", "20") is DeliveryFeeFormResult.Invalid)
    }
}
