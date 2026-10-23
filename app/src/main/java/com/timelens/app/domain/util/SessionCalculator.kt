package com.timelens.app.domain.util

import android.app.usage.UsageEvents
import java.util.Calendar

data class UsageEventModel(
    val packageName: String?,
    val eventType: Int,
    val timeStamp: Long
)

data class MetricsResult(
    val totalUnlocks: Int,
    val longestSessionMs: Long,
    val longestSessionAppPackage: String?,
    val peakHour: Int,
    val productiveHour: Int = 9,
    val totalSessions: Int,
    val appUsageMap: Map<String, Long>,
    val appSessionCountMap: Map<String, Int>,
    val appLongestSessionMap: Map<String, Long> = emptyMap(),
    val longestSessionStartTimeMs: Long = 0L,
    val longestSessionEndTimeMs: Long = 0L
)

object SessionCalculator {

    private fun distributeSessionAcrossHours(
        startMs: Long,
        endMs: Long,
        hourMap: MutableMap<Int, Long>,
        cal: Calendar
    ) {
        var cursor = startMs
        while (cursor < endMs) {
            cal.timeInMillis = cursor
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            val nextHourStart = cal.timeInMillis
            val segmentEnd = minOf(endMs, nextHourStart)
            val segmentDuration = (segmentEnd - cursor).coerceAtLeast(0L)
            if (segmentDuration > 0L) {
                hourMap[hour] = (hourMap[hour] ?: 0L) + segmentDuration
            }
            cursor = if (segmentEnd > cursor) segmentEnd else endMs
        }
    }

    fun calculateProductiveHour(
        hourUsageMap: Map<Int, Long>,
        activeWindowStart: Int = 8,
        activeWindowEnd: Int = 21,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): Int {
        val maxHour = if (currentHour in activeWindowStart..activeWindowEnd) currentHour else activeWindowEnd
        val validHours = (activeWindowStart..maxHour).toList()
        if (validHours.isEmpty()) return activeWindowStart

        return validHours.minByOrNull { hour ->
            hourUsageMap[hour] ?: 0L
        } ?: activeWindowStart
    }

    fun calculateMetrics(
        eventsList: List<UsageEvents.Event>,
        startTimeMs: Long,
        isEligibleApp: (String) -> Boolean = { true },
        evalCurrentTime: Long? = null
    ): MetricsResult {
        val models = eventsList.map {
            UsageEventModel(
                packageName = it.packageName,
                eventType = it.eventType,
                timeStamp = it.timeStamp
            )
        }
        return calculateMetricsFromModels(models, startTimeMs, isEligibleApp, evalCurrentTime)
    }

    fun calculateMetricsFromModels(
        eventsList: List<UsageEventModel>,
        startTimeMs: Long,
        isEligibleApp: (String) -> Boolean = { true },
        evalCurrentTime: Long? = null
    ): MetricsResult {
        var unlocks = 0
        var longestSessionMs = 0L
        var longestSessionAppPackage: String? = null
        var longestSessionStartTimeMs = 0L
        var longestSessionEndTimeMs = 0L
        val hourUsageMap = mutableMapOf<Int, Long>()
        val appUsageMap = mutableMapOf<String, Long>()
        val appSessionCountMap = mutableMapOf<String, Int>()
        val appLongestSessionMap = mutableMapOf<String, Long>()
        val sharedCal = Calendar.getInstance()

        var currentApp: String? = null
        var sessionStartTime: Long = 0L
        var pendingPauseTime: Long? = null

        fun recordSession(app: String, rawStart: Long, endTime: Long) {
            val start = maxOf(rawStart, startTimeMs)
            if (endTime <= start || !isEligibleApp(app)) return
            val duration = endTime - start

            // Ignore corrupted sessions longer than 12 hours
            if (duration > 12 * 60 * 60 * 1000L) return

            appUsageMap[app] = (appUsageMap[app] ?: 0L) + duration
            appSessionCountMap[app] = (appSessionCountMap[app] ?: 0) + 1
            appLongestSessionMap[app] = maxOf(appLongestSessionMap[app] ?: 0L, duration)

            if (duration > longestSessionMs) {
                longestSessionMs = duration
                longestSessionAppPackage = app
                longestSessionStartTimeMs = start
                longestSessionEndTimeMs = endTime
            }

            distributeSessionAcrossHours(start, endTime, hourUsageMap, sharedCal)
        }

        fun closeCurrentSession(endTime: Long) {
            val app = currentApp ?: return
            val start = sessionStartTime
            if (endTime > start) {
                recordSession(app, start, endTime)
            }
            currentApp = null
            sessionStartTime = 0L
            pendingPauseTime = null
        }

        for (event in eventsList) {
            when (event.eventType) {
                // Keyguard hidden / device unlocked
                18 -> {
                    if (event.timeStamp >= startTimeMs) {
                        unlocks++
                    }
                }

                // Foreground Activity Resumed / Move to Foreground
                1 -> {
                    val pkg = event.packageName
                    if (pkg == currentApp) {
                        // Navigating within the same app - cancel pending pause
                        pendingPauseTime = null
                    } else {
                        // Transitioning to a different app
                        val effectiveEndTime = pendingPauseTime ?: event.timeStamp
                        closeCurrentSession(effectiveEndTime)

                        currentApp = pkg
                        sessionStartTime = event.timeStamp
                        pendingPauseTime = null
                    }
                }

                // Activity Paused / Move to Background
                2 -> {
                    if (event.packageName == currentApp) {
                        pendingPauseTime = event.timeStamp
                    }
                }

                // Screen turned off or device locked
                16, 17 -> { // SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN
                    val effectiveEndTime = pendingPauseTime ?: event.timeStamp
                    closeCurrentSession(effectiveEndTime)
                }

                // Device shutdown
                26 -> {
                    val effectiveEndTime = pendingPauseTime ?: event.timeStamp
                    closeCurrentSession(effectiveEndTime)
                }
            }
        }

        // Close any ongoing active session at the current time
        if (currentApp != null) {
            val currentTime = evalCurrentTime ?: System.currentTimeMillis()
            val effectiveEndTime = pendingPauseTime ?: currentTime
            closeCurrentSession(effectiveEndTime)
        }

        val peakHour = hourUsageMap.maxByOrNull { it.value }?.key ?: 0
        val productiveHour = calculateProductiveHour(hourUsageMap)
        val totalSessions = appSessionCountMap.values.sum()

        return MetricsResult(
            totalUnlocks = unlocks,
            longestSessionMs = longestSessionMs,
            longestSessionAppPackage = longestSessionAppPackage,
            peakHour = peakHour,
            productiveHour = productiveHour,
            totalSessions = totalSessions,
            appUsageMap = appUsageMap,
            appSessionCountMap = appSessionCountMap,
            appLongestSessionMap = appLongestSessionMap,
            longestSessionStartTimeMs = longestSessionStartTimeMs,
            longestSessionEndTimeMs = longestSessionEndTimeMs
        )
    }

    fun calculateAppHourlyUsage(
        eventsList: List<UsageEvents.Event>,
        targetPackage: String
    ): Map<Int, Long> {
        val models = eventsList.map {
            UsageEventModel(
                packageName = it.packageName,
                eventType = it.eventType,
                timeStamp = it.timeStamp
            )
        }
        return calculateAppHourlyUsageFromModels(models, targetPackage)
    }

    fun calculateAppHourlyUsageFromModels(
        eventsList: List<UsageEventModel>,
        targetPackage: String,
        evalCurrentTime: Long? = null
    ): Map<Int, Long> {
        val hourlyMap = (0..23).associateWith { 0L }.toMutableMap()
        val sharedCal = Calendar.getInstance()
        var currentApp: String? = null
        var sessionStartTime: Long = 0L
        var pendingPauseTime: Long? = null

        fun recordSession(start: Long, duration: Long) {
            if (duration <= 0 || duration > 12 * 60 * 60 * 1000L) return
            distributeSessionAcrossHours(start, start + duration, hourlyMap, sharedCal)
        }

        fun closeSession(endTime: Long) {
            if (currentApp == targetPackage && endTime > sessionStartTime) {
                recordSession(sessionStartTime, endTime - sessionStartTime)
            }
            currentApp = null
            sessionStartTime = 0L
            pendingPauseTime = null
        }

        for (event in eventsList) {
            when (event.eventType) {
                1 -> { // Resumed
                    if (event.packageName == currentApp) {
                        pendingPauseTime = null
                    } else {
                        val effectiveEndTime = pendingPauseTime ?: event.timeStamp
                        closeSession(effectiveEndTime)

                        currentApp = event.packageName
                        sessionStartTime = event.timeStamp
                        pendingPauseTime = null
                    }
                }
                2 -> { // Paused
                    if (event.packageName == currentApp) {
                        pendingPauseTime = event.timeStamp
                    }
                }
                16, 17, 26 -> { // Screen off, lock, shutdown
                    val effectiveEndTime = pendingPauseTime ?: event.timeStamp
                    closeSession(effectiveEndTime)
                }
            }
        }

        if (currentApp == targetPackage) {
            val currentTime = evalCurrentTime ?: System.currentTimeMillis()
            val effectiveEndTime = pendingPauseTime ?: currentTime
            closeSession(effectiveEndTime)
        }

        return hourlyMap
    }
}
