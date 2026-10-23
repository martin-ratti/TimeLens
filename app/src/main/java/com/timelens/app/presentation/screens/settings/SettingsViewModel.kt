package com.timelens.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.service.UsageMonitorService
import com.timelens.app.util.TimeFormatter
import com.timelens.app.worker.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    fun setDailyGoal(hours: Int) {
        prefsManager.setDailyGoalHours(hours)
    }

    fun setThemeMode(mode: com.timelens.app.domain.model.ThemeMode) {
        prefsManager.setThemeMode(mode)
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

    fun exportDataCsv(onSuccess: (Intent) -> Unit, onEmpty: () -> Unit = {}) {
        viewModelScope.launch {
            val days = dailyUsageDao.getLastDays(30).sortedBy { it.date }
            if (days.isEmpty()) {
                onEmpty()
                return@launch
            }
            val csvContent = buildString {
                appendLine("Fecha,TiempoTotalMinutos,TiempoFormateado,Desbloqueos,SesionesTotales,HoraPico,AppPrincipal,SesionMasLargaMinutos")
                for (day in days) {
                    val totalMin = day.totalScreenTimeMs / 60000L
                    val formatted = TimeFormatter.formatMillisToShort(day.totalScreenTimeMs)
                    val longestMin = day.longestSessionMs / 60000L
                    appendLine("${day.date},$totalMin,$formatted,${day.totalUnlocks},${day.totalSessions},${day.peakHour}:00,${day.topAppPackage},$longestMin")
                }
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "TimeLens - Historial de Uso (CSV)")
                putExtra(Intent.EXTRA_TEXT, csvContent)
                type = "text/csv"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, "Exportar datos CSV").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            onSuccess(chooser)
        }
    }
}
