package com.bendey.restaurant.core.domain.ops

/** Un tramo de atención. `weekday` 1 = lunes … 7 = domingo; horas `HH:MM` (hora de Lima). */
data class BusinessInterval(val weekday: Int, val openAt: String, val closeAt: String)

data class BusinessHoursStatus(val configured: Boolean = false, val open: Boolean = true, val message: String = "") {
    /** Texto para la tarjeta de Ajustes. */
    val summary: String
        get() = when {
            !configured -> BusinessHoursRules.ALWAYS_OPEN_NOTICE
            open -> "Abierto ahora"
            else -> message.ifBlank { "Cerrado por ahora" }
        }
}

data class BusinessHours(val intervals: List<BusinessInterval> = emptyList(), val status: BusinessHoursStatus = BusinessHoursStatus())

/** Un tramo en edición (texto `HH:MM` o vacío si todavía no se eligió). */
data class SlotDraft(val openAt: String = "", val closeAt: String = "")

/** Un día en edición: [open] es el interruptor «Abierto». */
data class DayDraft(val open: Boolean = false, val slots: List<SlotDraft> = emptyList())

/** Lunes (1) a domingo (7). */
typealias WeekDraft = Map<Int, DayDraft>

object BusinessHoursRules {
    const val MAX_PER_DAY = 4
    const val MAX_TOTAL = 7 * MAX_PER_DAY
    const val DEFAULT_OPEN = "11:00"
    const val DEFAULT_CLOSE = "22:00"
    const val ALWAYS_OPEN_NOTICE = "Sin horario guardado el restaurante recibe pedidos todo el tiempo"
    const val MIDNIGHT_NOTE = "Si el cierre es menor o igual a la apertura, el tramo cruza la medianoche y termina al día siguiente."

    val DAY_NAMES = mapOf(
        1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves", 5 to "Viernes", 6 to "Sábado", 7 to "Domingo",
    )

    /** `HH:MM` -> minutos desde la medianoche (0..1439), o null si no es válido (mismo formato estricto del backend). */
    fun parseHm(text: String): Int? {
        val s = text.trim()
        if (s.length != 5 || s[2] != ':') return null
        if (listOf(0, 1, 3, 4).any { s[it] !in '0'..'9' }) return null
        val h = s.substring(0, 2).toInt()
        val m = s.substring(3, 5).toInt()
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun formatHm(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    /** Un cierre menor a la apertura cruza la medianoche (la igualdad se rechaza antes). */
    fun crossesMidnight(slot: SlotDraft): Boolean {
        val o = parseHm(slot.openAt) ?: return false
        val c = parseHm(slot.closeAt) ?: return false
        return c < o
    }

    /** Mensaje de error de UN tramo, o null si es válido. */
    fun validateSlot(slot: SlotDraft): String? {
        val o = parseHm(slot.openAt)
        val c = parseHm(slot.closeAt)
        return when {
            o == null || c == null -> "Elige la hora de apertura y la de cierre."
            o == c -> "La apertura y el cierre no pueden ser la misma hora."
            else -> null
        }
    }

    /** Explicación bajo un tramo que cruza la medianoche, o null. */
    fun midnightExplanation(slot: SlotDraft): String? =
        if (crossesMidnight(slot)) "Termina al día siguiente a las ${slot.closeAt}." else null

    /** Primer error de la semana (con el nombre del día) o null si se puede guardar. */
    fun validateWeek(week: WeekDraft): String? {
        var total = 0
        for (day in 1..7) {
            val d = week[day] ?: continue
            if (!d.open) continue
            if (d.slots.isEmpty()) return "${DAY_NAMES[day]}: agrega al menos un tramo o apaga el día."
            if (d.slots.size > MAX_PER_DAY) return "${DAY_NAMES[day]}: máximo $MAX_PER_DAY tramos."
            d.slots.forEach { s -> validateSlot(s)?.let { return "${DAY_NAMES[day]}: $it" } }
            total += d.slots.size
        }
        if (total > MAX_TOTAL) return "Demasiados tramos en total."
        return null
    }

    /** Semana en edición -> tramos para el PUT (solo días abiertos). Asume [validateWeek] == null. */
    fun toIntervals(week: WeekDraft): List<BusinessInterval> = buildList {
        for (day in 1..7) {
            val d = week[day] ?: continue
            if (!d.open) continue
            d.slots.forEach { add(BusinessInterval(day, it.openAt.trim(), it.closeAt.trim())) }
        }
    }

    /** Tramos guardados -> semana en edición (los días sin tramos quedan cerrados). */
    fun fromIntervals(intervals: List<BusinessInterval>): WeekDraft =
        (1..7).associateWith { day ->
            val slots = intervals.filter { it.weekday == day }.sortedBy { it.openAt }.map { SlotDraft(it.openAt, it.closeAt) }
            DayDraft(open = slots.isNotEmpty(), slots = slots)
        }

    /** Copia los tramos de [fromDay] a los 7 días. */
    fun copyToAll(week: WeekDraft, fromDay: Int): WeekDraft {
        val src = week[fromDay] ?: return week
        return (1..7).associateWith { src }
    }

    /** Enciende un día con un tramo por defecto para no dejarlo vacío (inválido). */
    fun setDayOpen(week: WeekDraft, day: Int, open: Boolean): WeekDraft {
        val cur = week[day] ?: DayDraft()
        val next = if (open) {
            cur.copy(open = true, slots = cur.slots.ifEmpty { listOf(SlotDraft(DEFAULT_OPEN, DEFAULT_CLOSE)) })
        } else {
            cur.copy(open = false)
        }
        return week + (day to next)
    }

    fun addSlot(week: WeekDraft, day: Int): WeekDraft {
        val cur = week[day] ?: DayDraft(open = true)
        if (cur.slots.size >= MAX_PER_DAY) return week
        return week + (day to cur.copy(open = true, slots = cur.slots + SlotDraft(DEFAULT_OPEN, DEFAULT_CLOSE)))
    }

    fun removeSlot(week: WeekDraft, day: Int, index: Int): WeekDraft {
        val cur = week[day] ?: return week
        val slots = cur.slots.filterIndexed { i, _ -> i != index }
        return week + (day to cur.copy(slots = slots, open = slots.isNotEmpty() && cur.open))
    }

    fun updateSlot(week: WeekDraft, day: Int, index: Int, slot: SlotDraft): WeekDraft {
        val cur = week[day] ?: return week
        if (index !in cur.slots.indices) return week
        return week + (day to cur.copy(slots = cur.slots.mapIndexed { i, s -> if (i == index) slot else s }))
    }

    /** Hay cambios respecto de lo guardado (compara los tramos resultantes, no el borrador crudo). */
    fun isDirty(week: WeekDraft, saved: List<BusinessInterval>): Boolean {
        val draft = toIntervals(week).sortedWith(compareBy({ it.weekday }, { it.openAt }))
        val base = saved.sortedWith(compareBy({ it.weekday }, { it.openAt }))
        return draft != base
    }
}
