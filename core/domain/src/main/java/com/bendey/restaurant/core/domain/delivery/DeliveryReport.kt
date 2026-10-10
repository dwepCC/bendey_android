package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.time.PeruDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Reporte de delivery y para recoger (D5) -- `GET /api/restaurant/delivery/report`. */

data class DeliveryReportSummary(
    val orders: Int = 0,
    val delivered: Int = 0,
    val inProgress: Int = 0,
    val cancelled: Int = 0,
    val gross: Double = 0.0,
    val avgTicket: Double = 0.0,
    val lostAmount: Double = 0.0,
    /** 0 a 1. */
    val cancelRate: Double = 0.0,
)

data class DeliveryReportChannel(
    /** marketplace | local */
    val source: String,
    /** delivery | takeaway */
    val type: String,
    val orders: Int,
    val cancelled: Int,
    val amount: Double,
)

/** Promedio en minutos y cuántos pedidos lo respaldan (0 muestras = sin dato). */
data class DeliveryReportTime(val minutes: Double, val samples: Int) {
    val hasData: Boolean get() = samples > 0
}

data class DeliveryReportTimes(
    val accept: DeliveryReportTime = DeliveryReportTime(0.0, 0),
    val prep: DeliveryReportTime = DeliveryReportTime(0.0, 0),
    val delivery: DeliveryReportTime = DeliveryReportTime(0.0, 0),
    val total: DeliveryReportTime = DeliveryReportTime(0.0, 0),
)

data class DeliveryReportCancellation(
    val reason: String,
    val label: String,
    val orders: Int,
    val amount: Double,
    val sample: String,
)

data class DeliveryReportPayment(
    val mode: String,
    val method: String,
    val label: String,
    val orders: Int,
    val amount: Double,
)

data class DeliveryReportDriver(
    val driverId: Int,
    val name: String,
    val assigned: Int,
    val delivered: Int,
    val failed: Int,
    val rejected: Int,
    val avgDeliveryMinutes: Double,
    val earnings: Double,
    val cashCollected: Double,
    val cashPending: Double,
)

data class DeliveryReportRefunds(val pending: Int = 0, val pendingAmount: Double = 0.0)

data class DeliveryReport(
    val summary: DeliveryReportSummary = DeliveryReportSummary(),
    val channels: List<DeliveryReportChannel> = emptyList(),
    val times: DeliveryReportTimes = DeliveryReportTimes(),
    val cancellations: List<DeliveryReportCancellation> = emptyList(),
    val payments: List<DeliveryReportPayment> = emptyList(),
    val drivers: List<DeliveryReportDriver> = emptyList(),
    val refunds: DeliveryReportRefunds = DeliveryReportRefunds(),
    val truncated: Boolean = false,
)

interface DeliveryReportRepository {
    /** [from] y [to] en `yyyy-MM-dd`. */
    suspend fun report(from: String, to: String): AppResult<DeliveryReport>
}

/** Rangos del selector del reporte. */
enum class ReportPreset(val label: String) {
    TODAY("Hoy"),
    LAST_7("7 días"),
    LAST_30("30 días"),
    CUSTOM("Elegir fechas"),
}

data class ReportRange(val from: LocalDate, val to: LocalDate) {
    val fromIso: String get() = from.toString()
    val toIso: String get() = to.toString()
    val days: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
}

object ReportRangeRules {
    const val MAX_DAYS = 92

    /** Hoy de Lima (nunca el del dispositivo). */
    fun todayLima(): LocalDate = LocalDate.now(PeruDateTime.ZONE)

    /** Hoy = solo hoy; 7 días = hoy y los 6 anteriores; 30 días = hoy y los 29 anteriores. CUSTOM no tiene rango propio. */
    fun preset(preset: ReportPreset, today: LocalDate): ReportRange? = when (preset) {
        ReportPreset.TODAY -> ReportRange(today, today)
        ReportPreset.LAST_7 -> ReportRange(today.minusDays(6), today)
        ReportPreset.LAST_30 -> ReportRange(today.minusDays(29), today)
        ReportPreset.CUSTOM -> null
    }

    /** Mensaje si el rango no sirve (desde después de hasta, futuro, más de 92 días); null si está bien. */
    fun validate(from: LocalDate, to: LocalDate, today: LocalDate): String? = when {
        from.isAfter(to) -> "La fecha «Desde» no puede ser posterior a «Hasta»."
        to.isAfter(today) -> "«Hasta» no puede ser una fecha futura."
        ReportRange(from, to).days > MAX_DAYS -> "El rango máximo es de $MAX_DAYS días."
        else -> null
    }

    /** «dd/MM/yyyy» para mostrar. */
    fun display(date: LocalDate): String = PeruDateTime.formatDate(date.toString()) ?: date.toString()
}

/** Reporte: lo ven c.v o s.m, y nunca el repartidor. */
fun canSeeDeliveryReport(permissions: List<String>?, employeeType: String?): Boolean {
    if (employeeType == "driver") return false
    val p = permissions.orEmpty()
    return "c.v" in p || "s.m" in p
}

/** Textos y formatos del reporte (lógica pura). */
object DeliveryReportText {
    fun money(v: Double): String = String.format(java.util.Locale.US, "S/ %.2f", v)

    /** 0.1234 -> "12.3 %". */
    fun percent(rate: Double): String = String.format(java.util.Locale.US, "%.1f %%", (rate * 100.0).coerceIn(0.0, 100.0))

    /** Minutos con muestras; sin muestras "Sin datos". */
    fun time(t: DeliveryReportTime): String =
        if (!t.hasData) "Sin datos"
        else "${String.format(java.util.Locale.US, "%.0f", t.minutes)} min (${if (t.samples == 1) "1 pedido" else "${t.samples} pedidos"})"

    fun channelLabel(source: String, type: String): String {
        val s = if (source == "marketplace") "Marketplace" else "Local"
        val t = if (type == "takeaway") "Recoger" else "Delivery"
        return "$s · $t"
    }

    fun refundsNotice(r: DeliveryReportRefunds): String? =
        if (r.pending > 0) "${r.pending} ${if (r.pending == 1) "cobro" else "cobros"} por devolver (${money(r.pendingAmount)})" else null

    const val TRUNCATED_NOTICE = "El rango tenía demasiados pedidos y se recortó: los números pueden estar incompletos. Prueba con menos días."
}

/** Insignia de pedido retrasado y hora prometida (D3). */
object DeliveryLateness {
    fun isLate(card: DeliveryCard): Boolean = card.lateMinutes > 0

    /** «Retrasado 12 min» o null. */
    fun lateLabel(card: DeliveryCard): String? = if (isLate(card)) "Retrasado ${card.lateMinutes} min" else null

    /** «Prometido 14:35» (hora de Lima) solo si hay hora prometida y no está retrasado. */
    fun promisedLabel(card: DeliveryCard): String? {
        if (isLate(card)) return null
        val t = PeruDateTime.formatTime(card.promisedAt) ?: return null
        return "Prometido $t"
    }
}
