package com.bendey.restaurant.core.domain.billing

/** Fallback alineado con backend `pkg/tax.DefaultConfig()` cuando el tenant no tiene tasa. */
const val DEFAULT_TAX_RATE_PERCENT = 18.0

data class TaxConfig(
    val taxRate: Double = DEFAULT_TAX_RATE_PERCENT,
    val igvRegime: String = "standard",
    val taxBenefitZone: Boolean = false,
)

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
        else -> if (taxConfig.taxBenefitZone && taxConfig.igvRegime == "exonerated") {
            0.0
        } else {
            taxRatePercent
        }
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
