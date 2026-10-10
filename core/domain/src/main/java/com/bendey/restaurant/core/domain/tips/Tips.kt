package com.bendey.restaurant.core.domain.tips

import com.bendey.restaurant.core.domain.model.AppResult
import java.util.Locale
import kotlin.math.floor

/**
 * Propinas voluntarias (sección 22 del contrato). NO van en el comprobante ni suman al total de la venta:
 * son plata del personal. Se anotan al cobrar, se acreditan al mozo y se liquidan desde la caja.
 */

/** Lo que el cajero teclea en el bloque «Propina» del cobro. */
data class TipDraft(
    /** Monto en S/ tal cual se escribió (vacío = sin propina). */
    val amount: String = "",
    /** Medio con el que la dejó el cliente; null = el medio principal del cobro. */
    val method: String? = null,
    /** Mozo; null = el de la mesa (en venta rápida no hay mesa: la propina queda «sin mozo» hasta asignarla). */
    val staffId: Int? = null,
)

/** Propina lista para enviar (`tip_amount`, `tip_method`, `tip_staff_id`). */
data class TipInput(val amount: Double, val method: String, val staffId: Int?)

object TipRules {
    const val MAX_AMOUNT = 5000.0

    /** Atajos de monto: porcentaje del total sin propina. */
    val SHORTCUT_PERCENTS: List<Int> = listOf(5, 10)

    /** Monto tecleado -> número, o null si está vacío o no es un número. */
    fun parse(text: String): Double? = text.replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    /** Hay propina cuando el monto tecleado es mayor a cero. */
    fun hasTip(draft: TipDraft): Boolean = (parse(draft.amount) ?: 0.0) > 0.0

    /** Mensaje si el monto no sirve (negativo, mayor a S/ 5000, no numérico); null si está bien o vacío. */
    fun validate(text: String): String? {
        if (text.isBlank()) return null
        val v = parse(text) ?: return "Escribe un monto válido."
        return when {
            v < 0.0 -> "La propina no puede ser negativa."
            v > MAX_AMOUNT -> "La propina no puede pasar de S/ 5000."
            else -> null
        }
    }

    /** Propina sugerida: [percent] % de [total], redondeada al céntimo (mitad hacia arriba). */
    fun suggest(total: Double, percent: Int): Double {
        if (total <= 0.0 || percent <= 0) return 0.0
        return floor(total * percent + 0.5 + 1e-9) / 100.0
    }

    /** «12.35» para el campo de monto (siempre con punto y 2 decimales). */
    fun format(amount: Double): String = String.format(Locale.US, "%.2f", amount)

    /**
     * Propina a enviar, o null si no hay (monto vacío/0) o es inválida. [defaultMethod] es el medio principal
     * del cobro cuando el cajero no eligió otro.
     */
    fun toInput(draft: TipDraft, defaultMethod: String): TipInput? {
        val v = parse(draft.amount) ?: return null
        if (v <= 0.0 || v > MAX_AMOUNT) return null
        val cents = Math.round(v * 100.0) / 100.0
        return TipInput(cents, (draft.method ?: defaultMethod).ifBlank { "cash" }, draft.staffId)
    }
}

/** Texto fijo de ayuda del bloque. */
object TipCopy {
    const val TITLE = "Propina"
    const val LINE_NOT_IN_RECEIPT = "Propina (no va en el comprobante)"
    const val HELP = "La propina es voluntaria: no suma al total de la venta ni al vuelto, y se entrega al mozo."
}

// -------- Pantalla «Propinas» --------

/** Propinas por pagar de un mozo (`GET /tips/pending`). `staffId` 0 = sin mozo. */
data class PendingTipStaff(
    val staffId: Int,
    val name: String,
    val count: Int,
    val total: Double,
    /** Propinas sin movimiento de caja anotado. */
    val unbooked: Int,
)

data class TipStaffRow(val staffId: Int, val name: String, val count: Int, val total: Double, val paid: Double, val pending: Double)

data class TipMethodRow(val method: String, val count: Int, val total: Double)

data class TipsReport(
    val total: Double = 0.0,
    val count: Int = 0,
    val paid: Double = 0.0,
    val pending: Double = 0.0,
    val staff: List<TipStaffRow> = emptyList(),
    val methods: List<TipMethodRow> = emptyList(),
)

data class TipRecent(val amount: Double, val method: String, val createdAt: String, val paid: Boolean)

data class MyTips(val pending: Double = 0.0, val paid: Double = 0.0, val recent: List<TipRecent> = emptyList())

data class TipPayoutResult(val staffName: String, val amount: Double, val tipsCount: Int)

interface TipsRepository {
    suspend fun pending(): AppResult<List<PendingTipStaff>>
    suspend fun payout(staffId: Int): AppResult<TipPayoutResult>

    /** [from] y [to] en `yyyy-MM-dd`. */
    suspend fun report(from: String, to: String): AppResult<TipsReport>
    suspend fun mine(): AppResult<MyTips>
}

/** Pagar propinas: o.ch o s.m. */
fun canPayTips(permissions: List<String>?): Boolean {
    val p = permissions.orEmpty()
    return "o.ch" in p || "s.m" in p
}

/** Reporte de propinas: c.v o s.m. */
fun canSeeTipsReport(permissions: List<String>?): Boolean {
    val p = permissions.orEmpty()
    return "c.v" in p || "s.m" in p
}

/** Textos de la pantalla (lógica pura). */
object TipsText {
    fun money(v: Double): String = String.format(Locale.US, "S/ %.2f", v)

    fun count(n: Int): String = if (n == 1) "1 propina" else "$n propinas"

    fun staffName(row: PendingTipStaff): String = row.name.ifBlank { if (row.staffId == 0) "Sin mozo asignado" else "Sin nombre" }

    /** Confirmación antes de pagar: total y aviso de que sale de la caja de quien paga. */
    fun payoutConfirm(row: PendingTipStaff): String =
        "Vas a pagar ${money(row.total)} en propinas (${count(row.count)}) a ${staffName(row)}. " +
            "El efectivo sale de TU caja abierta y queda registrado como «Pago de propinas»."

    /** Total general por pagar. */
    fun totalPending(rows: List<PendingTipStaff>): Double = rows.sumOf { it.total }

    fun methodLabel(code: String): String = when (code.lowercase()) {
        "", "cash" -> "Efectivo"
        "yape" -> "Yape"
        "plin" -> "Plin"
        "card" -> "Tarjeta"
        "transfer" -> "Transferencia"
        else -> code.replaceFirstChar { it.uppercase() }
    }
}

/** Pestañas de la pantalla «Propinas». */
enum class PropinasTab(val label: String) {
    POR_PAGAR("Por pagar"),
    REPORTE("Reporte"),
    MIS_PROPINAS("Mis propinas"),
}

object PropinasTabs {
    /** Pestañas visibles: «Por pagar» con o.ch/s.m, «Reporte» con c.v/s.m y «Mis propinas» siempre. */
    fun available(permissions: List<String>?): List<PropinasTab> = buildList {
        if (canPayTips(permissions)) add(PropinasTab.POR_PAGAR)
        if (canSeeTipsReport(permissions)) add(PropinasTab.REPORTE)
        add(PropinasTab.MIS_PROPINAS)
    }
}
