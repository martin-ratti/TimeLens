package com.timelens.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelens.app.presentation.theme.NeonBlue
import com.timelens.app.util.TimeFormatter

@Composable
fun HourlyBarChart(
    hourlyUsageMs: Map<Int, Long>,
    modifier: Modifier = Modifier,
    barColor: Color = NeonBlue
) {
    val maxMs = remember(hourlyUsageMs) {
        hourlyUsageMs.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    }

    var selectedHour by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedHour != null) {
            val selectedMs = hourlyUsageMs[selectedHour!!] ?: 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = barColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.getDefault(), "%02d:00", selectedHour)} — ${TimeFormatter.formatMillisToShort(selectedMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (hour in 0..23) {
                val ms = hourlyUsageMs[hour] ?: 0L
                val heightFraction = (ms.toFloat() / maxMs).coerceIn(0f, 1f)
                val isSelected = selectedHour == hour

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedHour = if (selectedHour == hour) null else hour
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(if (isSelected) 8.dp else 6.dp)
                    ) {
                        val canvasHeight = size.height
                        val canvasWidth = size.width
                        val barHeight = (canvasHeight * heightFraction).coerceAtLeast(if (ms > 0) 6f else 0f)

                        drawRoundRect(
                            color = if (ms > 0) (if (isSelected) onSurfaceColor else barColor) else Color.Transparent,
                            topLeft = Offset(0f, canvasHeight - barHeight),
                            size = Size(canvasWidth, barHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
