package com.bendey.restaurant.core.domain.billing

/** Fallback alineado con backend `pkg/tax.DefaultConfig()` cuando el tenant no tiene tasa. */
const val DEFAULT_TAX_RATE_PERCENT = 18.0

data class TaxConfig(
    val taxRate: Double = DEFAULT_TAX_RATE_PERCENT,
    /** Contribuyente del Nuevo RUS: no puede emitir facturas. No altera la afectacion de la linea. */
    val isNRUS: Boolean = false,
    /** Acogido a la Ley 27037: la afectacion POR DEFECTO de una linea nueva es «20 Exonerado». */
    val hasAmazonBenefit: Boolean = false,
)

/**
 * Afectacion con la que nace una linea que no declaro la suya (espejo de tax.AfectacionDeLinea).
 *
 * Un negocio acogido a la Ley 27037 vende, en su operacion corriente, dentro de la zona: ese es el
 * defecto. Las ventas fuera del ambito se resuelven declarando la afectacion en la linea, y esta
 * funcion nunca la pisa.
 */
fun defaultAffectation(declared: String?, taxConfig: TaxConfig = TaxConfig()): String {
    val code = declared?.trim().orEmpty()
    if (code.isNotEmpty()) return code
    return if (taxConfig.hasAmazonBenefit) "20" else "10"
}

data class ItemTaxBreakdown(
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
)

fun resolveTaxRatePercent(rate: Double?): Double =
    if (rate != null && rate.isFinite() && rate > 0) rate else DEFAULT_TAX_RATE_PERCENT

fun effectiveRate(
    igvAffectationType: String,
    taxRatePercent: Double,
    taxConfig: TaxConfig = TaxConfig(),
): Double {
    val code = igvAffectationType.trim().ifEmpty { "10" }
    return when (code) {
        "20", "30", "40" -> 0.0
        // Aca habia una trampa espejada del backend: «zona + regimen exonerado» ponia 0% sobre los
        // items GRAVADOS. Exonerar no es cobrar 0%: es emitir la linea con afectacion 20.
        else -> taxRatePercent
    }
}

/** Calcula subtotal, IGV y total por ítem (paridad con `taxCalc.ts` / backend `pkg/tax`). */
fun calcItem(
    unitPrice: Double,
    quantity: Double,
    discount: Double,
    igvAffectationType: String,
    priceIncludesIgv: Boolean,
    taxRatePercent: Double,
    taxConfig: TaxConfig = TaxConfig(),
): ItemTaxBreakdown {
    val rate = effectiveRate(igvAffectationType, taxRatePercent, taxConfig)
    val gross = quantity * unitPrice - discount

    if (rate == 0.0) {
        val total = roundSunat(gross)
        return ItemTaxBreakdown(subtotal = total, taxAmount = 0.0, total = total)
    }

    return if (priceIncludesIgv) {
        val subtotal = gross / (1 + rate / 100.0)
        val taxAmount = subtotal * (rate / 100.0)
        ItemTaxBreakdown(
            subtotal = roundSunat(subtotal),
            taxAmount = roundSunat(taxAmount),
            total = roundSunat(subtotal + taxAmount),
        )
    } else {
        val subtotal = gross
        val taxAmount = gross * (rate / 100.0)
        ItemTaxBreakdown(
            subtotal = roundSunat(subtotal),
            taxAmount = roundSunat(taxAmount),
            total = roundSunat(subtotal + taxAmount),
        )
    }
}
