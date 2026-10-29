package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.TaxRateOption
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AffectationTotalsTest {

    private val ec = TenantLocale(
        country = "EC", currency = "USD", currencySymbol = "$", symbolSpacing = false,
        taxName = "IVA", taxAuthority = "SRI",
        taxRates = listOf(
            TaxRateOption("4", "IVA 15%", 15.0, isDefault = true),
            TaxRateOption("5", "IVA 5%", 5.0),
            TaxRateOption("0", "IVA 0%", 0.0),
            TaxRateOption("6", "No objeto", 0.0),
            TaxRateOption("7", "Exento", 0.0),
        ),
    )

    @After fun reset() = TenantLocaleHolder.set(TenantLocale.PE)

    @Test fun `Ecuador va tarifa por tarifa como el RIDE`() {
        TenantLocaleHolder.set(ec)
        val tax = LabelKey.TAX_NAME.text
        val rows = affectationTotalsRows(
            mapOf("4" to AffectationTotal(12.0, 1.8), "0" to AffectationTotal(1.0, 0.0), "7" to AffectationTotal(5.0, 0.0)), 1.8, ec,
        )
        assertEquals(
            listOf("Subtotal 15 %=12.0", "Subtotal 0 %=1.0", "Subtotal exento de $tax=5.0", "$tax 15 %=1.8"),
            rows.map { "${it.label}=${it.amount}" },
        )
    }

    @Test fun `Peru separa gravada, exonerada, inafecta y exportacion`() {
        val rows = affectationTotalsRows(
            mapOf("10" to AffectationTotal(100.0, 18.0), "20" to AffectationTotal(10.0, 0.0), "40" to AffectationTotal(50.0, 0.0)), 18.0, TenantLocale.PE,
        )
        assertEquals(listOf("op_10", "op_20", "op_40", "tax"), rows.map { it.key })
    }

    @Test fun `clave del SRI solo si hay clave`() {
        assertTrue(sriTicketLines(null, null).isEmpty())
        assertEquals("Ambiente: PRUEBAS", sriTicketLines("123", "pruebas").first())
    }
}
