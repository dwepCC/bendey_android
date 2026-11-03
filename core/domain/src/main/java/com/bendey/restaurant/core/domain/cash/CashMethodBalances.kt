package com.bendey.restaurant.core.domain.cash

/**
 * Montos por método (Yape, Plin…) que se mandan al abrir o al cerrar la caja, a partir de lo que el
 * cajero digitó. Lo vacío o inválido no se manda: al abrir, el backend lo completa con lo sugerido; al
 * cerrar, queda "sin confirmar" (que no es lo mismo que 0).
 */
fun methodBalanceInputs(amounts: Map<String, String>): List<CashMethodBalanceInput> =
    amounts.mapNotNull { (method, raw) ->
        val value = raw.trim().replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
        if (value < 0 || value.isNaN() || value.isInfinite()) return@mapNotNull null
        CashMethodBalanceInput(method = method, amount = Math.round(value * 100) / 100.0)
    }

/**
 * Una línea por método para los reportes en texto (exportación y arqueo):
 * "Yape: abrió 55.00 · +80.00 · −20.00 · esperado 115.00 · confirmado 110.00 (falta 5.00)".
 */
fun methodBalanceLines(rows: List<CashMethodBalance>, money: (Double) -> String): List<String> =
    rows.map { r ->
        val confirmado = when {
            r.counted == null -> "sin confirmar"
            r.difference == null || kotlin.math.abs(r.difference) < 0.005 -> "confirmado ${money(r.counted)}"
            r.difference > 0 -> "confirmado ${money(r.counted)} (sobra ${money(r.difference)})"
            else -> "confirmado ${money(r.counted)} (falta ${money(-r.difference)})"
        }
        "${r.name.ifBlank { r.method }}: abrió ${money(r.openingBalance)} · +${money(r.inflows)} · " +
            "−${money(r.outflows)} · esperado ${money(r.expected)} · $confirmado"
    }

/** Monto sugerido escrito para el campo: "55" y no "55.0", "12.5" queda "12.5". */
fun suggestedAmountText(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else "%.2f".format(java.util.Locale.US, value).trimEnd('0')
