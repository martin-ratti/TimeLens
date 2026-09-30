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
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DailySummaryEntryPoint::class.java
            )
            val prefs = entryPoint.prefsManager()
            if (!prefs.notificationsEnabled.value) {
                return Result.success()
            }

            val repository = entryPoint.repository()
            val notificationManager = entryPoint.notificationManager()

            val summary = repository.getTodaySummary()
            val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
            val goal = prefs.dailyGoalHours.value

            val comparisonText = if (yesterday != null && yesterday.totalScreenTimeMs > 0) {
                TimeFormatter.formatPercentageChange(summary.totalScreenTimeMs, yesterday.totalScreenTimeMs)
            } else {
                "Meta: ${goal}h"
            }

            notificationManager.showDailySummaryNotification(summary, comparisonText)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
