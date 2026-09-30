package com.timelens.app.domain.usecase

import com.timelens.app.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetWellnessReportUseCaseTest {

    private lateinit var useCase: GetWellnessReportUseCase

    @Before
    fun setUp() {
        useCase = GetWellnessReportUseCase()
    }

    @Test
    fun `when screen time exceeds goal, critical insight is generated with daytime action tip`() {
        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 5 * 3600000L, // 5 horas
            totalUnlocks = 25,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 14,
            totalSessions = 10
        )

        val report = useCase(
            summary = summary,
            yesterday = null,
            goalHours = 4,
            isNightReview = false
        )

        val goalInsight = report.allInsights.find { it.id == "goal_exceeded" }
        assertNotNull(goalInsight)
        assertEquals(InsightLevel.CRITICAL, goalInsight?.level)
        assertTrue(goalInsight?.message?.contains("superando tu meta") == true)
        assertTrue(goalInsight?.actionTip?.contains("Para lo que resta del día") == true)
    }

    @Test
    fun `when opened in night review mode, night closing review is prioritized`() {
        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 3 * 3600000L,
            totalUnlocks = 45,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 20,
            totalSessions = 20
        )

        val report = useCase(
            summary = summary,
            yesterday = null,
            goalHours = 4,
            isNightReview = true
        )

        val nightInsight = report.allInsights.find { it.id == "night_closing_review" }
        assertNotNull(nightInsight)
        assertEquals(InsightCategory.NIGHT_USAGE, nightInsight?.category)
        assertTrue(nightInsight?.title?.contains("22:00 hs") == true)
        assertTrue(nightInsight?.actionTip?.contains("cama") == true || nightInsight?.actionTip?.contains("descanso") == true)
    }

    @Test
    fun `when high unlocks detected, compulsive checking advice is generated`() {
        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 2 * 3600000L,
            totalUnlocks = 80,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 15,
            totalSessions = 50
        )

        val report = useCase(
            summary = summary,
            yesterday = null,
            goalHours = 4,
            isNightReview = false
        )

        val unlockInsight = report.allInsights.find { it.id == "unlocks_high" }
        assertNotNull(unlockInsight)
        assertEquals(InsightLevel.ATTENTION, unlockInsight?.level)
        assertTrue(unlockInsight?.message?.contains("80") == true)
    }

    @Test
    fun `when marathon session in specific app, eye strain tip names the app`() {
        val longestSession = Session(
            packageName = "com.instagram.android",
            appName = "Instagram",
            startTimeMs = 1000L,
            endTimeMs = 1000L + (50 * 60 * 1000L),
            durationMs = 50 * 60 * 1000L
        )

        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 2 * 3600000L,
            totalUnlocks = 30,
            topApps = listOf(
                AppUsageInfo(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    totalTimeMs = 50 * 60 * 1000L,
                    sessionCount = 2,
                    longestSessionMs = 50 * 60 * 1000L,
                    category = AppCategory.SOCIAL
                )
            ),
            longestSession = longestSession,
            peakHour = 16,
            totalSessions = 15
        )

        val report = useCase(
            summary = summary,
            yesterday = null,
            goalHours = 4,
            isNightReview = false
        )

        val sessionInsight = report.allInsights.find { it.id == "session_long" }
        assertNotNull(sessionInsight)
        assertTrue(sessionInsight?.title?.contains("Instagram") == true)
        assertTrue(sessionInsight?.actionTip?.contains("20-20-20") == true)
    }

    @Test
    fun `when social media dominates screen time, social balance advice is included`() {
        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 2 * 3600000L, // 120 min
            totalUnlocks = 30,
            topApps = listOf(
                AppUsageInfo(
                    packageName = "com.zhiliaoapp.musically",
                    appName = "TikTok",
                    totalTimeMs = 70 * 60 * 1000L, // > 50%
                    sessionCount = 5,
                    longestSessionMs = 20 * 60 * 1000L,
                    category = AppCategory.SOCIAL
                )
            ),
            longestSession = null,
            peakHour = 17,
            totalSessions = 20
        )

        val report = useCase(
            summary = summary,
            yesterday = null,
            goalHours = 4,
            isNightReview = false
        )

        val socialInsight = report.allInsights.find { it.id == "social_balance" }
        assertNotNull(socialInsight)
        assertTrue(socialInsight?.message?.contains("TikTok") == true)
        assertTrue(socialInsight?.actionTip?.contains("TikTok") == true)
    }
}
