package com.bendey.restaurant.feature.caja

import org.json.JSONObject

data class ArqueoDenomination(
    val value: String,
    val label: String,
    val kind: ArqueoKind,
)

enum class ArqueoKind { BILL, COIN }

val ARQUEO_DENOMINATIONS: List<ArqueoDenomination> = listOf(
    ArqueoDenomination("200", "Billete 200", ArqueoKind.BILL),
    ArqueoDenomination("100", "Billete 100", ArqueoKind.BILL),
    ArqueoDenomination("50", "Billete 50", ArqueoKind.BILL),
    ArqueoDenomination("20", "Billete 20", ArqueoKind.BILL),
    ArqueoDenomination("10", "Billete 10", ArqueoKind.BILL),
    ArqueoDenomination("5", "Moneda 5", ArqueoKind.COIN),
    ArqueoDenomination("2", "Moneda 2", ArqueoKind.COIN),
    ArqueoDenomination("1", "Moneda 1", ArqueoKind.COIN),
    ArqueoDenomination("0.5", "Moneda 0.50", ArqueoKind.COIN),
    ArqueoDenomination("0.2", "Moneda 0.20", ArqueoKind.COIN),
    ArqueoDenomination("0.1", "Moneda 0.10", ArqueoKind.COIN),
    ArqueoDenomination("0.05", "Moneda 0.05", ArqueoKind.COIN),
    ArqueoDenomination("0.01", "Moneda 0.01", ArqueoKind.COIN),
)

fun emptyArqueo(): Map<String, Int> =
    ARQUEO_DENOMINATIONS.associate { it.value to 0 }

fun sumArqueo(values: Map<String, Int>): Double =
    ARQUEO_DENOMINATIONS.sumOf { denom ->
        denom.value.toDouble() * (values[denom.value] ?: 0)
    }

fun parseArqueoJson(json: String?): Map<String, Int> {
    val base = emptyArqueo().toMutableMap()
    if (json.isNullOrBlank()) return base
    return try {
        val obj = JSONObject(json)
        obj.keys().forEach { key ->
            base[key] = obj.optInt(key, 0).coerceAtLeast(0)
        }
        base
    } catch (_: Exception) {
        base
    }
}

fun formatArqueoReportText(
    branchName: String?,
    openedAt: String?,
    openingBalance: Double,
    expectedBalance: Double,
    arqueo: Map<String, Int>,
    currency: java.text.NumberFormat,
): String = buildString {
    appendLine("ARQUEO DE CAJA")
    branchName?.let { appendLine("Sucursal: $it") }
    openedAt?.let { appendLine("Apertura: $it") }
    appendLine("Saldo apertura: ${currency.format(openingBalance)}")
    appendLine("Saldo sistema: ${currency.format(expectedBalance)}")
    appendLine()
    appendLine("Denominaciones:")
    ARQUEO_DENOMINATIONS.forEach { denom ->
        val qty = arqueo[denom.value] ?: 0
        if (qty > 0) {
            val subtotal = denom.value.toDouble() * qty
            appendLine("  ${denom.label}: $qty = ${currency.format(subtotal)}")
        }
    }
    appendLine()
    appendLine("Total contado: ${currency.format(sumArqueo(arqueo))}")
    val diff = sumArqueo(arqueo) - expectedBalance
    appendLine("Diferencia: ${currency.format(diff)}")
}

fun formatSessionReportText(
    report: com.bendey.restaurant.core.domain.cash.CashSessionReport,
    currency: java.text.NumberFormat,
): String = buildString {
    val session = report.session
    appendLine("REPORTE DE CAJA #${session.id}")
    session.branchName?.let { appendLine("Sucursal: $it") }
    session.openedAt?.let { appendLine("Apertura: $it") }
    session.closedAt?.let { appendLine("Cierre: $it") }
    session.openedByName?.let { appendLine("Operador: $it") }
    appendLine()
    appendLine("Apertura: ${currency.format(session.openingBalance)}")
    appendLine("Ingresos: ${currency.format(report.totalIncome)}")
    appendLine("Egresos: ${currency.format(report.totalExpense)}")
    appendLine("Ventas netas: ${currency.format(report.totalNetSales)}")
    if (report.totalVoidedSales > 0) {
        appendLine("Ventas anuladas: ${currency.format(report.totalVoidedSales)}")
    }
    appendLine("Saldo final: ${currency.format(report.finalBalance)}")
    if (report.salesByMethod.isNotEmpty()) {
        appendLine()
        appendLine("Ventas por método:")
        report.salesByMethod.forEach { row ->
            appendLine("  ${row.method}: ${currency.format(row.total)}")
        }
    }
    if (report.nonCashSalesByMethod.isNotEmpty()) {
        appendLine()
        appendLine("Ventas no efectivo:")
        report.nonCashSalesByMethod.forEach { row ->
            appendLine("  ${row.method}: ${currency.format(row.total)}")
        }
    }
    if (report.incomeDetail.isNotEmpty()) {
        appendLine()
        appendLine("Detalle ingresos:")
        report.incomeDetail.take(30).forEach { row ->
            appendLine("  ${row.date} ${row.docNumber} ${currency.format(row.amount)}")
        }
    }
    if (report.expenseDetail.isNotEmpty()) {
        appendLine()
        appendLine("Detalle egresos:")
        report.expenseDetail.take(30).forEach { row ->
            appendLine("  ${row.date} ${row.reference} ${currency.format(row.amount)}")
        }
    }
    if (report.cancelledSalesDetail.isNotEmpty()) {
        appendLine()
        appendLine("Ventas anuladas:")
        report.cancelledSalesDetail.take(30).forEach { row ->
            appendLine("  ${row.date} ${row.docNumber} ${row.paymentMethod} ${currency.format(row.amount)}")
        }
    }
}
