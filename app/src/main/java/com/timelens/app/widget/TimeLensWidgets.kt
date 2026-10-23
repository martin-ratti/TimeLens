package com.timelens.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.timelens.app.R
import com.timelens.app.presentation.MainActivity
import com.timelens.app.util.TimeFormatter

private val TextPrimary = ColorProvider(Color(0xFFF1F5F9))
private val TextSecondary = ColorProvider(Color(0xFF94A3B8))

private fun statusColorProvider(data: WidgetUsageData) = ColorProvider(Color(data.status.argb))

@Composable
private fun WidgetRing(data: WidgetUsageData, ringSize: Int) {
    val ringBitmap = WidgetRingRenderer.render(data.ratio, data.status.argb)
    Box(contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(ringBitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = TimeFormatter.formatMillisToShort(data.totalMs),
                style = TextStyle(
                    color = statusColorProvider(data),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = "de ${data.goalHours}h",
                style = TextStyle(color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun WidgetMessage(text: String) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun WidgetContainer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(openApp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Widget 2x2: anillo circular con el tiempo de hoy frente al límite diario. */
class TimeLensRingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent {
            WidgetContainer {
                when {
                    !data.hasPermission -> WidgetMessage("Abrí TimeLens y activá el permiso de uso")
                    data.hasError -> WidgetMessage("No se pudo cargar. Tocá para abrir")
                    else -> WidgetRing(data, ringSize = 100)
                }
            }
        }
    }
}

/** Widget 4x2: anillo + Top 3 apps + desbloqueos. */
class TimeLensSummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent {
            WidgetContainer {
                when {
                    !data.hasPermission -> WidgetMessage("Abrí TimeLens y activá el permiso de uso")
                    data.hasError -> WidgetMessage("No se pudo cargar. Tocá para abrir")
                    else -> SummaryContent(data)
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(data: WidgetUsageData) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetRing(data, ringSize = 92)
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Hoy  •  ${data.unlocks} desbloqueos",
                style = TextStyle(color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            if (data.topApps.isEmpty()) {
                Text(
                    text = "Sin uso registrado",
                    style = TextStyle(color = TextPrimary, fontSize = 12.sp)
                )
            } else {
                data.topApps.forEach { app ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                provider = ImageProvider(app.icon),
                                contentDescription = null,
                                modifier = GlanceModifier.size(18.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                        Text(
                            text = app.name,
                            maxLines = 1,
                            style = TextStyle(color = TextPrimary, fontSize = 12.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = TimeFormatter.formatMillisToShort(app.timeMs),
                            style = TextStyle(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class TimeLensRingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeLensRingWidget()
}

class TimeLensSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeLensSummaryWidget()
}
