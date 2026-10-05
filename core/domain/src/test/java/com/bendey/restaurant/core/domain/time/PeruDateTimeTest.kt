package com.bendey.restaurant.core.domain.time

import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PeruDateTimeTest {
    private lateinit var tz: TimeZone
    private lateinit var locale: Locale

    @Before fun foreignDevice() {
        // Un dispositivo en Tokio y en inglés no debe cambiar lo que se ve.
        tz = TimeZone.getDefault(); locale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo")); Locale.setDefault(Locale.US)
    }

    @After fun restore() { TimeZone.setDefault(tz); Locale.setDefault(locale) }

    @Test fun utcInstantShowsLimaWallClock() {
        assertEquals("09/09/2026", PeruDateTime.formatDate("2026-09-09T13:15:00Z"))
        assertEquals("08:15", PeruDateTime.formatTime("2026-09-09T13:15:00Z"))
        assertEquals("09/09/2026 08:15", PeruDateTime.formatDateTime("2026-09-09T13:15:00Z"))
    }

    @Test fun lateUtcCrossesToPreviousDayInLima() {
        assertEquals("08/09/2026 22:30", PeruDateTime.formatDateTime("2026-09-09T03:30:00Z"))
    }

    @Test fun offsetIsConvertedNotTruncated() {
        assertEquals("09/09/2026 08:15", PeruDateTime.formatDateTime("2026-09-09T08:15:00-05:00"))
        assertEquals("09/09/2026 08:15", PeruDateTime.formatDateTime("2026-09-09T15:15:00+02:00"))
    }

    @Test fun zonelessValueIsLimaTime() {
        assertEquals("09/09/2026 08:15", PeruDateTime.formatDateTime("2026-09-09 08:15:00"))
        assertEquals("08:15", PeruDateTime.formatTime("2026-09-09T08:15:00"))
    }

    @Test fun dayOnlyHasNoTime() {
        assertEquals("09/09/2026", PeruDateTime.formatDate("2026-09-09"))
        assertEquals("09/09/2026", PeruDateTime.formatDateTime("2026-09-09"))
        assertNull(PeruDateTime.formatTime("2026-09-09"))
    }

    @Test fun unreadableFallsBackToRawOrNull() {
        assertNull(PeruDateTime.formatDate("ayer"))
        assertNull(PeruDateTime.formatDate(null))
        assertEquals("ayer", PeruDateTime.formatDateOrRaw("ayer"))
        assertEquals("", PeruDateTime.formatDateTimeOrRaw(null))
    }

    @Test fun epochMillisAndDayMonth() {
        // 2026-09-09T13:15:00Z
        assertEquals("08:15", PeruDateTime.formatTime(1_788_959_700_000L))
        assertEquals("09/09", PeruDateTime.formatDayMonth(LocalDate.of(2026, 9, 9)))
    }
}
