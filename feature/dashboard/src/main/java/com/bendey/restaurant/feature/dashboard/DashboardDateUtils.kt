package com.bendey.restaurant.feature.dashboard

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PERU_ZONE: ZoneId = ZoneId.of("America/Lima")
private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayInPeru(): LocalDate = LocalDate.now(PERU_ZONE)

fun DashboardRange.resolveDatesPeru(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    DashboardRange.TODAY -> today to today
    DashboardRange.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
    DashboardRange.WEEK -> today.minusDays(6) to today
    DashboardRange.MONTH -> today.minusDays(29) to today
    DashboardRange.CUSTOM -> today to today
}

fun LocalDate.toApiDate(): String = format(ISO_DATE)

fun parseApiDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim(), ISO_DATE) }.getOrNull()

fun isDashboardDateAdmin(employeeType: String?): Boolean {
    val et = employeeType?.lowercase().orEmpty()
    return et == "admin" || et == "supervisor"
}
