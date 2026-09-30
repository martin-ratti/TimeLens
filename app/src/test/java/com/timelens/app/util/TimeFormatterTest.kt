package com.timelens.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatMillisToReadable formats hours and minutes`() {
        // 2 hours, 15 minutes, 30 seconds
        val millis = (2 * 3600 + 15 * 60 + 30) * 1000L
        assertEquals("2h 15min", TimeFormatter.formatMillisToReadable(millis))
    }

    @Test
    fun `formatMillisToReadable formats minutes and seconds`() {
        // 5 minutes, 42 seconds
        val millis = (5 * 60 + 42) * 1000L
        assertEquals("5min 42s", TimeFormatter.formatMillisToReadable(millis))
    }

    @Test
    fun `formatMillisToReadable formats seconds only`() {
        val millis = 25 * 1000L
        assertEquals("25s", TimeFormatter.formatMillisToReadable(millis))
    }

    @Test
    fun `formatMillisToShort formats hours and minutes`() {
        val millis = (3 * 3600 + 45 * 60) * 1000L
        assertEquals("3h 45m", TimeFormatter.formatMillisToShort(millis))
    }

    @Test
    fun `formatMillisToShort formats minutes only when less than 1 hour`() {
        val millis = 45 * 60 * 1000L
        assertEquals("45m", TimeFormatter.formatMillisToShort(millis))
    }

    @Test
    fun `formatMillisToShort formats zero correctly`() {
        assertEquals("0m", TimeFormatter.formatMillisToShort(0L))
    }

    @Test
    fun `formatPercentageChange returns without previous data when previous is 0`() {
        assertEquals("Sin datos previos", TimeFormatter.formatPercentageChange(1000L, 0L))
    }

    @Test
    fun `formatPercentageChange returns positive change`() {
        assertEquals("+50% vs ayer", TimeFormatter.formatPercentageChange(1500L, 1000L))
    }

    @Test
    fun `formatPercentageChange returns negative change`() {
        assertEquals("-25% vs ayer", TimeFormatter.formatPercentageChange(750L, 1000L))
    }

    @Test
    fun `formatPercentageChange returns equal when change is zero`() {
        assertEquals("Igual que ayer", TimeFormatter.formatPercentageChange(1000L, 1000L))
    }

    @Test
    fun `getTimeOfDayLabel returns correct labels for all 24 hours`() {
        assertEquals("Madrugada", TimeFormatter.getTimeOfDayLabel(2))
        assertEquals("Madrugada", TimeFormatter.getTimeOfDayLabel(5))
        assertEquals("Mañana", TimeFormatter.getTimeOfDayLabel(6))
        assertEquals("Mañana", TimeFormatter.getTimeOfDayLabel(11))
        assertEquals("Tarde", TimeFormatter.getTimeOfDayLabel(12))
        assertEquals("Tarde", TimeFormatter.getTimeOfDayLabel(17))
        assertEquals("Noche", TimeFormatter.getTimeOfDayLabel(18))
        assertEquals("Noche", TimeFormatter.getTimeOfDayLabel(22))
        assertEquals("Madrugada", TimeFormatter.getTimeOfDayLabel(23))
        assertEquals("Madrugada", TimeFormatter.getTimeOfDayLabel(0))
    }
}
