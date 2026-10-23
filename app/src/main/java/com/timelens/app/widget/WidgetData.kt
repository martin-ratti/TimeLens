package com.timelens.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidBitmap
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.repository.UsageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

data class WidgetApp(
    val name: String,
    val timeMs: Long,
    val icon: Bitmap?
)

data class WidgetUsageData(
    val hasPermission: Boolean = true,
    val hasError: Boolean = false,
    val totalMs: Long = 0L,
    val goalHours: Int = 6,
    val unlocks: Int = 0,
    val topApps: List<WidgetApp> = emptyList()
) {
    val goalMs: Long get() = goalHours * 3_600_000L

    /** Proporción consumida del límite diario (puede superar 1f). */
    val ratio: Float get() = if (goalMs > 0L) totalMs.toFloat() / goalMs else 0f

    val status: WidgetStatus
        get() = when {
            ratio >= 1f -> WidgetStatus.EXCEEDED
            ratio >= 0.8f -> WidgetStatus.NEAR_LIMIT
            else -> WidgetStatus.OK
        }
}

enum class WidgetStatus(val argb: Int) {
    OK(0xFF00D4FF.toInt()),
    NEAR_LIMIT(0xFFFF9100.toInt()),
    EXCEEDED(0xFFFF5252.toInt())
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): UsageRepository
    fun preferences(): UserPreferencesManager
}

object WidgetDataLoader {

    suspend fun load(context: Context): WidgetUsageData {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            val repository = entryPoint.repository()
            val goal = entryPoint.preferences().dailyGoalHours.value

            if (!repository.hasUsagePermission()) {
                return WidgetUsageData(hasPermission = false, goalHours = goal)
            }

            val summary = repository.getTodaySummary()
            WidgetUsageData(
                totalMs = summary.totalScreenTimeMs,
                goalHours = goal,
                unlocks = summary.totalUnlocks,
                topApps = summary.topApps.take(3).map { app ->
                    WidgetApp(
                        name = app.appName,
                        timeMs = app.totalTimeMs,
                        icon = (app.icon as? androidx.compose.ui.graphics.ImageBitmap)?.asAndroidBitmap()
                    )
                }
            )
        } catch (e: Exception) {
            WidgetUsageData(hasError = true)
        }
    }
}

/** Dibuja el anillo de progreso como bitmap, porque Glance no soporta Canvas. */
object WidgetRingRenderer {

    fun render(progress: Float, color: Int, sizePx: Int = 320): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.12f
        val inset = stroke / 2f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            this.color = 0x22FFFFFF
        }
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                this.color = color
            }
            canvas.drawArc(rect, -90f, clamped * 360f, false, progressPaint)
        }
        return bitmap
    }
}
