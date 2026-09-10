package com.bendey.restaurant.feature.caja

import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Filtra y busca en el historial de cierres YA CARGADO — `listSessions` no acepta rango de
 * fechas (ver [com.bendey.restaurant.core.domain.cash.CashRepository.listSessions] y el
 * handler `ListSessionsAPI` en backend_go), así que "Hoy/7 días/30 días" y el buscador son un
 * filtro en el cliente, no una nueva petición. Sigue siendo mejor que el selector de 10 chips
 * que reemplaza: acá no hay tope.
 */
internal object CajaHistoryFilters {

    fun apply(
        sessions: List<CashSessionBrief>,
        query: String,
        dateFilter: HistoryDateFilter,
    ): List<CashSessionBrief> {
        val byDate = if (dateFilter.days == null) {
            sessions
        } else {
            val cutoff = LocalDate.now().minusDays(dateFilter.days)
            sessions.filter { session ->
                val openedDate = session.openedAt?.let(::parseOrNull)?.toLocalDate() ?: return@filter true
                !openedDate.isBefore(cutoff)
            }
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return byDate
        return byDate.filter { session ->
            session.id.toString().contains(trimmed, ignoreCase = true) ||
                session.openedByName.orEmpty().contains(trimmed, ignoreCase = true)
        }
    }

    private fun parseOrNull(value: String): OffsetDateTime? =
        runCatching { OffsetDateTime.parse(value) }.getOrNull()
}

private val momentFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es", "PE"))

/**
 * "2026-09-09T08:15:00-05:00" → "9 sep, 08:15". Si el valor no es parseable se devuelve tal
 * cual — mejor mostrar el dato crudo que ocultarlo.
 */
internal fun formatSessionMoment(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(raw).format(momentFormatter) }.getOrDefault(raw)
}
