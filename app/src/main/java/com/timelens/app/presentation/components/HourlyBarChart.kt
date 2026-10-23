package com.timelens.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelens.app.presentation.theme.NeonBlue
import com.timelens.app.presentation.theme.NeonCyan
import com.timelens.app.util.TimeFormatter

@Composable
fun HourlyBarChart(
    hourlyUsageMs: Map<Int, Long>,
    modifier: Modifier = Modifier,
    barColor: Color = NeonBlue,
    peakHour: Int? = null,
    peakColor: Color = NeonCyan
) {
    val maxMs = remember(hourlyUsageMs) {
        hourlyUsageMs.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    }

    var selectedHour by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        val h = selectedHour
        if (h != null) {
            val selectedMs = hourlyUsageMs[h] ?: 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = barColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, barColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.getDefault(), "%02d:00", h)} hs — ${TimeFormatter.formatMillisToShort(selectedMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

        // Single Canvas for all 24 hours: 1 node instead of 72 nodes!
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 8.dp, bottom = 6.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width / 24f
                        val tappedHour = (offset.x / slotWidth).toInt().coerceIn(0, 23)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedHour = if (selectedHour == tappedHour) null else tappedHour
                    }
                }
        ) {
            val canvasHeight = size.height
            val slotWidth = size.width / 24f
            val regularBarWidth = 6.dp.toPx()
            val highlightBarWidth = 8.dp.toPx()

            for (hour in 0..23) {
                val ms = hourlyUsageMs[hour] ?: 0L
                val fraction = (ms.toFloat() / maxMs).coerceIn(0f, 1f)
                val isSelected = selectedHour == hour
                val isPeak = peakHour == hour && ms > 0
                val barW = if (isSelected || isPeak) highlightBarWidth else regularBarWidth
                val barX = (hour * slotWidth) + (slotWidth - barW) / 2f

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(barX, 0f),
                    size = Size(barW, canvasHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                if (ms > 0L) {
                    val barHeight = (canvasHeight * fraction).coerceAtLeast(6f)
                    val drawColor = when {
                        isSelected -> onSurfaceColor
                        isPeak -> peakColor
                        else -> barColor
                    }
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset(barX, canvasHeight - barHeight),
                        size = Size(barW, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        // Hour labels (0h, 6h, 12h, 18h, 23h)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "06:00", "12:00", "18:00", "23:00").forEach { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
