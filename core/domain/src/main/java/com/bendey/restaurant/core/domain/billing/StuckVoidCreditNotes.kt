package com.bendey.restaurant.core.domain.billing

/**
 * Anulaciones que quedaron a medias (R10.9, SOLO LECTURA). Equivale a `StuckVoidCreditNotesPanel` de Tauri
 * (`GET /api/billing/stuck-void-credit-notes`): se emitió una nota de crédito para anular una venta, SUNAT
 * no la aceptó (o lleva más de una hora sin responder) y la venta original sigue vigente, así que todavía
 * cuenta en los reportes. "Necesita atención" = lo que el backend devuelve en esa lista; Android no lo
 * recalcula. La recuperación (reenviar o emitir una nota nueva) se hace desde Bendey Resto en la PC.
 */
data class StuckVoidCreditNote(
    val originalSaleId: Int,
    val originalNumber: String,
    val originalDocType: String,
    val originalTotal: Double,
    val creditNoteId: Int,
    val creditNoteNumber: String,
    val sunatMessage: String,
    /** true = no está rechazada, solo lleva demasiado tiempo sin resolverse. */
    val stalled: Boolean,
    val createdAt: String?,
)

object StuckVoidCopy {
    fun title(count: Int): String =
        if (count == 1) "1 comprobante necesita atención" else "$count comprobantes necesitan atención"

    fun description(count: Int): String =
        "SUNAT no aceptó la nota de crédito, así que " +
            (if (count == 1) "esta venta sigue vigente" else "estas ventas siguen vigentes") +
            " y aún cuentan en tus reportes."

    const val RESOLVE_HINT = "Para reenviarlas o emitir una nota nueva, entra a Ventas desde Bendey Resto en la PC."
    const val SHOW = "Ver detalle"
    const val HIDE = "Ocultar detalle"
    const val LOAD_ERROR = "No pudimos revisar si hay comprobantes pendientes de atención."
    const val LOAD_ERROR_RETRY = "Reintentar"

    fun reason(row: StuckVoidCreditNote): String =
        if (row.stalled) "sin respuesta de SUNAT" else "rechazada por SUNAT"

    /** "2026-09-30T10:15:00-05:00" -> "30/09/2026"; vacío si no hay fecha. */
    fun date(createdAt: String?): String {
        val d = createdAt?.take(10).orEmpty()
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").matchEntire(d) ?: return ""
        return "${m.groupValues[3]}/${m.groupValues[2]}/${m.groupValues[1]}"
    }

    fun line1(row: StuckVoidCreditNote, formattedTotal: String): String =
        "${row.originalDocType} ${row.originalNumber} · $formattedTotal"

    fun line2(row: StuckVoidCreditNote, date: String): String {
        val nc = row.creditNoteNumber.ifBlank { "#${row.creditNoteId}" }
        return listOf("NC $nc", reason(row), date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}

/** Estado del aviso: nunca ocupa pantalla si no hay nada que decir ni nada que falló. */
sealed interface StuckVoidBannerState {
    /** No se muestra nada (sin SUNAT, sin permiso, cargando por primera vez o sin pendientes). */
    data object Hidden : StuckVoidBannerState
    data class Pending(val rows: List<StuckVoidCreditNote>) : StuckVoidBannerState
    /** Falló la consulta: se avisa con Reintentar, no se finge que no hay pendientes. */
    data class Failed(val message: String) : StuckVoidBannerState
}

fun stuckVoidBannerState(
    sunatEnabled: Boolean,
    rows: List<StuckVoidCreditNote>?,
    error: String?,
): StuckVoidBannerState = when {
    !sunatEnabled -> StuckVoidBannerState.Hidden
    !error.isNullOrBlank() -> StuckVoidBannerState.Failed(StuckVoidCopy.LOAD_ERROR)
    rows.isNullOrEmpty() -> StuckVoidBannerState.Hidden
    else -> StuckVoidBannerState.Pending(rows)
}
