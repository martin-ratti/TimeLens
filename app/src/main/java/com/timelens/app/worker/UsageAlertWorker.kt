package com.timelens.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.repository.UsageRepository
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.util.TimeFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

class UsageAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UsageAlertEntryPoint {
        fun repository(): UsageRepository
        fun notificationManager(): TimeLensNotificationManager
        fun prefsManager(): UserPreferencesManager
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                UsageAlertEntryPoint::class.java
            )
            val prefsManager = entryPoint.prefsManager()
            if (!prefsManager.notificationsEnabled.value) {
                return Result.success()
            }

            val repository = entryPoint.repository()
            val notificationManager = entryPoint.notificationManager()
            val summary = repository.getTodaySummary()

            val alertPrefs = applicationContext.getSharedPreferences("timelens_alerts_state", Context.MODE_PRIVATE)
            val todayStr = LocalDate.now().toString()

            // 1. Alerta de superación de objetivo diario
            val goalHours = prefsManager.dailyGoalHours.value
            val goalMs = goalHours * 3600000L
            val lastGoalDate = alertPrefs.getString("last_goal_alert_date", null)

            if (summary.totalScreenTimeMs >= goalMs && lastGoalDate != todayStr) {
                notificationManager.showGoalExceededNotification(summary.totalScreenTimeMs, goalHours)
                alertPrefs.edit().putString("last_goal_alert_date", todayStr).apply()
            }

            // 2. Alerta de sesión prolongada (>= 60 min)
            val longestSession = summary.longestSession
            if (longestSession != null && longestSession.durationMs >= 60 * 60 * 1000L) {
                val lastAlertedMs = alertPrefs.getLong("last_long_session_ms", 0L)
                val lastAlertedApp = alertPrefs.getString("last_long_session_app", "")

                // Notificar si es una nueva app o si pasaron al menos 30 min más de sesión continua
                val isNewSession = lastAlertedApp != longestSession.appName
                val exceededAdditionalThreshold = longestSession.durationMs >= (lastAlertedMs + 30 * 60 * 1000L)

                if (isNewSession || exceededAdditionalThreshold) {
                    val sessionMinutes = longestSession.durationMs / 60000L
                    notificationManager.showLongSessionNotification(longestSession.appName, sessionMinutes)
                    alertPrefs.edit()
                        .putString("last_long_session_app", longestSession.appName)
                        .putLong("last_long_session_ms", longestSession.durationMs)
                        .apply()
                }
            }

            // 3. Alerta de nuevo récord de sesión más larga
            if (longestSession != null && longestSession.durationMs >= 90 * 60 * 1000L) {
                val lastRecordDate = alertPrefs.getString("last_record_alert_date", null)
                if (lastRecordDate != todayStr) {
                    val weeklyTrend = repository.getWeeklyTrend()
                    val pastDays = weeklyTrend.filter { it.date != summary.date }
                    val maxPastSessionMs = pastDays.mapNotNull { it.longestSession?.durationMs }.maxOrNull() ?: 0L

                    if (maxPastSessionMs > 0 && longestSession.durationMs > maxPastSessionMs) {
                        notificationManager.showRecordSessionNotification(
                            longestSession.appName,
                            TimeFormatter.formatMillisToShort(longestSession.durationMs)
                        )
                        alertPrefs.edit().putString("last_record_alert_date", todayStr).apply()
                    }
                }
            }

            // 4. Alertas de Límites por Aplicación
            val appLimits = prefsManager.appLimits.value
            if (appLimits.isNotEmpty()) {
                val appUsageList = repository.getAppUsageToday()
                val appUsageMap = appUsageList.associateBy { it.packageName }

                for ((pkg, limitMinutes) in appLimits) {
                    val appUsage = appUsageMap[pkg] ?: continue
                    val usedMinutes = (appUsage.totalTimeMs / (60 * 1000L)).toInt()
                    val limitKey = "app_limit_${pkg}_$todayStr"
                    val lastAlertLevel = alertPrefs.getInt(limitKey, 0)

                    if (usedMinutes >= limitMinutes && lastAlertLevel < 100) {
                        notificationManager.showAppLimitNotification(
                            packageName = pkg,
                            appName = appUsage.appName,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            isExceeded = true
                        )
                        alertPrefs.edit().putInt(limitKey, 100).apply()
                    } else if (usedMinutes >= (limitMinutes * 0.8) && lastAlertLevel < 80) {
                        notificationManager.showAppLimitNotification(
                            packageName = pkg,
                            appName = appUsage.appName,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            isExceeded = false
                        )
                        alertPrefs.edit().putInt(limitKey, 80).apply()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
