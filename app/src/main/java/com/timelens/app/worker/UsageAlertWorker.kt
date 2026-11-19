package com.timelens.app.worker

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.CancellationException
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

            val isManualTest = inputData.getBoolean("IS_MANUAL_TEST", false)
            val currentHour = java.time.LocalTime.now().hour
            // Respetar horario de descanso nocturno (23:00 a 07:59 inclusive 23:00..06:59) para no despertar UsageStatsManager
            if (!isManualTest && isQuietSleepHour(currentHour)) {
                return Result.success()
            }

            val alertPrefs = applicationContext.getSharedPreferences("timelens_alerts_state", Context.MODE_PRIVATE)
            val today = LocalDate.now()
            val todayStr = today.toString()

            pruneOldAlertKeys(alertPrefs, today)

            val repository = entryPoint.repository()
            val notificationManager = entryPoint.notificationManager()
            val summary = repository.getTodaySummary()
            val nowMs = System.currentTimeMillis()

            // Reiniciar el estado de alertas de sesión larga al cambiar de día
            if (alertPrefs.getString("last_long_session_date", null) != todayStr) {
                alertPrefs.edit()
                    .putString("last_long_session_date", todayStr)
                    .putString("last_long_session_app", "")
                    .putLong("last_long_session_ms", 0L)
                    .apply()
            }

            // 1. Alerta de superación de límite diario
            val goalHours = prefsManager.dailyGoalHours.value
            val goalMs = goalHours * 3600000L
            val lastGoalDate = alertPrefs.getString("last_goal_alert_date", null)

            if (summary.totalScreenTimeMs >= goalMs && lastGoalDate != todayStr) {
                notificationManager.showGoalExceededNotification(summary.totalScreenTimeMs, goalHours)
                alertPrefs.edit().putString("last_goal_alert_date", todayStr).apply()
            }

            // 2. Alerta de sesión prolongada (>= 60 min) — SOLO si la sesión está activa o terminó hace <= 25 min
            val longestSession = summary.longestSession
            val isSessionRecent = longestSession != null &&
                (longestSession.endTimeMs <= 0L || (nowMs - longestSession.endTimeMs) <= 25 * 60 * 1000L)

            if (longestSession != null && isSessionRecent && longestSession.durationMs >= 60 * 60 * 1000L) {
                val lastAlertedMs = alertPrefs.getLong("last_long_session_ms", 0L)
                val lastAlertedApp = alertPrefs.getString("last_long_session_app", "")

                // Notificar si es una nueva app o si pasaron al menos 30 min más de sesión continua
                val isNewSession = lastAlertedApp != longestSession.appName
                val exceededAdditionalThreshold = longestSession.durationMs >= (lastAlertedMs + 30 * 60 * 1000L)

                if (isNewSession || exceededAdditionalThreshold) {
                    val sessionMinutes = longestSession.durationMs / 60000L
                    notificationManager.showLongSessionNotification(longestSession.appName, sessionMinutes)
                    alertPrefs.edit()
                        .putString("last_long_session_date", todayStr)
                        .putString("last_long_session_app", longestSession.appName)
                        .putLong("last_long_session_ms", longestSession.durationMs)
                        .apply()
                }
            }

            // 3. Alerta de nuevo récord de sesión más larga (solo si ocurrió recientemente)
            if (longestSession != null && isSessionRecent && longestSession.durationMs >= 90 * 60 * 1000L) {
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

            // 4. Alertas de Límites por Aplicación (reutilizando topApps o caché recién cargada)
            val appLimits = prefsManager.appLimits.value
            if (appLimits.isNotEmpty()) {
                val summaryAppsMap = summary.topApps.associateBy { it.packageName }
                val missingFromSummary = appLimits.keys.any { it !in summaryAppsMap }
                val appUsageMap = if (missingFromSummary) {
                    repository.getAppUsageToday().associateBy { it.packageName }
                } else {
                    summaryAppsMap
                }

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val RETENTION_DAYS = 7L
        private val DATE_SUFFIX_REGEX = Regex("_(\\d{4}-\\d{2}-\\d{2})$")

        fun isQuietSleepHour(hour: Int): Boolean {
            return hour >= 23 || hour < 8
        }

        fun pruneOldAlertKeys(prefs: SharedPreferences, today: LocalDate = LocalDate.now()) {
            val cutoffDate = today.minusDays(RETENTION_DAYS)
            val keysToRemove = prefs.all.keys.filter { key ->
                if (!key.startsWith("app_limit_")) return@filter false
                val datePart = DATE_SUFFIX_REGEX.find(key)?.groupValues?.getOrNull(1) ?: return@filter false
                val parsed = runCatching { LocalDate.parse(datePart) }.getOrNull() ?: return@filter false
                parsed.isBefore(cutoffDate)
            }
            if (keysToRemove.isNotEmpty()) {
                val editor = prefs.edit()
                keysToRemove.forEach { editor.remove(it) }
                editor.apply()
            }
        }
    }
}
