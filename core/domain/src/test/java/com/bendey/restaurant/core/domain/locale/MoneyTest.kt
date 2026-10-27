package com.bendey.restaurant.core.domain.locale

import java.text.NumberFormat
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Criterio «Perú idéntico» del paquete de país (BENDEY-PAQUETE-DE-PAIS-DISENO.md §6.2): para PE,
 * `Money` debe dar EXACTAMENTE lo que daban las `money()` duplicadas y los `NumberFormat` es-PE.
 */
class MoneyTest {
    private val values = listOf(0.0, 0.5, 1234.5, -10.0, 1_000_000.0, 12.345, 0.005, 99.999)

    private val ec = TenantLocale(
        country = "EC", currency = "USD", currencySymbol = "$", symbolSpacing = false, decimals = 2,
        numberLocale = "es-EC", timeZone = "America/Guayaquil", taxName = "IVA", taxAuthority = "SRI",
    )

    @After fun restore() = TenantLocaleHolder.reset()

    @Test fun peruIsIdenticalToTheOldMoneyFunctions() {
        for (v in values) {
            val old = String.format(Locale.US, "S/ %.2f", v)
            assertEquals("valor $v", old, Money.format(v))
            assertEquals("valor $v", old, Money.format(v, TenantLocale.PE))
            assertEquals("valor $v", old, Money.pen(v))
        }
        assertEquals("S/ 0.00", Money.format(0.0))
        assertEquals("S/ 0.50", Money.format(0.5))
        assertEquals("S/ 1234.50", Money.format(1234.5))
        assertEquals("S/ -10.00", Money.format(-10.0))
        assertEquals("S/ 1000000.00", Money.format(1_000_000.0))
    }

    @Test fun peruIgnoresTheDeviceLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale("es", "ES")) // coma decimal: no debe colarse
            assertEquals("S/ 1234.50", Money.format(1234.5))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test fun peruPrefixSymbolAndAmount() {
        assertEquals("S/ ", Money.prefix())
        assertEquals("S/", Money.symbol)
        assertEquals("1234.50", Money.amount(1234.5))
    }

    @Test fun peruCurrencyFormatIsTheOldNumberFormat() {
        val old = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        val new = Money.currencyFormat()
        for (v in values) assertEquals("valor $v", old.format(v), new.format(v))
    }

    @Test fun ecuadorFollowsTheHolder() {
        TenantLocaleHolder.set(ec)
        assertEquals("$0.00", Money.format(0.0))
        assertEquals("$1234.50", Money.format(1234.5))
        assertEquals("$-10.00", Money.format(-10.0))
        assertEquals("$", Money.prefix())
        assertEquals("$", Money.symbol)
        // La suscripción a Bendey sigue en soles aunque el tenant sea de Ecuador.
        assertEquals("S/ 49.00", Money.pen(49.0))
        // El NumberFormat de Ecuador muestra el símbolo del país y 2 decimales.
        val shown = Money.currencyFormat().format(1234.5)
        assert(shown.contains("$") && shown.contains("1") && shown.contains("50")) { shown }
    }

    @Test fun otherDecimalsAndSpacing() {
        val noDecimals = TenantLocale(currency = "CLP", currencySymbol = "$", symbolSpacing = true, decimals = 0)
        assertEquals("$ 1235", Money.format(1234.5, noDecimals))
    }

    @Test fun sanitizedNeverBreaksFormatting() {
        TenantLocaleHolder.set(TenantLocale(currencySymbol = " ", currency = "USD", decimals = 99, timeZone = "Marte/Olimpo"))
        val cur = TenantLocaleHolder.current
        assertEquals("USD", cur.currencySymbol)
        assertEquals(4, cur.decimals)
        assertEquals("America/Lima", cur.timeZone)
    }
}
