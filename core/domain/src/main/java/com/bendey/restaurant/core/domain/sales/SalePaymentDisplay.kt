package com.bendey.restaurant.core.domain.sales

import java.util.Locale
import kotlin.math.abs

data class SalePaymentLine(
    val method: String,
    val amount: Double,
)

private val PAYMENT_METHOD_ES = mapOf(
    "cash" to "Efectivo",
    "efectivo" to "Efectivo",
    "yape" to "Yape",
    "plin" to "Plin",
    "transferencia" to "Transferencia",
    "transfer" to "Transferencia",
    "tarjeta" to "Tarjeta",
    "card" to "Tarjeta",
    "debito" to "Tarjeta débito",
    "credito" to "Crédito",
    "credit" to "Crédito",
)

fun salePaymentMethodLabelEs(code: String?): String {
    val key = code?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (key.isEmpty()) return "—"
    PAYMENT_METHOD_ES[key]?.let { return it }
    return if (key.length <= 32) key.replaceFirstChar { it.uppercase(Locale.ROOT) } else key
}

fun salePaymentMethodLabel(
    code: String?,
    customNames: Map<String, String> = emptyMap(),
): String {
    val normalized = normalizePaymentMethodCode(code)
    if (normalized.isEmpty()) return "—"
    customNames[normalized]?.let { return it }
    return salePaymentMethodLabelEs(normalized)
}

fun normalizePaymentMethodCode(code: String?): String =
    code?.trim()?.lowercase(Locale.ROOT).orEmpty()

fun paymentMethodMatchesFilter(methodCode: String, filterCode: String): Boolean {
    val method = normalizePaymentMethodCode(methodCode)
    val filter = normalizePaymentMethodCode(filterCode)
    if (filter.isEmpty()) return false
    if (method == filter) return true
    if ((method == "cash" || method == "efectivo") && (filter == "cash" || filter == "efectivo")) return true
    if ((method == "credit" || method == "credito") && (filter == "credit" || filter == "credito")) return true
    return false
}

fun resolveSalePayments(sale: SaleSummary): List<SalePaymentLine> {
    if (sale.payments.isNotEmpty()) return sale.payments
    val method = sale.paymentMethod?.trim().orEmpty()
    if (method.isEmpty()) return emptyList()
    return listOf(SalePaymentLine(method = method, amount = sale.total))
}

fun sumPaymentsForMethod(sale: SaleSummary, filterCode: String): Double =
    resolveSalePayments(sale)
        .filter { paymentMethodMatchesFilter(it.method, filterCode) }
        .sumOf { abs(it.amount) }

fun formatPaymentsCompact(
    sale: SaleSummary,
    customNames: Map<String, String> = emptyMap(),
    maxChars: Int = 36,
): String {
    val payments = resolveSalePayments(sale)
    if (payments.isEmpty()) return "—"
    val detailed = payments.joinToString(", ") { payment ->
        "${salePaymentMethodLabel(payment.method, customNames)} (S/ ${fmtMoney(payment.amount)})"
    }
    if (detailed.length <= maxChars) return detailed
    return payments.joinToString(" + ") { salePaymentMethodLabel(it.method, customNames) }.ifBlank { detailed }
}

fun formatOtherPaymentMethods(
    sale: SaleSummary,
    filterCode: String,
    customNames: Map<String, String> = emptyMap(),
): String {
    val others = resolveSalePayments(sale).filter { !paymentMethodMatchesFilter(it.method, filterCode) }
    if (others.isEmpty()) return "—"
    return others.joinToString(", ") { payment ->
        "${salePaymentMethodLabel(payment.method, customNames)} S/ ${fmtMoney(payment.amount)}"
    }
}

private fun fmtMoney(value: Double): String = String.format(Locale.US, "%.2f", value)
