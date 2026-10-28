package com.bendey.restaurant.core.domain.locale

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Único formateador de montos de la app (incremento I4c del paquete de país).
 *
 * Hay dos estilos, los mismos dos que existían antes, para que en Perú no cambie ni un carácter:
 *  - [format]: «S/ 1234.50» — símbolo, espacio si `symbol_spacing`, número con punto decimal y sin
 *    separador de miles (`String.format(Locale.US, "%.2f")`). Es el que usaban las ~15 `money()`.
 *  - [currencyFormat]: un `NumberFormat` de moneda del idioma del país; en Perú es exactamente
 *    `NumberFormat.getCurrencyInstance(Locale("es", "PE"))`, el que usaban las pantallas de POS,
 *    mesas, caja, ventas, etc.
 *
 * Los montos de la suscripción a Bendey son siempre en soles: van con [pen], no con el país del tenant.
 */
object Money {

    /** «S/ 12.50» en Perú; «$12.50» en Ecuador. */
    fun format(value: Double, locale: TenantLocale = TenantLocaleHolder.current): String =
        prefix(locale) + amount(value, locale)

    /** Solo el número, con los decimales del país: «12.50». Para campos editables y columnas «(S/)». */
    fun amount(value: Double, locale: TenantLocale = TenantLocaleHolder.current): String =
        String.format(Locale.US, "%.${locale.decimals}f", value)

    /** Lo que va antes del número: «S/ » en Perú, «$» en Ecuador. */
    fun prefix(locale: TenantLocale = TenantLocaleHolder.current): String =
        locale.currencySymbol + if (locale.symbolSpacing) " " else ""

    /** Símbolo de la moneda del tenant, para rótulos como «Monto (S/)». */
    val symbol: String
        get() = TenantLocaleHolder.current.currencySymbol

    /**
     * Símbolo de la moneda de un comprobante impreso. Perú: «S/» para soles y «USD» para dólares, como
     * siempre. Otro país: si la moneda es la del tenant (o no viene), su símbolo («$» en Ecuador).
     */
    fun documentSymbol(currency: String?, locale: TenantLocale = TenantLocaleHolder.current): String {
        val c = currency?.trim()?.uppercase().orEmpty()
        if (!locale.isPeru && (c.isEmpty() || c == locale.currency.uppercase())) return locale.currencySymbol
        return if (c == "USD") "USD" else "S/"
    }

    /** Montos de planes/suscripción de Bendey: siempre soles, sea cual sea el país del tenant. */
    fun pen(value: Double): String = format(value, TenantLocale.PE)

    /**
     * `NumberFormat` de moneda del país del tenant (instancia nueva: `NumberFormat` no es seguro
     * entre hilos). En Perú devuelve EXACTAMENTE `NumberFormat.getCurrencyInstance(Locale("es", "PE"))`
     * sin tocarlo, así que el texto es idéntico al de siempre.
     */
    fun currencyFormat(locale: TenantLocale = TenantLocaleHolder.current): NumberFormat {
        if (isPeruDefault(locale)) return NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        val nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(locale.numberLocale))
        runCatching { nf.currency = Currency.getInstance(locale.currency) }
        (nf as? DecimalFormat)?.let { df ->
            val symbols = df.decimalFormatSymbols
            symbols.currencySymbol = locale.currencySymbol
            df.decimalFormatSymbols = symbols
        }
        nf.minimumFractionDigits = locale.decimals
        nf.maximumFractionDigits = locale.decimals
        return nf
    }

    private fun isPeruDefault(locale: TenantLocale): Boolean =
        locale.currency == TenantLocale.PE.currency &&
            locale.currencySymbol == TenantLocale.PE.currencySymbol &&
            locale.numberLocale == TenantLocale.PE.numberLocale &&
            locale.decimals == TenantLocale.PE.decimals
}
