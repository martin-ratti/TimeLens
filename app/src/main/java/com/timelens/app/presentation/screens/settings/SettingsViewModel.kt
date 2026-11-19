package com.timelens.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.util.TimeFormatter
import com.timelens.app.worker.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
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
            val csvContent = buildCsvContent(days)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "TimeLens - Historial de Uso (CSV)")
                type = "text/csv"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                val baseDir = context.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
                val exportDir = File(baseDir, "exports").apply { mkdirs() }
                val csvFile = File(exportDir, "timelens_uso_${LocalDate.now()}.csv")
                csvFile.writeText(csvContent, Charsets.UTF_8)
                val pkgName = context.packageName?.takeIf { it.isNotBlank() } ?: "com.timelens.app"
                val authority = "$pkgName.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, csvFile)
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
            } catch (e: Exception) {
                sendIntent.putExtra(Intent.EXTRA_TEXT, csvContent)
            }

            val chooser = Intent.createChooser(sendIntent, "Exportar datos CSV").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            onSuccess(chooser)
        }
    }

    companion object {
        private val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')

        fun sanitizeCsvCell(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val formulaSafe = if (trimmed.first() in FORMULA_PREFIXES) {
                "'$trimmed"
            } else {
                trimmed
            }
            return if (formulaSafe.contains(',') || formulaSafe.contains('"') || formulaSafe.contains('\n')) {
                "\"${formulaSafe.replace("\"", "\"\"")}\""
            } else {
                formulaSafe
            }
        }

        fun buildCsvContent(days: List<DailyUsageEntity>): String {
            return buildString {
                appendLine("Fecha,TiempoTotalMinutos,TiempoFormateado,Desbloqueos,SesionesTotales,HoraPico,HoraProductiva,AppPrincipal,SesionMasLargaMinutos")
                for (day in days) {
                    val totalMin = day.totalScreenTimeMs / 60000L
                    val formatted = sanitizeCsvCell(TimeFormatter.formatMillisToShort(day.totalScreenTimeMs))
                    val longestMin = day.longestSessionMs / 60000L
                    val dateCell = sanitizeCsvCell(day.date)
                    val topAppCell = sanitizeCsvCell(day.topAppPackage)
                    val peakCell = sanitizeCsvCell("${day.peakHour}:00")
                    val productiveCell = sanitizeCsvCell("${day.productiveHour}:00")
                    appendLine("$dateCell,$totalMin,$formatted,${day.totalUnlocks},${day.totalSessions},$peakCell,$productiveCell,$topAppCell,$longestMin")
                }
            }
        }
    }
}
