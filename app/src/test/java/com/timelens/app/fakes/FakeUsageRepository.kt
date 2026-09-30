package com.timelens.app.fakes

import com.timelens.app.domain.model.*
import com.timelens.app.domain.repository.UsageRepository
import java.time.LocalDate

class FakeUsageRepository : UsageRepository {

    var hasPermission: Boolean = true
    var todaySummary: DaySummary = DaySummary(
        date = LocalDate.now(),
        totalScreenTimeMs = 3600000L,
        totalUnlocks = 20,
        topApps = emptyList(),
        longestSession = null,
        peakHour = 14,
        productiveHour = 10,
        totalSessions = 5
    )
    var daySummaries = mutableMapOf<LocalDate, DaySummary>()
    var topAppsList = mutableListOf<AppUsageInfo>()
    var weeklyTrendList = mutableListOf<DaySummary>()
    var shouldThrowError = false

    override suspend fun getTodaySummary(): DaySummary {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return todaySummary
    }

    override suspend fun getAppUsageToday(): List<AppUsageInfo> {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return topAppsList
    }

    override suspend fun getTopApps(limit: Int): List<AppUsageInfo> {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return topAppsList.take(limit)
    }

    override suspend fun getSessions(packageName: String, date: LocalDate): List<Session> {
        return emptyList()
    }

    override suspend fun getDaySummary(date: LocalDate): DaySummary? {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return daySummaries[date]
    }

    override suspend fun getWeeklyTrend(): List<DaySummary> {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return weeklyTrendList
    }

    override suspend fun getAppDetail(packageName: String): AppDetailInfo {
        if (shouldThrowError) throw RuntimeException("Error en repository")
        return AppDetailInfo(
            packageName = packageName,
            appName = "Test App",
            category = AppCategory.SOCIAL,
            totalTimeMs = 1800000L,
            sessionCount = 4,
            longestSessionMs = 900000L,
            avgSessionMs = 450000L,
            hourlyUsageMs = (0..23).associateWith { 0L },
            weeklyHistory = listOf("2026-09-30" to 1800000L)
        )
    }

    override suspend fun saveDaySummary(summary: DaySummary) {
        daySummaries[summary.date] = summary
    }

    override fun hasUsagePermission(): Boolean {
        return hasPermission
    }
}
