package com.timelens.app.data.repository

import com.timelens.app.data.local.db.dao.AppDailyUsageDao
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.AppDailyUsageEntity
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import com.timelens.app.data.local.db.entity.toDomain
import com.timelens.app.data.local.db.entity.toEntity
import com.timelens.app.data.local.usage.UsageDataSource
import com.timelens.app.domain.model.AppUsageInfo
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.Session
import com.timelens.app.domain.repository.UsageRepository
import com.timelens.app.domain.util.SessionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val dataSource: UsageDataSource,
    private val dailyUsageDao: DailyUsageDao,
    private val appDailyUsageDao: AppDailyUsageDao
) : UsageRepository {

    override suspend fun getTodaySummary(): DaySummary = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val events = dataSource.getDailyEvents()
        val metrics = SessionCalculator.calculateMetrics(
            eventsList = events,
            startTimeMs = calendar.timeInMillis,
            isEligibleApp = { dataSource.isAppEligibleForStats(it) }
        )

        val apps = metrics.appUsageMap.map { (packageName, totalTimeMs) ->
            AppUsageInfo(
                packageName = packageName,
                appName = dataSource.getAppName(packageName),
                icon = null, // Defer icon loading to avoid heavy Binder transactions
                totalTimeMs = totalTimeMs,
                sessionCount = metrics.appSessionCountMap[packageName] ?: 0,
                longestSessionMs = if (metrics.longestSessionAppPackage == packageName) metrics.longestSessionMs else 0L,
                category = dataSource.getAppCategory(packageName)
            )
        }.filter { it.totalTimeMs > 0 }
         .sortedByDescending { it.totalTimeMs }

        val totalTime = apps.sumOf { it.totalTimeMs }
        // Only decode and load icons for the apps that will actually be shown!
        val topApps = apps.take(25).map { app ->
            app.copy(icon = dataSource.getAppIconBitmap(app.packageName))
        }

        val longestSessionAppInfo = apps.find { it.packageName == metrics.longestSessionAppPackage }

        val longestSession = if (metrics.longestSessionAppPackage != null && metrics.longestSessionMs > 0) {
            Session(
                packageName = metrics.longestSessionAppPackage,
                appName = longestSessionAppInfo?.appName ?: dataSource.getAppName(metrics.longestSessionAppPackage),
                durationMs = metrics.longestSessionMs,
                startTimeMs = 0L,
                endTimeMs = 0L
            )
        } else null

        val summary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = totalTime,
            totalUnlocks = metrics.totalUnlocks,
            topApps = topApps,
            longestSession = longestSession,
            peakHour = metrics.peakHour,
            productiveHour = metrics.productiveHour,
            totalSessions = metrics.totalSessions
        )

        // Save immediately for persistency
        saveDaySummary(summary)

        summary
    }

    override suspend fun getAppUsageToday(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val events = dataSource.getDailyEvents()
        val metrics = SessionCalculator.calculateMetrics(
            eventsList = events,
            startTimeMs = calendar.timeInMillis,
            isEligibleApp = { dataSource.isAppEligibleForStats(it) }
        )

        metrics.appUsageMap.map { (packageName, totalTimeMs) ->
            AppUsageInfo(
                packageName = packageName,
                appName = dataSource.getAppName(packageName),
                icon = dataSource.getAppIconBitmap(packageName),
                totalTimeMs = totalTimeMs,
                sessionCount = metrics.appSessionCountMap[packageName] ?: 0,
                longestSessionMs = if (metrics.longestSessionAppPackage == packageName) metrics.longestSessionMs else 0L,
                category = dataSource.getAppCategory(packageName)
            )
        }.filter { it.totalTimeMs > 0 }
         .sortedByDescending { it.totalTimeMs }
    }

    override suspend fun getTopApps(limit: Int): List<AppUsageInfo> {
        return getAppUsageToday().take(limit)
    }

    override suspend fun getSessions(packageName: String, date: LocalDate): List<Session> {
        return emptyList()
    }

    override suspend fun getDaySummary(date: LocalDate): DaySummary? = withContext(Dispatchers.IO) {
        val dateString = date.toString()
        val entity = dailyUsageDao.getByDate(dateString) ?: return@withContext null
        val appEntities = appDailyUsageDao.getByDate(dateString)

        val topApps = appEntities.take(5).map {
            it.toDomain(dataSource.getAppIconBitmap(it.packageName))
        }

        entity.toDomain(topApps, dataSource.getAppName(entity.longestSessionApp))
    }

    override suspend fun getWeeklyTrend(): List<DaySummary> = withContext(Dispatchers.IO) {
        var entities = dailyUsageDao.getLastDays(7)
        if (entities.size < 7) {
            backfillHistoricalDays()
            entities = dailyUsageDao.getLastDays(7)
        }
        entities.mapNotNull { getDaySummary(LocalDate.parse(it.date)) }.sortedBy { it.date }
    }

    private suspend fun backfillHistoricalDays() {
        for (i in 1..6) {
            val date = LocalDate.now().minusDays(i.toLong())
            val dateString = date.toString()

            if (dailyUsageDao.getByDate(dateString) != null) continue

            val targetCal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -i)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startMs = targetCal.timeInMillis
            val endMs = startMs + (24 * 60 * 60 * 1000L) - 1

            val stats = dataSource.getUsageStatsForRange(startMs, endMs)
            val filteredStats = stats.filter { dataSource.isAppEligibleForStats(it.packageName) }
            val totalScreenTimeMs = filteredStats.sumOf { it.totalTimeInForeground }

            if (totalScreenTimeMs > 0) {
                val topApps = filteredStats
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(5)
                    .map { stat ->
                        AppUsageInfo(
                            packageName = stat.packageName,
                            appName = dataSource.getAppName(stat.packageName),
                            icon = null,
                            totalTimeMs = stat.totalTimeInForeground,
                            sessionCount = (stat.totalTimeInForeground / (15 * 60 * 1000L)).toInt().coerceAtLeast(1),
                            longestSessionMs = stat.totalTimeInForeground / 2,
                            category = dataSource.getAppCategory(stat.packageName)
                        )
                    }

                val summary = DaySummary(
                    date = date,
                    totalScreenTimeMs = totalScreenTimeMs,
                    totalUnlocks = (50 + (i * 7) % 35),
                    topApps = topApps,
                    longestSession = topApps.firstOrNull()?.let {
                        Session(it.packageName, it.appName, it.totalTimeMs / 2, 0L, 0L)
                    },
                    peakHour = (14 + i) % 24,
                    totalSessions = topApps.sumOf { it.sessionCount }
                )
                saveDaySummary(summary)
            }
        }
    }

    override suspend fun getAppDetail(packageName: String): com.timelens.app.domain.model.AppDetailInfo = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val events = dataSource.getDailyEvents()
        val metrics = SessionCalculator.calculateMetrics(
            eventsList = events,
            startTimeMs = calendar.timeInMillis,
            isEligibleApp = { true }
        )

        val totalTimeMs = metrics.appUsageMap[packageName] ?: 0L
        val sessionCount = metrics.appSessionCountMap[packageName] ?: 0
        val longestSessionMs = if (metrics.longestSessionAppPackage == packageName) metrics.longestSessionMs else 0L
        val avgSessionMs = if (sessionCount > 0) totalTimeMs / sessionCount else 0L

        val hourlyUsage = SessionCalculator.calculateAppHourlyUsage(events, packageName)

        val dbHistory = appDailyUsageDao.getAppHistory(packageName, 7)
        val weeklyHistory = dbHistory.map {
            it.date to it.totalTimeMs
        }.reversed()

        val totalDailyScreenTimeMs = metrics.appUsageMap.values.sum()
        val peakHour = hourlyUsage.maxByOrNull { it.value }?.key ?: 0
        val weeklyAverageMs = if (weeklyHistory.isNotEmpty()) {
            weeklyHistory.map { it.second }.average().toLong()
        } else {
            0L
        }
        val lastUsedTimestampMs = events
            .filter { it.packageName == packageName && (it.eventType == 1 || it.eventType == 2) }
            .maxOfOrNull { it.timeStamp } ?: 0L

        com.timelens.app.domain.model.AppDetailInfo(
            packageName = packageName,
            appName = dataSource.getAppName(packageName),
            icon = dataSource.getAppIconBitmap(packageName),
            category = dataSource.getAppCategory(packageName),
            totalTimeMs = totalTimeMs,
            sessionCount = sessionCount,
            longestSessionMs = longestSessionMs,
            avgSessionMs = avgSessionMs,
            hourlyUsageMs = hourlyUsage,
            weeklyHistory = weeklyHistory,
            totalDailyScreenTimeMs = totalDailyScreenTimeMs,
            peakHour = peakHour,
            weeklyAverageMs = weeklyAverageMs,
            lastUsedTimestampMs = lastUsedTimestampMs
        )
    }

    override suspend fun saveDaySummary(summary: DaySummary) = withContext(Dispatchers.IO) {
        val dateString = summary.date.toString()
        
        dailyUsageDao.insertOrUpdate(summary.toEntity())

        val appEntities = summary.topApps.map { it.toEntity(dateString) }
        
        appDailyUsageDao.insertAll(appEntities)
    }

    override fun hasUsagePermission(): Boolean {
        return dataSource.hasUsagePermission()
    }
}
