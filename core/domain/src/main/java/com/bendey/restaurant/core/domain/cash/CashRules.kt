package com.bendey.restaurant.core.domain.cash

/** Lo que muestra el chip de caja de la barra superior. */
sealed interface CashChipState {
    /** Sin permiso de caja (mozo), sin sucursal o aún sin saber: no se dibuja. */
    data object Hidden : CashChipState
    data class Open(val sinceIso: String?) : CashChipState
    data object Closed : CashChipState

    /** El último intento de leer la caja falló: NO es lo mismo que cerrada. */
    data object Unknown : CashChipState
}

/**
 * Decisor del chip. Orden: sin permiso -> oculto; abierta confirmada -> abierta; la lectura falló ->
 * "Caja…" (no se afirma cerrada); lectura confirmada sin sesión -> cerrada; aún sin leer -> oculto.
 */
fun decideCashChip(
    canOperateCash: Boolean,
    hasOpenSession: Boolean,
    openedAt: String?,
    checked: Boolean,
    checkFailed: Boolean,
): CashChipState = when {
    !canOperateCash -> CashChipState.Hidden
    hasOpenSession -> CashChipState.Open(openedAt)
    checkFailed -> CashChipState.Unknown
    checked -> CashChipState.Closed
    else -> CashChipState.Hidden
}

/** La franja "Caja cerrada. [Abrir caja]" solo se muestra con la caja CONFIRMADA cerrada. */
fun shouldShowClosedBanner(state: CashChipState): Boolean = state == CashChipState.Closed

/**
 * Regla única de caja en el cobro (igual que Tauri y que el backend `ResolveCashSessionForPayments`):
 * la caja abierta se exige SOLO si algún pago es en efectivo. Yape/tarjeta/transferencia no la exigen.
 */
fun cashSessionRequiredForPayments(anyCashPayment: Boolean): Boolean = anyCashPayment

/** Resultado de cierre tal como lo cuenta el servidor. */
sealed interface CashCloseOutcome {
    data object NotClosed : CashCloseOutcome

    /** Cerrada sin conteo: no hay "contado" ni "diferencia"; nunca se muestra como "cuadró". */
    data object NotCounted : CashCloseOutcome
    data class Counted(val counted: Double, val difference: Double) : CashCloseOutcome
}

/**
 * [counted] es el flag del servidor (null en servidores/eventos viejos). Un cierre cuenta si el servidor
 * lo dice o, sin flag, si trae un contado. Una diferencia null NUNCA se toma como 0: si el servidor no la
 * mandó pero hay contado, se calcula contra el esperado; sin contado es [CashCloseOutcome.NotCounted].
 */
fun resolveCloseOutcome(
    closed: Boolean,
    counted: Boolean?,
    closingBalance: Double?,
    difference: Double?,
    expected: Double?,
): CashCloseOutcome {
    if (!closed) return CashCloseOutcome.NotClosed
    if (counted == false || closingBalance == null) return CashCloseOutcome.NotCounted
    val diff = difference ?: expected?.let { closingBalance - it }
        ?: return CashCloseOutcome.NotCounted
    return CashCloseOutcome.Counted(closingBalance, diff)
}

/** Efectivo contado del último cierre CON conteo (para prellenar la apertura); null si no hay. */
fun lastCountedCash(sessions: List<CashSessionBrief>): Double? =
    sessions
        .filter { it.status == CashSessionStatus.CLOSED && it.closingBalance != null }
        .maxByOrNull { it.closedAt ?: it.openedAt ?: "" }
        ?.closingBalance

/** "2026-10-04T10:02:00-05:00" -> "10:02" (hora de Lima). Si no se puede leer, null: mejor omitirlo que inventarlo. */
fun formatCashSince(iso: String?): String? = com.bendey.restaurant.core.domain.time.PeruDateTime.formatTime(iso)
