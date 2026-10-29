package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.TenantLocale
import kotlin.math.round

/** Base e impuesto de un código de afectación (Perú 10/20/30/40; Ecuador la tarifa del SRI). */
data class AffectationTotal(val subtotal: Double, val taxAmount: Double)

/** Una fila de totales del comprobante. */
data class TotalsRow(val key: String, val label: String, val amount: Double)

private fun r2(v: Double) = round(v * 100) / 100

/**
 * Totales por tipo de afectación, como el ERP y Tauri (rateTotalRows / groupedAffectationTotals):
 * - Ecuador: tarifa por tarifa, como el RIDE del SRI: «Subtotal 15 %», «Subtotal 0 %», «Subtotal exento de
 *   IVA»… y después «IVA 15 %». El subtotal de la tarifa general va siempre; los demás, si hay.
 * - Perú: «Op. Gravadas», «Op. Exoneradas», «Op. Inafectas», «Op. Exportación» (solo las que tienen monto)
 *   y el IGV.
 * `totals` es el `totals_by_affectation` de los datos de impresión (código → base e impuesto).
 */
fun affectationTotalsRows(totals: Map<String, AffectationTotal>, taxAmount: Double, locale: TenantLocale): List<TotalsRow> {
    val tax = LabelKey.TAX_NAME.text
    val rates = locale.countryTaxRates
    if (rates == null) {
        val labels = linkedMapOf("10" to "Op. Gravadas", "20" to "Op. Exoneradas", "30" to "Op. Inafectas", "40" to "Op. Exportación")
        val rows = mutableListOf<TotalsRow>()
        // Los códigos 11-17 (gravadas), 21 y 31-36 caen en su grupo, como en el resto de la app.
        val grouped = mutableMapOf<String, Double>()
        totals.forEach { (code, v) ->
            val group = when {
                code == "40" -> "40"
                code.startsWith("2") -> "20"
                code.startsWith("3") -> "30"
                else -> "10"
            }
            grouped[group] = (grouped[group] ?: 0.0) + v.subtotal
        }
        labels.forEach { (code, label) ->
            val v = r2(grouped[code] ?: 0.0)
            if (v > 0.0) rows += TotalsRow("op_$code", label, v)
        }
        if (taxAmount > 0) rows += TotalsRow("tax", tax, r2(taxAmount))
        return rows
    }
    fun pct(rate: Double) = if (rate % 1.0 == 0.0) "${rate.toInt()} %" else "$rate %"
    fun labelOf(code: String, rate: Double) = when (code) {
        "6" -> "Subtotal no objeto de $tax"
        "7" -> "Subtotal exento de $tax"
        else -> "Subtotal ${pct(rate)}"
    }
    val general = rates.firstOrNull { it.isDefault }
    val order = listOfNotNull(general) + rates.filter { it != general && it.rate > 0 } + rates.filter { it != general && it.rate == 0.0 }
    val rows = mutableListOf<TotalsRow>()
    order.forEach { r ->
        val v = totals[r.code]
        if (r == general || (v != null && r2(v.subtotal) != 0.0)) rows += TotalsRow("base_${r.code}", labelOf(r.code, r.rate), r2(v?.subtotal ?: 0.0))
    }
    order.forEach { r ->
        val v = totals[r.code]
        if (r.rate > 0 && (r == general || (v != null && r2(v.taxAmount) != 0.0))) rows += TotalsRow("tax_${r.code}", "$tax ${pct(r.rate)}", r2(v?.taxAmount ?: 0.0))
    }
    return rows
}

/** Ecuador: ambiente, emisión y clave de acceso del SRI en el ticket (en lugar del QR y el hash). Vacío si no hay clave. */
fun sriTicketLines(accessKey: String?, fiscalEnvironment: String?): List<String> {
    val key = accessKey?.trim().orEmpty()
    if (key.isEmpty()) return emptyList()
    val env = if (fiscalEnvironment == "produccion") "PRODUCCION" else "PRUEBAS"
    return listOf("Ambiente: $env", "Emision: NORMAL", "Autorizacion / Clave de acceso:", key)
}
