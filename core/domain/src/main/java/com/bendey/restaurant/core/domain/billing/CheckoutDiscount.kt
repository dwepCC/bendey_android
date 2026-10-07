package com.bendey.restaurant.core.domain.billing

import kotlin.math.round

enum class CheckoutDiscountMode {
    PERCENT,
    AMOUNT,
}

fun roundSunat(value: Double): Double = round(value * 1_000_000.0) / 1_000_000.0

fun roundDisplay(value: Double): Double = round(value * 100.0) / 100.0

fun calcCheckoutDiscountAmount(
    rawTotal: Double,
    mode: CheckoutDiscountMode,
    value: Double,
    /** Parte del total que no admite descuento (tarifa de delivery, D2.0): se resta de la base. */
    nonDiscountableAmount: Double = 0.0,
): Double {
    val base = roundSunat((rawTotal - nonDiscountableAmount.coerceAtLeast(0.0)).coerceAtLeast(0.0))
    if (base <= 0) return 0.0
    val rawValue = value.coerceAtLeast(0.0)
    return when (mode) {
        CheckoutDiscountMode.PERCENT -> {
            val pct = rawValue.coerceAtMost(100.0)
            roundSunat(base * (pct / 100.0))
        }
        CheckoutDiscountMode.AMOUNT -> roundSunat(rawValue.coerceAtMost(base))
    }
}

fun calcPayableTotal(
    rawTotal: Double,
    mode: CheckoutDiscountMode,
    value: Double,
    nonDiscountableAmount: Double = 0.0,
): Double {
    val discount = calcCheckoutDiscountAmount(rawTotal, mode, value, nonDiscountableAmount)
    return roundSunat((roundSunat(rawTotal) - discount).coerceAtLeast(0.0))
}

/**
 * `calcPayableTotal` + RC (ver [calcServiceChargePreview]) — el monto que de verdad hay que cobrar.
 * Única función para esta suma: antes Pos y Mesa la repetían cada uno por su lado en su
 * `checkoutPayableTotal`, y el diálogo de cobro la repetía una tercera vez solo para pintar el
 * total — cualquier cambio futuro a esta fórmula (ej. redondeo) tenía que tocarse en 3 sitios.
 */
fun calcPayableTotalWithServiceCharge(
    rawTotal: Double,
    mode: CheckoutDiscountMode,
    value: Double,
    serviceChargeRate: Double,
    serviceChargeEnabled: Boolean,
    taxRatePercent: Double,
    /** Tarifa de delivery (D2.0): no entra a la base del descuento ni del RC, se suma después. */
    nonDiscountableAmount: Double = 0.0,
): Double {
    val nd = nonDiscountableAmount.coerceAtLeast(0.0)
    val discountAmount = calcCheckoutDiscountAmount(rawTotal, mode, value, nd)
    val serviceCharge = calcServiceChargePreview(
        total = (rawTotal - nd).coerceAtLeast(0.0),
        discountAmount = discountAmount,
        rate = serviceChargeRate,
        enabled = serviceChargeEnabled,
        taxRatePercent = taxRatePercent,
    )
    return roundSunat(calcPayableTotal(rawTotal, mode, value, nd) + serviceCharge)
}

fun paidCoversTotal(paid: Double, expected: Double): Boolean =
    roundDisplay(paid) + 0.009 >= roundDisplay(expected)

data class CheckoutPaymentDraft(
    val method: String,
    val amount: String,
    val reference: String = "",
)
