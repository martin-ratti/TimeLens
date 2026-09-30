package com.timelens.app.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DomainModelsTest {

    @Test
    fun `AppCategory enum contains all expected categories with proper display names`() {
        assertEquals("Social", AppCategory.SOCIAL.displayName)
        assertEquals("Entretenimiento", AppCategory.ENTERTAINMENT.displayName)
        assertEquals("Productividad", AppCategory.PRODUCTIVITY.displayName)
        assertEquals("Comunicación", AppCategory.COMMUNICATION.displayName)
        assertEquals("Juegos", AppCategory.GAMING.displayName)
        assertEquals("Educación", AppCategory.EDUCATION.displayName)
        assertEquals("Utilidades", AppCategory.UTILITY.displayName)
        assertEquals("Otros", AppCategory.OTHER.displayName)
    }

    @Test
    fun `Session default duration calculation and estimation flag`() {
        val session = Session(
            packageName = "com.spotify.music",
            appName = "Spotify",
            startTimeMs = 1000L,
            endTimeMs = 7000L
        )

        assertEquals(6000L, session.durationMs)
        assertFalse(session.isEstimated)
    }

    @Test
    fun `DaySummary default productiveHour and fields verification`() {
        val today = LocalDate.now()
        val summary = DaySummary(
            date = today,
            totalScreenTimeMs = 1000L,
            totalUnlocks = 5,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 12,
            totalSessions = 2
        )

        assertEquals(today, summary.date)
        assertEquals(9, summary.productiveHour)
        assertEquals(12, summary.peakHour)
        assertEquals(5, summary.totalUnlocks)
    }

    @Test
    fun `WellnessInsight and InsightLevel completeness`() {
        val insight = WellnessInsight(
            id = "test_insight",
            title = "Test Title",
            message = "Test Message",
            actionTip = "Test Tip",
            level = InsightLevel.EXCELLENT,
            category = InsightCategory.SCREEN_TIME
        )

        assertEquals(InsightLevel.EXCELLENT, insight.level)
        assertEquals(InsightCategory.SCREEN_TIME, insight.category)
        assertEquals("test_insight", insight.id)
    }

    @Test
    fun `AppDetailInfo defaults and integrity`() {
        val detail = AppDetailInfo(
            packageName = "com.whatsapp",
            appName = "WhatsApp"
        )

        assertEquals("com.whatsapp", detail.packageName)
        assertEquals("WhatsApp", detail.appName)
        assertEquals(AppCategory.OTHER, detail.category)
        assertEquals(0L, detail.totalTimeMs)
        assertEquals(0, detail.sessionCount)
        assertTrue(detail.hourlyUsageMs.isEmpty())
        assertTrue(detail.weeklyHistory.isEmpty())
    }
}
