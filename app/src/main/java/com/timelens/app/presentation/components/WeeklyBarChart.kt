package com.timelens.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timelens.app.presentation.theme.NeonBlue
import com.timelens.app.presentation.theme.NeonPurple
import com.timelens.app.util.TimeFormatter

@Composable
fun WeeklyBarChart(
    days: List<String>,
    valuesMs: List<Long>,
    modifier: Modifier = Modifier,
    barColor: Color = NeonPurple,
    xLabels: List<String>? = null
) {
    if (days.isEmpty() || valuesMs.isEmpty() || days.size != valuesMs.size) {
        Box(modifier = modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text("Sin datos suficientes para el gráfico", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxMs = remember(valuesMs) { valuesMs.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    var selectedIndex by remember(days) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    val dense = days.size > 7
    val barWidth = when {
        days.size > 14 -> 6.dp
        dense -> 14.dp
        else -> 26.dp
    }
    val labelStep = when {
        days.size > 20 -> 5
        days.size > 10 -> 2
        else -> 1
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val idx = selectedIndex
        if (idx != null && idx in days.indices) {
            val selectedDay = days[idx]
            val selectedMs = valuesMs[idx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = barColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, barColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$selectedDay • ${TimeFormatter.formatMillisToReadable(selectedMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        val normalBrush = remember(barColor) {
            Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.7f)))
        }
        val selectedBrush = remember(barColor) {
            Brush.verticalGradient(listOf(NeonBlue, barColor))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in days.indices) {
                val ms = valuesMs[i]
                val fraction = (ms.toFloat() / maxMs).coerceIn(0f, 1f)
                val isSelected = selectedIndex == i

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedIndex = if (selectedIndex == i) null else i
                        }
                ) {
                    val hours = ms / (1000 * 60 * 60)
                    val mins = (ms / (1000 * 60)) % 60
                    val topLabel = when {
                        hours > 0 && mins >= 30 -> "${hours}h+"
                        hours > 0 -> "${hours}h"
                        ms > 0 -> "${mins}m"
                        else -> ""
                    }
                    if (!dense && topLabel.isNotEmpty()) {
                        Text(
                            text = topLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(barWidth),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasHeight = size.height
                            val canvasWidth = size.width
                            val radius = if (dense) 8f else 14f

                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset.Zero,
                                size = Size(canvasWidth, canvasHeight),
                                cornerRadius = CornerRadius(radius, radius)
                            )

                            val barHeight = (canvasHeight * fraction).coerceAtLeast(if (ms > 0) 8f else 0f)
                            if (barHeight > 0f) {
                                drawRoundRect(
                                    brush = if (isSelected) selectedBrush else normalBrush,
                                    topLeft = Offset(0f, canvasHeight - barHeight),
                                    size = Size(canvasWidth, barHeight),
                                    cornerRadius = CornerRadius(radius, radius)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val labelText = xLabels?.getOrNull(i) ?: days[i].take(3)
                    val showLabel = !dense || isSelected || i % labelStep == 0
                    Text(
                        text = if (showLabel) labelText else "",
                        style = if (dense) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .wrapContentWidth(Alignment.CenterHorizontally, unbounded = true)
                            .height(16.dp)
                    )
                }
            }
        }
    }
}
