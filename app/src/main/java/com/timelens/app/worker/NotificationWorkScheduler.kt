package com.timelens.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {

    private const val UNIQUE_WORK_SUMMARY = "DailySummaryWorker"
    private const val UNIQUE_WORK_ALERTS = "UsageAlertWorker"

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Programar Resumen Diario para las 22:00 hs
        val now = LocalDateTime.now()
        var targetTime = now.withHour(22).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelay = Duration.between(now, targetTime).toMillis()

        val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_SUMMARY,
            ExistingPeriodicWorkPolicy.UPDATE,
            summaryRequest
        )

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
        val request = OneTimeWorkRequestBuilder<UsageAlertWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "UsageAlertWorker_Immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerImmediateDailySummary(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "DailySummaryWorker_Immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
