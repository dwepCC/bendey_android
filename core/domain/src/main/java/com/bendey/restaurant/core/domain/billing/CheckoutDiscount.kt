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
): Double {
    val base = roundSunat(rawTotal.coerceAtLeast(0.0))
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
): Double {
    val discount = calcCheckoutDiscountAmount(rawTotal, mode, value)
    return roundSunat((roundSunat(rawTotal) - discount).coerceAtLeast(0.0))
}

fun paidCoversTotal(paid: Double, expected: Double): Boolean =
    roundDisplay(paid) + 0.009 >= roundDisplay(expected)

data class CheckoutPaymentDraft(
    val method: String,
    val amount: String,
    val reference: String = "",
)
