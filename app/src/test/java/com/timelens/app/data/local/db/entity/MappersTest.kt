package com.timelens.app.data.local.db.entity

import com.timelens.app.domain.model.AppCategory
import com.timelens.app.domain.model.AppUsageInfo
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.Session
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MappersTest {

    @Test
    fun `DailyUsageEntity toDomain maps all fields with longest session present`() {
        val entity = DailyUsageEntity(
            date = "2026-09-30",
            totalScreenTimeMs = 7200000L,
            totalUnlocks = 42,
            longestSessionMs = 1800000L,
            longestSessionApp = "com.whatsapp",
            topAppPackage = "com.whatsapp",
            topAppTimeMs = 3600000L,
            totalSessions = 25,
            peakHour = 19
        )

        val topApps = listOf(
            AppUsageInfo(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                totalTimeMs = 3600000L,
                sessionCount = 10,
                longestSessionMs = 1800000L,
                category = AppCategory.COMMUNICATION
            )
        )

        val domain = entity.toDomain(topApps, "WhatsApp")

        assertEquals(LocalDate.of(2026, 9, 30), domain.date)
        assertEquals(7200000L, domain.totalScreenTimeMs)
        assertEquals(42, domain.totalUnlocks)
        assertEquals(1, domain.topApps.size)
        assertNotNull(domain.longestSession)
        assertEquals("com.whatsapp", domain.longestSession?.packageName)
        assertEquals("WhatsApp", domain.longestSession?.appName)
        assertEquals(1800000L, domain.longestSession?.durationMs)
        assertEquals(19, domain.peakHour)
        assertEquals(25, domain.totalSessions)
    }

    @Test
    fun `DailyUsageEntity toDomain maps null longestSession when longestSessionApp is empty`() {
        val entity = DailyUsageEntity(
            date = "2026-09-30",
            totalScreenTimeMs = 0L,
            totalUnlocks = 0,
            longestSessionMs = 0L,
            longestSessionApp = "",
            topAppPackage = "",
            topAppTimeMs = 0L,
            totalSessions = 0,
            peakHour = 0
        )

        val domain = entity.toDomain(emptyList(), "")

        assertNull(domain.longestSession)
        assertEquals(0, domain.topApps.size)
    }

    @Test
    fun `AppDailyUsageEntity toDomain maps category and fields`() {
        val entity = AppDailyUsageEntity(
            date = "2026-09-30",
            packageName = "com.instagram.android",
            appName = "Instagram",
            totalTimeMs = 2400000L,
            sessionCount = 15,
            longestSessionMs = 800000L,
            category = "SOCIAL"
        )

        val domain = entity.toDomain(null)

        assertEquals("com.instagram.android", domain.packageName)
        assertEquals("Instagram", domain.appName)
        assertEquals(2400000L, domain.totalTimeMs)
        assertEquals(15, domain.sessionCount)
        assertEquals(800000L, domain.longestSessionMs)
        assertEquals(AppCategory.SOCIAL, domain.category)
        assertNull(domain.icon)
    }

    @Test
    fun `AppDailyUsageEntity toDomain handles invalid or null category gracefully`() {
        val entity = AppDailyUsageEntity(
            date = "2026-09-30",
            packageName = "com.unknown.app",
            appName = "Unknown",
            totalTimeMs = 1000L,
            sessionCount = 1,
            longestSessionMs = 1000L,
            category = "INVALID_CATEGORY_NAME"
        )

        val domain = entity.toDomain(null)

        assertNull(domain.category)
    }

    @Test
    fun `DaySummary toEntity maps domain to entity accurately`() {
        val summary = DaySummary(
            date = LocalDate.of(2026, 9, 30),
            totalScreenTimeMs = 5000000L,
            totalUnlocks = 30,
            topApps = listOf(
                AppUsageInfo(
                    packageName = "com.youtube",
                    appName = "YouTube",
                    totalTimeMs = 3000000L,
                    sessionCount = 4,
                    longestSessionMs = 1500000L,
                    category = AppCategory.ENTERTAINMENT
                )
            ),
            longestSession = Session(
                packageName = "com.youtube",
                appName = "YouTube",
                durationMs = 1500000L,
                startTimeMs = 0L,
                endTimeMs = 0L
            ),
            peakHour = 21,
            productiveHour = 10,
            totalSessions = 12
        )

        val entity = summary.toEntity()

        assertEquals("2026-09-30", entity.date)
        assertEquals(5000000L, entity.totalScreenTimeMs)
        assertEquals(30, entity.totalUnlocks)
        assertEquals(1500000L, entity.longestSessionMs)
        assertEquals("com.youtube", entity.longestSessionApp)
        assertEquals("com.youtube", entity.topAppPackage)
        assertEquals(3000000L, entity.topAppTimeMs)
        assertEquals(12, entity.totalSessions)
        assertEquals(21, entity.peakHour)
    }

    @Test
    fun `AppUsageInfo toEntity maps fields correctly`() {
        val info = AppUsageInfo(
            packageName = "com.google.chrome",
            appName = "Chrome",
            totalTimeMs = 1200000L,
            sessionCount = 8,
            longestSessionMs = 400000L,
            category = AppCategory.UTILITY
        )

        val entity = info.toEntity("2026-09-30")

        assertEquals("2026-09-30", entity.date)
        assertEquals("com.google.chrome", entity.packageName)
        assertEquals("Chrome", entity.appName)
        assertEquals(1200000L, entity.totalTimeMs)
        assertEquals(8, entity.sessionCount)
        assertEquals(400000L, entity.longestSessionMs)
        assertEquals("UTILITY", entity.category)
    }
}
