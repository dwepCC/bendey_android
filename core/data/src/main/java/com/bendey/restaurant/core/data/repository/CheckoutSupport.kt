package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.CheckoutPaymentDraft
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentLine
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.billing.roundSunat

fun parseCheckoutPayments(drafts: List<CheckoutPaymentDraft>): List<CheckoutPaymentLine>? {
    if (drafts.isEmpty()) return null
    val lines = mutableListOf<CheckoutPaymentLine>()
    for (draft in drafts) {
        val amount = draft.amount.replace(',', '.').trim().toDoubleOrNull() ?: return null
        if (amount <= 0) return null
        lines += CheckoutPaymentLine(
            method = draft.method.ifBlank { "cash" },
            amount = roundSunat(amount),
            reference = draft.reference.trim(),
        )
    }
    return lines
}

fun needsOpenCashSessionForPayments(
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentLine>,
): Boolean = payments.any { line ->
    val method = methods.firstOrNull { it.code == line.method }
    method?.isCash == true || (methods.isEmpty() && line.method.equals("cash", ignoreCase = true))
}
