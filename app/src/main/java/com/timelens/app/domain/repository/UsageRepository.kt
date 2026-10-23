package com.timelens.app.domain.repository

import com.timelens.app.domain.model.AppUsageInfo
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.Session
import java.time.LocalDate

interface UsageRepository {
    suspend fun getTodaySummary(): DaySummary
    suspend fun getAppUsageToday(): List<AppUsageInfo>
    suspend fun getTopApps(limit: Int = 5): List<AppUsageInfo>
    suspend fun getSessions(packageName: String, date: LocalDate): List<Session>
    suspend fun getDaySummary(date: LocalDate): DaySummary?
    suspend fun getWeeklyTrend(): List<DaySummary>
    suspend fun getTrend(days: Int): List<DaySummary>
    suspend fun getAppDetail(packageName: String): com.timelens.app.domain.model.AppDetailInfo
    suspend fun saveDaySummary(summary: DaySummary)
    fun hasUsagePermission(): Boolean
}
