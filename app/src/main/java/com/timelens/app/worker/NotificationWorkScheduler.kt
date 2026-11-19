package com.timelens.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {

    private const val UNIQUE_WORK_SUMMARY = "DailySummaryWorker"
    private const val UNIQUE_WORK_ALERTS = "UsageAlertWorker"

    fun calculateNext2200DelayMillis(
        now: LocalDateTime = LocalDateTime.now(),
        forceNextDay: Boolean = false
    ): Long {
        var targetTime = now.withHour(22).withMinute(0).withSecond(0).withNano(0)
        if (forceNextDay || !now.isBefore(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        return Duration.between(now, targetTime).toMillis().coerceAtLeast(60_000L)
    }

    fun scheduleDailySummary(
        context: Context,
        forceNextDay: Boolean = false,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
    ) {
        val initialDelay = calculateNext2200DelayMillis(forceNextDay = forceNextDay)
        val summaryRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_SUMMARY,
            policy,
            summaryRequest
        )
    }

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Programar Resumen Diario dinámico para las 22:00 hs con OneTimeWorkRequest
        scheduleDailySummary(context, forceNextDay = false, policy = ExistingWorkPolicy.REPLACE)

        // 2. Programar Alertas Periódicas (cada 15 min)
        val alertsRequest = PeriodicWorkRequestBuilder<UsageAlertWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_ALERTS,
            ExistingPeriodicWorkPolicy.KEEP,
            alertsRequest
        )
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_WORK_SUMMARY)
        workManager.cancelUniqueWork(UNIQUE_WORK_ALERTS)
    }

    fun triggerImmediateAlertCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<UsageAlertWorker>()
            .setInputData(workDataOf("IS_MANUAL_TEST" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "UsageAlertWorker_Immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerImmediateDailySummary(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInputData(workDataOf("IS_MANUAL_TEST" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "DailySummaryWorker_Immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
