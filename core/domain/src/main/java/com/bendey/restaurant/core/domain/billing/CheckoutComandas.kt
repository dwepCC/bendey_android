package com.bendey.restaurant.core.domain.billing

import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import kotlin.math.round

data class ComandaCheckoutRow(
    val comanda: SessionComandaSummary,
    val orderNumber: Int,
)

data class PartitionedComandas(
    val pending: List<ComandaCheckoutRow>,
    val billed: List<ComandaCheckoutRow>,
)

fun SessionComandaSummary.isComandaBilled(): Boolean =
    billedSaleId != null && billedSaleId > 0

fun SessionComandaSummary.isComandaBillable(): Boolean {
    if (cancelledAt != null) return false
    return !isComandaBilled()
}

fun partitionComandasFromSession(session: TableSessionDetail?): PartitionedComandas {
    if (session == null) return PartitionedComandas(emptyList(), emptyList())
    val pending = mutableListOf<ComandaCheckoutRow>()
    val billed = mutableListOf<ComandaCheckoutRow>()
    for (order in session.orders) {
        for (comanda in order.comandas) {
            val row = ComandaCheckoutRow(comanda, order.orderNumber)
            when {
                comanda.isComandaBilled() -> billed += row
                comanda.isComandaBillable() -> pending += row
            }
        }
    }
    return PartitionedComandas(pending, billed)
}

fun comandaPayableTotal(
    comanda: SessionComandaSummary,
    taxRatePercent: Double,
    taxConfig: TaxConfig = TaxConfig(),
): Double = calcItem(
    unitPrice = comanda.unitPrice,
    quantity = comanda.quantity,
    discount = 0.0,
    igvAffectationType = comanda.igvAffectationType ?: "10",
    priceIncludesIgv = comanda.priceIncludesIgv ?: true,
    taxRatePercent = taxRatePercent,
    taxConfig = taxConfig,
).total

fun sumComandasPayableTotal(
    comandas: List<SessionComandaSummary>,
    taxRatePercent: Double,
    taxConfig: TaxConfig = TaxConfig(),
): Double = roundMoney(comandas.sumOf { comandaPayableTotal(it, taxRatePercent, taxConfig) })

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0
