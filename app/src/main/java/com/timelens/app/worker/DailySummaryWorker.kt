package com.timelens.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.repository.UsageRepository
import com.timelens.app.domain.usecase.GetWellnessReportUseCase
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.util.TimeFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.LocalTime

class DailySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailySummaryEntryPoint {
        fun repository(): UsageRepository
        fun notificationManager(): TimeLensNotificationManager
        fun prefsManager(): UserPreferencesManager
        fun wellnessReportUseCase(): GetWellnessReportUseCase
    }

    override suspend fun doWork(): Result {
        val isManualTest = inputData.getBoolean("IS_MANUAL_TEST", false)
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DailySummaryEntryPoint::class.java
            )
            val prefs = entryPoint.prefsManager()
            if (!prefs.notificationsEnabled.value) {
                return Result.success()
            }

            if (!isManualTest) {
                // Reprogramar el siguiente resumen diario de las 22:00 para mañana
                NotificationWorkScheduler.scheduleDailySummary(
                    context = applicationContext,
                    forceNextDay = true
                )
            }

            val currentHour = LocalTime.now().hour
            val today = LocalDate.now()
            val alertPrefs = applicationContext.getSharedPreferences("timelens_alerts_state", Context.MODE_PRIVATE)

            // Ventana amplia (20:00..23:59 o 00:00..01:59 si Doze retrasó la ejecución pasando medianoche)
            if (!isManualTest && !isWithinNightSummaryWindow(currentHour)) {
                return Result.success()
            }

            val targetDate = resolveTargetSummaryDate(today, currentHour, isManualTest)
            val targetDateStr = targetDate.toString()

            if (!isManualTest && alertPrefs.getString("last_daily_summary_date", null) == targetDateStr) {
                return Result.success()
            }

            val repository = entryPoint.repository()
            val notificationManager = entryPoint.notificationManager()
            val wellnessUseCase = entryPoint.wellnessReportUseCase()

            val summary = if (targetDate == today) {
                repository.getTodaySummary()
            } else {
                repository.getDaySummary(targetDate) ?: repository.getTodaySummary()
            }
            val previousDay = repository.getDaySummary(targetDate.minusDays(1))
            val goal = prefs.dailyGoalHours.value

            val comparisonText = if (previousDay != null && previousDay.totalScreenTimeMs > 0) {
                TimeFormatter.formatPercentageChange(summary.totalScreenTimeMs, previousDay.totalScreenTimeMs)
            } else {
                "Límite: ${goal}h"
            }

            val report = wellnessUseCase(summary, previousDay, goal, isNightReview = true)

            val shown = notificationManager.showDailySummaryNotification(
                summary = summary,
                comparisonText = comparisonText,
                insight = report.primaryInsight
            )
            if (shown && !isManualTest) {
                alertPrefs.edit().putString("last_daily_summary_date", targetDateStr).apply()
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
        fun isWithinNightSummaryWindow(hour: Int): Boolean {
            return hour in 20..23 || hour in 0..1
        }

        fun resolveTargetSummaryDate(
            today: LocalDate,
            currentHour: Int,
            isManualTest: Boolean = false
        ): LocalDate {
            return if (!isManualTest && currentHour in 0..1) {
                today.minusDays(1)
            } else {
                today
            }
        }
    }
}
