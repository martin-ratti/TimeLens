package com.timelens.app.data.local.db.entity

import com.timelens.app.domain.model.AppUsageInfo
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.Session
import java.time.LocalDate

fun DailyUsageEntity.toDomain(topApps: List<AppUsageInfo>, appName: String): DaySummary {
    return DaySummary(
        date = LocalDate.parse(this.date),
        totalScreenTimeMs = this.totalScreenTimeMs,
        totalUnlocks = this.totalUnlocks,
        topApps = topApps,
        longestSession = if (this.longestSessionApp.isNotEmpty()) {
            Session(
                packageName = this.longestSessionApp,
                appName = appName,
                startTimeMs = 0L,
                endTimeMs = this.longestSessionMs,
                durationMs = this.longestSessionMs
            )
        } else null,
        peakHour = this.peakHour,
        totalSessions = this.totalSessions
    )
}

fun AppDailyUsageEntity.toDomain(icon: Any?): AppUsageInfo {
    return AppUsageInfo(
        packageName = this.packageName,
        appName = this.appName,
        icon = icon,
        totalTimeMs = this.totalTimeMs,
        sessionCount = this.sessionCount,
        longestSessionMs = this.longestSessionMs,
        category = this.category?.let { runCatching { com.timelens.app.domain.model.AppCategory.valueOf(it) }.getOrNull() }
    )
}

fun DaySummary.toEntity(): DailyUsageEntity {
    return DailyUsageEntity(
        date = this.date.toString(),
        totalScreenTimeMs = this.totalScreenTimeMs,
        totalUnlocks = this.totalUnlocks,
        longestSessionMs = this.longestSession?.durationMs ?: 0L,
        longestSessionApp = this.longestSession?.packageName ?: "",
        topAppPackage = this.topApps.firstOrNull()?.packageName ?: "",
        topAppTimeMs = this.topApps.firstOrNull()?.totalTimeMs ?: 0L,
        totalSessions = this.totalSessions,
        peakHour = this.peakHour
    )
}

fun AppUsageInfo.toEntity(date: String): AppDailyUsageEntity {
    return AppDailyUsageEntity(
        date = date,
        packageName = this.packageName,
        appName = this.appName,
        totalTimeMs = this.totalTimeMs,
        sessionCount = this.sessionCount,
        longestSessionMs = this.longestSessionMs,
        category = this.category?.name
    )
}
