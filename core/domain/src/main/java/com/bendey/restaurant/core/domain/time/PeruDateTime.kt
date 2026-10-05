package com.bendey.restaurant.core.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Único punto de formateo de fecha/hora VISIBLE (R11 — DESIGN-SYSTEM §12). Idioma es-PE, zona
 * America/Lima y formatos `dd/MM/yyyy` y `HH:mm`, sin depender de la zona ni del idioma del
 * dispositivo (un POS con el reloj en otra zona no debe mostrar otra hora de venta).
 *
 * Regla: ninguna pantalla usa `DateTimeFormatter.ofPattern` / `SimpleDateFormat` para mostrar
 * fechas; todas pasan por aquí (lo vigila `DateFormatGuardTest`). Las fechas ISO que viajan al
 * backend (`yyyy-MM-dd`) NO son formato visible y siguen con `ISO_LOCAL_DATE`.
 *
 * Los `format*` devuelven null si el valor no se puede leer; los `*OrRaw` devuelven el dato crudo
 * (mejor mostrar el dato que ocultarlo).
 */
object PeruDateTime {
    val ZONE: ZoneId = ZoneId.of("America/Lima")
    val LOCALE: Locale = Locale("es", "PE")

    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE)
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE)
    private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", LOCALE)

    private sealed interface Parsed {
        data class Moment(val at: java.time.ZonedDateTime) : Parsed
        data class DayOnly(val day: LocalDate) : Parsed
    }

    /** Acepta ISO con zona ("...Z", "...-05:00"), sin zona ("2026-09-09 08:15:00" = hora de Lima) y solo día. */
    private fun parse(raw: String?): Parsed? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        runCatching { return Parsed.Moment(OffsetDateTime.parse(value).atZoneSameInstant(ZONE)) }
        runCatching { return Parsed.Moment(Instant.parse(value).atZone(ZONE)) }
        val local = value.replace(' ', 'T')
        runCatching { return Parsed.Moment(LocalDateTime.parse(local).atZone(ZONE)) }
        runCatching { return Parsed.DayOnly(LocalDate.parse(value.take(10))) }
        return null
    }

    /** "2026-09-09T13:15:00Z" -> "09/09/2026" (día en Lima). */
    fun formatDate(raw: String?): String? = when (val p = parse(raw)) {
        is Parsed.Moment -> p.at.format(DATE)
        is Parsed.DayOnly -> p.day.format(DATE)
        null -> null
    }

    /** "2026-09-09T13:15:00Z" -> "08:15". Un valor de solo día no tiene hora: devuelve null. */
    fun formatTime(raw: String?): String? = when (val p = parse(raw)) {
        is Parsed.Moment -> p.at.format(TIME)
        else -> null
    }

    /** "2026-09-09T13:15:00Z" -> "09/09/2026 08:15"; si solo hay día, "09/09/2026". */
    fun formatDateTime(raw: String?): String? = when (val p = parse(raw)) {
        is Parsed.Moment -> p.at.format(DATE_TIME)
        is Parsed.DayOnly -> p.day.format(DATE)
        null -> null
    }

    /** Etiqueta corta de eje de gráfico: "09/09". */
    fun formatDayMonth(day: LocalDate): String = day.format(DAY_MONTH)

    fun formatDateOrRaw(raw: String?): String = formatDate(raw) ?: raw.orEmpty()
    fun formatTimeOrRaw(raw: String?): String = formatTime(raw) ?: raw.orEmpty()
    fun formatDateTimeOrRaw(raw: String?): String = formatDateTime(raw) ?: raw.orEmpty()

    /** Hora (HH:mm) de un instante en epoch-millis, en Lima. */
    fun formatTime(epochMs: Long): String = Instant.ofEpochMilli(epochMs).atZone(ZONE).format(TIME)
}
