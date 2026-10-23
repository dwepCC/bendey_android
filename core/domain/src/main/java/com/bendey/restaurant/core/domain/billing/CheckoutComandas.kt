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

/**
 * Dividir la cuenta por unidades: cuántas unidades cobrar de cada línea (id → cantidad). Sin entrada,
 * la línea completa. Solo unidades enteras (el backend lo exige en comanda_quantities).
 */
object SplitUnits {
    /** ¿Se puede cobrar solo una parte de la línea? Hace falta al menos 2 unidades enteras. */
    fun canSplit(comanda: SessionComandaSummary): Boolean = !comanda.isDeliveryFee && kotlin.math.floor(comanda.quantity) >= 2.0

    /** Unidades que se cobran: la elegida (1…N-1) o la línea completa. */
    fun billQuantity(comanda: SessionComandaSummary, quantities: Map<Int, Int>): Double {
        val q = quantities[comanda.id] ?: return comanda.quantity
        return if (q >= 1 && q < comanda.quantity) q.toDouble() else comanda.quantity
    }

    /** ¿Alguna línea elegida se cobra solo en parte? Entonces no es el cobro de todo lo pendiente. */
    fun hasPartial(comandas: List<SessionComandaSummary>, quantities: Map<Int, Int>): Boolean =
        comandas.any { billQuantity(it, quantities) < it.quantity }

    /** Lo que viaja en comanda_quantities: solo las líneas elegidas que se cobran en parte. */
    fun payload(comandas: List<SessionComandaSummary>, quantities: Map<Int, Int>): List<ComandaQuantity> =
        comandas.filter { billQuantity(it, quantities) < it.quantity }
            .map { ComandaQuantity(it.id, billQuantity(it, quantities).toInt()) }

    /** 3.0 → «3», 2.5 → «2.5» (antes se truncaba con toInt()). */
    fun qtyLabel(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

    /** Nuevo mapa al mover el selector: llegar a la línea completa la saca del mapa (cobro de siempre). */
    fun set(quantities: Map<Int, Int>, comanda: SessionComandaSummary, value: Int): Map<Int, Int> =
        if (value >= comanda.quantity) quantities - comanda.id else quantities + (comanda.id to value.coerceAtLeast(1))
}

fun comandaPayableTotal(
    comanda: SessionComandaSummary,
    taxRatePercent: Double,
    taxConfig: TaxConfig = TaxConfig(),
    quantity: Double = comanda.quantity,
): Double = calcItem(
    unitPrice = comanda.unitPrice,
    quantity = quantity,
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
    quantities: Map<Int, Int> = emptyMap(),
): Double = roundMoney(comandas.sumOf { comandaPayableTotal(it, taxRatePercent, taxConfig, SplitUnits.billQuantity(it, quantities)) })

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0
