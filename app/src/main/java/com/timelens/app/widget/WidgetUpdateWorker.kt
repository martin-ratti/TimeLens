package com.timelens.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Refresca los widgets de la pantalla de inicio con los datos más recientes. */
class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val glanceManager = GlanceAppWidgetManager(applicationContext)
            val ringIds = glanceManager.getGlanceIds(TimeLensRingWidget::class.java)
            val summaryIds = glanceManager.getGlanceIds(TimeLensSummaryWidget::class.java)

            if (ringIds.isEmpty() && summaryIds.isEmpty()) {
                return Result.success()
            }

            // Calculamos y cacheamos el snapshot una sola vez para compartirlo entre ambos widgets
            WidgetDataLoader.load(applicationContext, forceRefresh = true)

            if (ringIds.isNotEmpty()) {
                TimeLensRingWidget().updateAll(applicationContext)
            }
            if (summaryIds.isNotEmpty()) {
                TimeLensSummaryWidget().updateAll(applicationContext)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "TimeLensWidgetPeriodicUpdate"
        private const val IMMEDIATE_NAME = "TimeLensWidgetImmediateUpdate"

        fun hasActiveWidgets(context: Context): Boolean {
            return try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return false
                val ringIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimeLensRingWidgetReceiver::class.java)
                )
                val summaryIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TimeLensSummaryWidgetReceiver::class.java)
                )
                ringIds.isNotEmpty() || summaryIds.isNotEmpty()
            } catch (e: Exception) {
                true
            }
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun refreshNow(context: Context) {
            if (!hasActiveWidgets(context)) return
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
