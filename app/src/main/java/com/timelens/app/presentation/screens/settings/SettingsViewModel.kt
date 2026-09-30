package com.timelens.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.service.UsageMonitorService
import com.timelens.app.worker.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: UserPreferencesManager,
    private val dailyUsageDao: DailyUsageDao,
    private val notificationManager: TimeLensNotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val dailyGoalHours: StateFlow<Int> = prefsManager.dailyGoalHours
    val notificationsEnabled: StateFlow<Boolean> = prefsManager.notificationsEnabled
    val darkThemeEnabled: StateFlow<Boolean> = prefsManager.darkThemeEnabled
    val themeMode: StateFlow<com.timelens.app.domain.model.ThemeMode> = prefsManager.themeMode
    val dynamicColorEnabled: StateFlow<Boolean> = prefsManager.dynamicColorEnabled

    fun setDailyGoal(hours: Int) {
        prefsManager.setDailyGoalHours(hours)
    }

    fun setThemeMode(mode: com.timelens.app.domain.model.ThemeMode) {
        prefsManager.setThemeMode(mode)
    }

    fun toggleDynamicColor(enabled: Boolean) {
        prefsManager.setDynamicColorEnabled(enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        prefsManager.setNotificationsEnabled(enabled)
        if (enabled) {
            notificationManager.createNotificationChannels()
            NotificationWorkScheduler.scheduleAll(context)
        } else {
            NotificationWorkScheduler.cancelAll(context)
            notificationManager.cancelAll()
            UsageMonitorService.stop(context)
        }
    }

    fun sendTestNotification(): Boolean {
        return notificationManager.showTestNotification()
    }

    fun triggerDailySummaryTest() {
        NotificationWorkScheduler.triggerImmediateDailySummary(context)
    }

    fun triggerAlertCheckTest() {
        NotificationWorkScheduler.triggerImmediateAlertCheck(context)
    }

    fun toggleDarkTheme(enabled: Boolean) {
        prefsManager.setDarkThemeEnabled(enabled)
    }

    fun shareApp(onSuccess: (Intent) -> Unit) {
        val shareMessage = """
            ⏱️ ¡Te recomiendo TimeLens!
            
            Una app para tomar el control de tu tiempo de pantalla, descubrir tus horarios pico y crear hábitos digitales más saludables.
            
            🛡️ 100% privada, funciona offline y sin rastreadores.
            Conoce más en: https://github.com/martin-ratti/TimeLens
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val chooser = Intent.createChooser(sendIntent, "Compartir TimeLens").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        onSuccess(chooser)
    }
}
