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
import com.timelens.app.presentation.theme.NeonPurple
import com.timelens.app.util.TimeFormatter

@Composable
fun WeeklyBarChart(
    days: List<String>,
    valuesMs: List<Long>,
    modifier: Modifier = Modifier,
    barColor: Color = NeonPurple
) {
    if (days.isEmpty() || valuesMs.isEmpty() || days.size != valuesMs.size) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Sin datos suficientes para el gráfico", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxMs = remember(valuesMs) { valuesMs.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex != null) {
            val selectedDay = days[selectedIndex!!]
            val selectedMs = valuesMs[selectedIndex!!]
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
                        text = "$selectedDay — ${TimeFormatter.formatMillisToReadable(selectedMs)}",
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
                .height(200.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in days.indices) {
                val ms = valuesMs[i]
                val heightFraction = (ms.toFloat() / maxMs).coerceIn(0f, 1f)
                val isSelected = selectedIndex == i

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedIndex = if (selectedIndex == i) null else i
                        }
                ) {
                // Formatting the time for the tooltip/label above bar
                val hours = ms / (1000 * 60 * 60)
                if (hours > 0) {
                    Text(
                        text = "${hours}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else if (ms > 0) {
                    Text(
                        text = "<1h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasHeight = size.height
                        val canvasWidth = size.width
                        
                        val barHeight = canvasHeight * heightFraction
                        
                        drawRoundRect(
                            color = if (isSelected) onSurfaceColor else barColor,
                            topLeft = Offset(0f, canvasHeight - barHeight),
                            size = Size(canvasWidth, barHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = days[i].take(3), // Ej: "Lun", "Mar"
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) barColor else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            }
        }
    }
}
}
