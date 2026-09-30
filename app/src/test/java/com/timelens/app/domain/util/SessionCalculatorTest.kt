package com.timelens.app.domain.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SessionCalculatorTest {

    @Test
    fun `calculateMetrics handles exact start boundary correctly`() {
        val startTime = 1000L
        val metrics = SessionCalculator.calculateMetrics(emptyList(), startTime)

        assertEquals(0, metrics.totalUnlocks)
        assertEquals(0L, metrics.longestSessionMs)
        assertEquals(null, metrics.longestSessionAppPackage)
        assertEquals(0, metrics.peakHour)
        assertEquals(0, metrics.totalSessions)
        assertEquals(emptyMap<String, Long>(), metrics.appUsageMap)
    }

    @Test
    fun `calculateMetrics handles session crossing midnight before startTimeMs`() {
        val startTimeMs = 100_000L
        // Session started before startTimeMs (at 80_000) and ended at 120_000
        val events = listOf(
            UsageEventModel(
                packageName = "com.whatsapp",
                eventType = 1, // Resumed
                timeStamp = 80_000L
            ),
            UsageEventModel(
                packageName = "com.whatsapp",
                eventType = 2, // Paused
                timeStamp = 120_000L
            )
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = startTimeMs,
            evalCurrentTime = 130_000L
        )

        // Only the duration from startTimeMs to 120_000 should be counted: 20_000ms
        assertEquals(20_000L, metrics.appUsageMap["com.whatsapp"])
        assertEquals(20_000L, metrics.longestSessionMs)
        assertEquals("com.whatsapp", metrics.longestSessionAppPackage)
        assertEquals(1, metrics.totalSessions)
    }

    @Test
    fun `calculateMetrics closes session properly on device shutdown`() {
        val startTimeMs = 10_000L
        val events = listOf(
            UsageEventModel(
                packageName = "com.google.android.youtube",
                eventType = 1, // Resumed
                timeStamp = 20_000L
            ),
            UsageEventModel(
                packageName = null,
                eventType = 26, // DEVICE_SHUTDOWN at 50_000L
                timeStamp = 50_000L
            ),
            UsageEventModel(
                packageName = "com.instagram.android",
                eventType = 1, // Reopened hours later
                timeStamp = 100_000L
            ),
            UsageEventModel(
                packageName = "com.instagram.android",
                eventType = 16, // Screen off at 110_000L
                timeStamp = 110_000L
            )
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = startTimeMs,
            evalCurrentTime = 120_000L
        )

        // YouTube should be exactly 50_000 - 20_000 = 30_000ms, not continuing until 100_000
        assertEquals(30_000L, metrics.appUsageMap["com.google.android.youtube"])
        assertEquals(10_000L, metrics.appUsageMap["com.instagram.android"])
        assertEquals(2, metrics.totalSessions)
    }

    @Test
    fun `calculateMetrics merges brief pauses within the same app`() {
        val startTimeMs = 10_000L
        val events = listOf(
            UsageEventModel(
                packageName = "com.twitter.android",
                eventType = 1, // Resumed at 20_000
                timeStamp = 20_000L
            ),
            UsageEventModel(
                packageName = "com.twitter.android",
                eventType = 2, // Paused at 30_000
                timeStamp = 30_000L
            ),
            UsageEventModel(
                packageName = "com.twitter.android",
                eventType = 1, // Resumed at 32_000 (canceled pause)
                timeStamp = 32_000L
            ),
            UsageEventModel(
                packageName = "com.twitter.android",
                eventType = 16, // Screen off at 60_000
                timeStamp = 60_000L
            )
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = startTimeMs,
            evalCurrentTime = 70_000L
        )

        // Single continuous session of 40_000ms (60_000 - 20_000)
        assertEquals(40_000L, metrics.appUsageMap["com.twitter.android"])
        assertEquals(40_000L, metrics.longestSessionMs)
        assertEquals(1, metrics.totalSessions)
    }

    @Test
    fun `calculateMetrics handles pause followed by screen off`() {
        val startTimeMs = 10_000L
        val events = listOf(
            UsageEventModel(
                packageName = "com.android.chrome",
                eventType = 1, // Resumed at 20_000
                timeStamp = 20_000L
            ),
            UsageEventModel(
                packageName = "com.android.chrome",
                eventType = 2, // Paused at 35_000
                timeStamp = 35_000L
            ),
            UsageEventModel(
                packageName = null,
                eventType = 16, // Screen off at 45_000
                timeStamp = 45_000L
            )
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = startTimeMs,
            evalCurrentTime = 50_000L
        )

        // Session ended when paused: 35_000 - 20_000 = 15_000ms
        assertEquals(15_000L, metrics.appUsageMap["com.android.chrome"])
        assertEquals(15_000L, metrics.longestSessionMs)
    }

    @Test
    fun `calculateMetrics ignores anomalous sessions exceeding 12 hours`() {
        val startTimeMs = 10_000L
        val thirteenHoursMs = 13 * 3600 * 1000L
        val events = listOf(
            UsageEventModel(
                packageName = "com.buggy.app",
                eventType = 1,
                timeStamp = 20_000L
            ),
            UsageEventModel(
                packageName = "com.buggy.app",
                eventType = 16,
                timeStamp = 20_000L + thirteenHoursMs
            )
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = startTimeMs,
            evalCurrentTime = 20_000L + thirteenHoursMs + 1000L
        )

        // Corrupted session discarded
        assertEquals(null, metrics.appUsageMap["com.buggy.app"])
        assertEquals(0L, metrics.longestSessionMs)
        assertEquals(0, metrics.totalSessions)
    }

    @Test
    fun `calculateMetrics computes peakHour and productiveHour accurately`() {
        // Setup Calendar timestamps for specific hours
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14) // 14:00
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t14 = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 10) // 10:00
        val t10 = cal.timeInMillis

        val events = listOf(
            // 45 min at 14:00
            UsageEventModel("com.app.peak", 1, t14),
            UsageEventModel("com.app.peak", 2, t14 + (45 * 60 * 1000L)),
            // 5 min at 10:00
            UsageEventModel("com.app.focus", 1, t10),
            UsageEventModel("com.app.focus", 2, t10 + (5 * 60 * 1000L))
        )

        val metrics = SessionCalculator.calculateMetricsFromModels(
            eventsList = events,
            startTimeMs = t10 - 3600000L,
            evalCurrentTime = t14 + 3600000L
        )

        assertEquals(14, metrics.peakHour)
        // Productive hour in waking window with lowest usage (e.g. 8, 9, 11, etc. which had 0 ms)
        assertTrue(metrics.productiveHour in 8..21)
        assertNotEquals(14, metrics.productiveHour)
    }

    @Test
    fun `calculateAppHourlyUsage distributes time correctly for target package`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t11 = cal.timeInMillis

        val events = listOf(
            UsageEventModel("com.target.app", 1, t11),
            UsageEventModel("com.target.app", 2, t11 + (25 * 60 * 1000L)),
            UsageEventModel("com.other.app", 1, t11 + (30 * 60 * 1000L)),
            UsageEventModel("com.other.app", 2, t11 + (40 * 60 * 1000L))
        )

        val hourly = SessionCalculator.calculateAppHourlyUsageFromModels(
            eventsList = events,
            targetPackage = "com.target.app",
            evalCurrentTime = t11 + 3600000L
        )

        assertEquals(25 * 60 * 1000L, hourly[11])
        assertEquals(0L, hourly[12])
        assertEquals(0L, hourly[10])
    }
}
