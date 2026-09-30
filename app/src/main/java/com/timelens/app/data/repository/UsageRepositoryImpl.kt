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
        syncRealHistoricalDays()
        val entities = dailyUsageDao.getLastDays(7)
        entities.mapNotNull { getDaySummary(LocalDate.parse(it.date)) }.sortedBy { it.date }
    }

    private suspend fun syncRealHistoricalDays() {
        val today = LocalDate.now()
        // Purgar inmediatamente cualquier residuo artificial y registros desactualizados
        dailyUsageDao.deleteArtificialRecords()
        appDailyUsageDao.deleteOrphanedRecords()
        val cutoffDate = today.minusDays(7).toString()
        dailyUsageDao.deleteOlderThan(cutoffDate)
        appDailyUsageDao.deleteOlderThan(cutoffDate)

        for (i in 1..6) {
            val date = today.minusDays(i.toLong())
            val dateString = date.toString()

            val existing = dailyUsageDao.getByDate(dateString)
            if (existing != null) {
                continue
            }

            val targetCal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -i)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startMs = targetCal.timeInMillis
            val endMs = startMs + (24 * 60 * 60 * 1000L) - 1

            val events = dataSource.getEventsForRange(startMs, endMs)

            if (events.isNotEmpty()) {
                val metrics = SessionCalculator.calculateMetrics(
                    eventsList = events,
                    startTimeMs = startMs,
                    isEligibleApp = { dataSource.isAppEligibleForStats(it) }
                )

                val apps = metrics.appUsageMap.map { (packageName, totalTimeMs) ->
                    AppUsageInfo(
                        packageName = packageName,
                        appName = dataSource.getAppName(packageName),
                        icon = null,
                        totalTimeMs = totalTimeMs,
                        sessionCount = metrics.appSessionCountMap[packageName] ?: 0,
                        longestSessionMs = if (metrics.longestSessionAppPackage == packageName) metrics.longestSessionMs else 0L,
                        category = dataSource.getAppCategory(packageName)
                    )
                }.filter { it.totalTimeMs > 0 }
                 .sortedByDescending { it.totalTimeMs }

                val totalTime = apps.sumOf { it.totalTimeMs }

                if (totalTime > 0) {
                    val longestSessionAppInfo = apps.find { it.packageName == metrics.longestSessionAppPackage }
                    val longestSession = if (metrics.longestSessionAppPackage != null && metrics.longestSessionMs > 0) {
                        Session(
                            packageName = metrics.longestSessionAppPackage,
                            appName = longestSessionAppInfo?.appName ?: dataSource.getAppName(metrics.longestSessionAppPackage),
                            durationMs = metrics.longestSessionMs,
                            startTimeMs = 0L,
                            endTimeMs = metrics.longestSessionMs
                        )
                    } else null

                    val summary = DaySummary(
                        date = date,
                        totalScreenTimeMs = totalTime,
                        totalUnlocks = metrics.totalUnlocks,
                        topApps = apps.take(25),
                        longestSession = longestSession,
                        peakHour = metrics.peakHour,
                        productiveHour = metrics.productiveHour,
                        totalSessions = metrics.totalSessions
                    )
                    saveDaySummary(summary)
                }
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

        val todayDateStr = LocalDate.now().toString()
        val dbHistory = appDailyUsageDao.getAppHistory(packageName, 7)
            .filter { it.date != todayDateStr }
        
        val historyList = mutableListOf<Pair<String, Long>>()
        historyList.addAll(dbHistory.map { it.date to it.totalTimeMs })
        if (totalTimeMs > 0L) {
            historyList.add(todayDateStr to totalTimeMs)
        }
        val weeklyHistory = historyList.sortedBy { it.first }

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
