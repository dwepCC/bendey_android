package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.model.AppResult

/** Un pedido cobrado en efectivo contra entrega que aun no se rindio. */
data class SettlementOrder(val paymentId: Int, val orderCode: String, val customerName: String, val amount: Double)

/** Lo pendiente de rendir de un repartidor (o de quien cobro). */
data class SettlementGroup(
    val collectorKind: String,
    val collectorId: Int,
    val collectorName: String,
    val total: Double,
    val orders: List<SettlementOrder>,
)

/** Pedido cancelado con el efectivo ya cobrado: queda «por devolver» al cliente. */
data class RefundPending(val paymentId: Int, val orderCode: String, val amount: Double, val collectorName: String, val cancelReason: String)

data class SettlementsPending(val groups: List<SettlementGroup>, val refunds: List<RefundPending>)

data class SettlementRecord(
    val id: Int,
    val collectorName: String,
    val receivedByName: String,
    val ordersCount: Int,
    val receivedTotal: Double,
    val difference: Double,
    val createdAt: String,
)

/** Rendicion (D2c): el repartidor entrega al cajero el efectivo cobrado; cada pedido se factura como Nota de Venta. */
interface SettlementRepository {
    suspend fun pending(): AppResult<SettlementsPending>
    suspend fun history(): AppResult<List<SettlementRecord>>

    /** Devuelve un aviso (o null) cuando la rendicion se registro pero la diferencia no pudo anotarse en caja. */
    suspend fun receive(group: SettlementGroup, paymentIds: List<Int>, received: Double, reason: String?): AppResult<String?>
    suspend fun resolveRefund(paymentId: Int, note: String): AppResult<Unit>
}

/** Diferencia = recibido - esperado, redondeada a centimos; negativa = faltante. */
fun settlementDifference(expected: Double, received: Double): Double =
    Math.round((received - expected) * 100.0) / 100.0

fun settlementHasDifference(expected: Double, received: Double): Boolean =
    kotlin.math.abs(settlementDifference(expected, received)) >= 0.01
