package com.timelens.app.worker

import com.timelens.app.fakes.FakeSharedPreferences
import com.timelens.app.presentation.MainActivity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WorkersAndNavigationValidationTest {

    @Test
    fun `DailySummaryWorker accepts expanded evening and post-midnight Doze window`() {
        assertTrue(DailySummaryWorker.isWithinNightSummaryWindow(20))
        assertTrue(DailySummaryWorker.isWithinNightSummaryWindow(22))
        assertTrue(DailySummaryWorker.isWithinNightSummaryWindow(23))
        assertTrue(DailySummaryWorker.isWithinNightSummaryWindow(0))
        assertTrue(DailySummaryWorker.isWithinNightSummaryWindow(1))

        assertFalse(DailySummaryWorker.isWithinNightSummaryWindow(2))
        assertFalse(DailySummaryWorker.isWithinNightSummaryWindow(9))
        assertFalse(DailySummaryWorker.isWithinNightSummaryWindow(19))
    }

    @Test
    fun `DailySummaryWorker resolves yesterday when Doze delays run slightly past midnight`() {
        val today = LocalDate.of(2026, 10, 15)

        assertEquals(
            today.minusDays(1),
            DailySummaryWorker.resolveTargetSummaryDate(today, currentHour = 0, isManualTest = false)
        )
        assertEquals(
            today.minusDays(1),
            DailySummaryWorker.resolveTargetSummaryDate(today, currentHour = 1, isManualTest = false)
        )
        assertEquals(
            today,
            DailySummaryWorker.resolveTargetSummaryDate(today, currentHour = 22, isManualTest = false)
        )
        assertEquals(
            today,
            DailySummaryWorker.resolveTargetSummaryDate(today, currentHour = 0, isManualTest = true)
        )
    }

    @Test
    fun `NotificationWorkScheduler calculates delay to 22_00 today or tomorrow accurately`() {
        val before22 = LocalDateTime.of(2026, 10, 15, 20, 0, 0)
        val delayBefore = NotificationWorkScheduler.calculateNext2200DelayMillis(before22, forceNextDay = false)
        assertEquals(2 * 3_600_000L, delayBefore)

        val after22 = LocalDateTime.of(2026, 10, 15, 22, 30, 0)
        val delayAfter = NotificationWorkScheduler.calculateNext2200DelayMillis(after22, forceNextDay = false)
        assertEquals(23 * 3_600_000L + 30 * 60_000L, delayAfter)

        val forcedNext = NotificationWorkScheduler.calculateNext2200DelayMillis(before22, forceNextDay = true)
        assertEquals(26 * 3_600_000L, forcedNext)
    }

    @Test
    fun `UsageAlertWorker prunes alert keys older than 7 days and keeps recent ones`() {
        val prefs = FakeSharedPreferences()
        val today = LocalDate.of(2026, 10, 15)
        prefs.edit()
            .putInt("app_limit_com.instagram.android_2026-10-01", 100)
            .putInt("app_limit_com.whatsapp_2026-10-07", 80)
            .putInt("app_limit_com.youtube_2026-10-12", 100)
            .putString("last_goal_alert_date", "2026-10-15")
            .apply()

        UsageAlertWorker.pruneOldAlertKeys(prefs, today)

        assertFalse(prefs.contains("app_limit_com.instagram.android_2026-10-01"))
        assertFalse(prefs.contains("app_limit_com.whatsapp_2026-10-07"))
        assertTrue(prefs.contains("app_limit_com.youtube_2026-10-12"))
        assertEquals("2026-10-15", prefs.getString("last_goal_alert_date", null))
    }

    @Test
    fun `MainActivity sanitizeNavRoute allows valid routes and rejects malicious routes`() {
        assertEquals("home", MainActivity.sanitizeNavRoute("home"))
        assertEquals("daily_summary_night", MainActivity.sanitizeNavRoute("daily_summary_night"))
        assertEquals("app_detail/com.instagram.android", MainActivity.sanitizeNavRoute("app_detail/com.instagram.android"))

        assertNull(MainActivity.sanitizeNavRoute(null))
        assertNull(MainActivity.sanitizeNavRoute(""))
        assertNull(MainActivity.sanitizeNavRoute("https://evil.example.com"))
        assertNull(MainActivity.sanitizeNavRoute("app_detail/../../secret"))
        assertNull(MainActivity.sanitizeNavRoute("unknown_route"))
    }
}
