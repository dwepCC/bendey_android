package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.model.AppResult
import java.util.Locale

/** Lo que se le debe a un repartidor por entregas ya hechas y aún sin liquidar. */
data class PendingEarning(
    val driverId: Int,
    val name: String,
    val deliveries: Int,
    val total: Double,
)

/** Liquidación hecha: salió de la caja de quien pagó. */
data class EarningsPayout(
    val name: String,
    val amount: Double,
    val deliveries: Int,
    /** false = el pago quedó registrado pero su movimiento de caja no. */
    val booked: Boolean,
)

interface DeliveryEarningsRepository {
    suspend fun pending(): AppResult<List<PendingEarning>>

    /** Liquida TODO lo pendiente del repartidor. */
    suspend fun payout(driverId: Int): AppResult<EarningsPayout>
}

object DeliveryEarningsCopy {
    const val TAB = "Ganancias"
    const val INTRO = "Lo que se les debe a los repartidores por sus entregas. Al pagar, el efectivo sale de tu caja."
    const val EMPTY = "Nadie tiene ganancias por pagar."
    const val NO_PAY_PERMISSION = "Para pagar ganancias necesitas permiso de cobro (caja)."
    const val PAY = "Pagar ganancias"

    fun money(v: Double): String = String.format(Locale.US, "S/ %.2f", v)

    fun deliveriesLabel(n: Int): String = if (n == 1) "1 entrega" else "$n entregas"

    fun confirmTitle(e: PendingEarning): String = "Pagar a ${e.name}"

    fun confirmText(e: PendingEarning): String =
        "Vas a pagar ${money(e.total)} por ${deliveriesLabel(e.deliveries)}. Este monto sale de TU caja abierta."

    fun paidMessage(p: EarningsPayout): String {
        val base = "Pagaste ${money(p.amount)} a ${p.name} por ${deliveriesLabel(p.deliveries)}."
        return if (p.booked) base else "$base El pago quedó anotado, pero no se registró en la caja: revísala."
    }
}

/** La pestaña «Ganancias» la ven o.ch y s.m (nunca el repartidor); solo o.ch paga. */
fun canSeeDriverEarnings(permissions: List<String>?, employeeType: String?): Boolean {
    if (employeeType == "driver") return false
    val p = permissions.orEmpty()
    return "o.ch" in p || "s.m" in p
}

fun canPayDriverEarnings(permissions: List<String>?, employeeType: String?): Boolean =
    employeeType != "driver" && "o.ch" in permissions.orEmpty()
