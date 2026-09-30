package com.timelens.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timelens.app.R
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.presentation.MainActivity
import com.timelens.app.util.TimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeLensNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ALERTS_ID = "timelens_alerts"
        const val CHANNEL_SUMMARY_ID = "timelens_summary"
        const val CHANNEL_MONITOR_ID = "timelens_monitor"

        const val NOTIFICATION_ID_GOAL = 1001
        const val NOTIFICATION_ID_LONG_SESSION = 1002
        const val NOTIFICATION_ID_RECORD = 1003
        const val NOTIFICATION_ID_SUMMARY = 1004
        const val NOTIFICATION_ID_TEST = 1005
        const val NOTIFICATION_ID_SERVICE = 1006
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Alertas de Bienestar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas cuando superás tu objetivo diario o tenés sesiones prolongadas"
                enableVibration(true)
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY_ID,
                "Resumen Diario",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumen diario de uso de pantalla y hábitos nocturnos"
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                "Monitoreo en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo silencioso en tiempo real del uso de aplicaciones"
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(summaryChannel)
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun showGoalExceededNotification(usedMillis: Long, goalHours: Int): Boolean {
        createNotificationChannels()
        if (!hasNotificationPermission()) {
            android.util.Log.w("TimeLensNotify", "Permiso no concedido para showGoalExceededNotification")
            return false
        }

        val formattedTime = TimeFormatter.formatMillisToShort(usedMillis)
        val content = "Alcanzaste $formattedTime de pantalla hoy ($goalHours h de límite). ¡Un momento ideal para desconectar y descansar la vista!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎯 ¡Objetivo diario alcanzado!")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_GOAL, notification)
        return true
    }

    fun showLongSessionNotification(appName: String, sessionMinutes: Long): Boolean {
        createNotificationChannels()
        if (!hasNotificationPermission()) {
            android.util.Log.w("TimeLensNotify", "Permiso no concedido para showLongSessionNotification")
            return false
        }

        val content = "Llevás $sessionMinutes min continuos en $appName. ¡Recordá estirar el cuerpo y descansar la vista!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏳ Sesión prolongada en $appName")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_LONG_SESSION, notification)
        return true
    }

    fun showRecordSessionNotification(appName: String, durationFormatted: String): Boolean {
        createNotificationChannels()
        if (!hasNotificationPermission()) {
            android.util.Log.w("TimeLensNotify", "Permiso no concedido para showRecordSessionNotification")
            return false
        }

        val content = "Registraste una sesión de $durationFormatted en $appName, tu sesión más larga de los últimos días."

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Nueva sesión récord")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECORD, notification)
        return true
    }

    fun showDailySummaryNotification(
        summary: DaySummary,
        comparisonText: String,
        insight: com.timelens.app.domain.model.WellnessInsight? = null
    ): Boolean {
        createNotificationChannels()
        if (!hasNotificationPermission()) {
            android.util.Log.w("TimeLensNotify", "Permiso no concedido para showDailySummaryNotification")
            return false
        }

        val formattedTotal = TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)
        val topApp = summary.topApps.firstOrNull()?.appName ?: "Sin uso"
        val topAppTime = summary.topApps.firstOrNull()?.let { TimeFormatter.formatMillisToShort(it.totalTimeMs) } ?: "0m"

        val shortContent = if (insight != null) {
            "${insight.title} • $formattedTotal ($comparisonText)"
        } else {
            "Uso total: $formattedTotal ($comparisonText). Desbloqueos: ${summary.totalUnlocks}."
        }

        val expandedContent = buildString {
            append("Hoy usaste tu teléfono $formattedTotal ($comparisonText).\n")
            append("• App principal: $topApp ($topAppTime)\n")
            append("• Desbloqueos: ${summary.totalUnlocks}\n")
            append("• Sesión más larga: ${summary.longestSession?.let { TimeFormatter.formatMillisToShort(it.durationMs) } ?: "0m"}")
            if (insight != null) {
                append("\n\n💡 Consejo de Bienestar:\n")
                append("${insight.title}: ${insight.actionTip}")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🌙 Resumen del día — TimeLens")
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_SUMMARY, notification)
        return true
    }

    fun showTestNotification(): Boolean {
        createNotificationChannels()
        if (!hasNotificationPermission()) {
            android.util.Log.w("TimeLensNotify", "Permiso no concedido para showTestNotification")
            return false
        }

        val content = "¡Excelente! Las notificaciones y alertas de bienestar digital de TimeLens están configuradas y funcionando al 100%."

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✨ TimeLens: Notificaciones Activas")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_TEST, notification)
        return true
    }

    fun buildForegroundNotification(): Notification {
        val content = "Monitoreando hábitos de uso y bienestar digital"

        return NotificationCompat.Builder(context, CHANNEL_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("TimeLens activo")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(getMainActivityPendingIntent())
            .build()
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}
