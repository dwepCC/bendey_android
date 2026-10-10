package com.bendey.restaurant.core.domain.rcshare

import com.bendey.restaurant.core.domain.delivery.ReportRange
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.domain.model.AppResult
import java.time.LocalDate
import java.util.Locale

/**
 * Reparto del Recargo al Consumo (RC), sección 24 del contrato. El RC ya va en el comprobante y entra a caja con
 * la venta; aquí solo se deja constancia de cuánto recibió cada trabajador.
 */

data class RcLine(val staffId: Int, val name: String, val amount: Double)

data class RcDistribution(
    val id: Int,
    /** `yyyy-MM-dd`. */
    val periodFrom: String,
    val periodTo: String,
    val amount: Double,
    val notes: String,
    val paidFromCash: Boolean,
    val booked: Boolean,
    val createdAt: String,
    val lines: List<RcLine>,
)

data class RcSummary(
    val from: String = "",
    val to: String = "",
    val collected: Double = 0.0,
    val distributed: Double = 0.0,
    val pending: Double = 0.0,
    val salesCount: Int = 0,
    /** Porcentaje promedio aplicado (p. ej. 10 = 10 %). */
    val avgRate: Double = 0.0,
    val history: List<RcDistribution> = emptyList(),
)

/** Una fila del reparto: `staffId` y el monto tal cual se tecleó (puede estar vacío). */
data class RcShareDraftLine(val staffId: Int, val amount: String = "")

data class RcShareInput(
    val from: String,
    val to: String,
    val lines: List<RcLine>,
    val notes: String,
    val payFromCash: Boolean,
)

interface RcShareRepository {
    /** [from] y [to] en `yyyy-MM-dd`. */
    suspend fun summary(from: String, to: String): AppResult<RcSummary>
    suspend fun distribute(input: RcShareInput): AppResult<Unit>
}

/** Ver el reparto: c.v o s.m. */
fun canSeeRcShare(permissions: List<String>?): Boolean {
    val p = permissions.orEmpty()
    return "c.v" in p || "s.m" in p
}

/** Repartir: solo s.m. */
fun canDistributeRc(permissions: List<String>?): Boolean = "s.m" in permissions.orEmpty()

object RcShareRules {
    const val MAX_DAYS = ReportRangeRules.MAX_DAYS

    /** Período por defecto: el mes actual hasta hoy. */
    fun defaultPeriod(today: LocalDate): ReportRange = ReportRange(today.withDayOfMonth(1), today)

    /** Mes anterior completo (atajo del selector). */
    fun previousMonth(today: LocalDate): ReportRange {
        val first = today.withDayOfMonth(1).minusMonths(1)
        return ReportRange(first, first.withDayOfMonth(first.lengthOfMonth()))
    }

    fun validatePeriod(from: LocalDate, to: LocalDate, today: LocalDate): String? =
        ReportRangeRules.validate(from, to, today)

    fun toCents(amount: Double): Long = Math.round(amount * 100.0)

    fun fromCents(cents: Long): Double = cents / 100.0

    /** Monto tecleado -> centavos; vacío o inválido = 0. */
    fun parseCents(text: String): Long {
        val v = text.replace(',', '.').trim().toDoubleOrNull() ?: return 0L
        return if (v <= 0.0) 0L else toCents(v)
    }

    /** Suma de los montos tecleados (en centavos, sin errores de coma flotante). */
    fun totalCents(lines: List<RcShareDraftLine>): Long = lines.sumOf { parseCents(it.amount) }

    fun total(lines: List<RcShareDraftLine>): Double = fromCents(totalCents(lines))

    /** «Dividir en partes iguales lo que falta»: lo que queda tras los montos ya puestos, entre quienes están vacíos. */
    fun splitRemainder(pending: Double, lines: List<RcShareDraftLine>): List<RcShareDraftLine> {
        val empty = lines.filter { parseCents(it.amount) == 0L }
        if (empty.isEmpty()) return lines
        val remaining = toCents(pending) - totalCents(lines)
        if (remaining <= 0L) return lines
        val base = remaining / empty.size
        val extra = (remaining % empty.size).toInt()
        val shares = HashMap<Int, Long>()
        empty.forEachIndexed { i, l -> shares[l.staffId] = base + if (i < extra) 1L else 0L }
        return lines.map { l ->
            val s = shares[l.staffId]
            if (s != null) l.copy(amount = String.format(Locale.US, "%.2f", fromCents(s))) else l
        }
    }

    /** Lo que falta por asignar: pendiente − lo tecleado (negativo = se pasó). */
    fun leftoverCents(pending: Double, lines: List<RcShareDraftLine>): Long = toCents(pending) - totalCents(lines)

    /** Mensaje si el reparto no se puede enviar; null si está bien. */
    fun validate(pending: Double, lines: List<RcShareDraftLine>): String? {
        val withAmount = lines.filter { parseCents(it.amount) > 0L }
        if (withAmount.isEmpty()) return "Indica cuánto recibe al menos un trabajador."
        if (lines.any { it.amount.isNotBlank() && parseCents(it.amount) == 0L }) return "Revisa los montos: deben ser mayores a cero."
        if (withAmount.map { it.staffId }.toSet().size != withAmount.size) return "Hay trabajadores repetidos."
        if (totalCents(lines) > toCents(pending)) return "El reparto supera lo que queda por repartir."
        return null
    }

    fun toInput(from: String, to: String, lines: List<RcShareDraftLine>, namesById: Map<Int, String>, notes: String, payFromCash: Boolean) =
        RcShareInput(
            from = from,
            to = to,
            lines = lines.filter { parseCents(it.amount) > 0L }
                .map { RcLine(it.staffId, namesById[it.staffId].orEmpty(), fromCents(parseCents(it.amount))) },
            notes = notes.trim(),
            payFromCash = payFromCash,
        )

    /** Mes ya terminado con RC sin repartir: aviso destacado. */
    fun isOverdue(summary: RcSummary, today: LocalDate): Boolean {
        if (summary.pending <= 0.009) return false
        val to = runCatching { LocalDate.parse(summary.to.take(10)) }.getOrNull() ?: return false
        return to.isBefore(today.withDayOfMonth(1))
    }
}

object RcShareText {
    fun money(v: Double): String = String.format(Locale.US, "S/ %.2f", v)

    fun percent(v: Double): String = String.format(Locale.US, "%.1f %%", v)

    fun salesCount(n: Int): String = if (n == 1) "1 venta" else "$n ventas"

    const val HELP =
        "El recargo al consumo se cobra para repartirlo entre los trabajadores. Si no se reparte todo, SUNAT lo considera " +
            "ingreso del negocio (Informe 049-2014). Aquí dejas constancia del reparto. Confirma el criterio con tu contador."

    fun overdueNotice(pending: Double): String =
        "Quedan ${money(pending)} de recargo al consumo sin repartir en un mes que ya terminó. " +
            "Si no se reparten, SUNAT los considera ingreso del negocio."
}
