package com.timelens.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.worker.DailySyncWorker
import com.timelens.app.worker.NotificationWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TimeLensApp : Application() {

    @Inject
    lateinit var notificationManager: TimeLensNotificationManager

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
        setupNotifications()
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySyncWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    private fun setupNotifications() {
        notificationManager.createNotificationChannels()
        if (prefsManager.notificationsEnabled.value) {
            NotificationWorkScheduler.scheduleAll(this)
        }
    }
}
