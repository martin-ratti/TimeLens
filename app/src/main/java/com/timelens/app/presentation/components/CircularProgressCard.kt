package com.timelens.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelens.app.presentation.theme.*
import com.timelens.app.util.TimeFormatter

@Composable
fun CircularProgressCard(
    totalTimeText: String,
    progress: Float, // 0f to 1f
    comparisonText: String,
    goalHours: Int = 6,
    usedMillis: Long = (progress * goalHours * 3600000L).toLong(),
    modifier: Modifier = Modifier
) {
    val goalMs = goalHours * 3600000L
    val rawRatio = if (goalMs > 0L) usedMillis.toFloat() / goalMs else progress
    val isExceeded = goalMs > 0L && usedMillis >= goalMs
    val isNearLimit = !isExceeded && rawRatio >= 0.8f

    val statusColor = when {
        isExceeded -> NeonRed
        isNearLimit -> NeonOrange
        else -> NeonGreen
    }

    val timeTextColor = when {
        isExceeded -> NeonRed
        isNearLimit -> NeonOrange
        else -> NeonBlue
    }

    val clampedProgress = progress.coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isExceeded -> NeonRed.copy(alpha = 0.45f)
                isNearLimit -> NeonOrange.copy(alpha = 0.35f)
                else -> NeonBlue.copy(alpha = 0.22f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = timeTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tiempo de pantalla hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val gradientBrush = remember(isExceeded, isNearLimit) {
                when {
                    isExceeded -> Brush.sweepGradient(listOf(NeonOrange, NeonRed, NeonRed, NeonOrange))
                    isNearLimit -> Brush.sweepGradient(listOf(NeonBlue, NeonOrange, NeonRed, NeonBlue))
                    else -> Brush.sweepGradient(listOf(NeonCyan, NeonBlue, NeonPurple, NeonCyan))
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(204.dp)
            ) {
                Canvas(modifier = Modifier.size(204.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background arc
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc with gradient
                    drawArc(
                        brush = gradientBrush,
                        startAngle = -90f,
                        sweepAngle = clampedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalTimeText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = timeTextColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Límite máx: ${goalHours}h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    val percentUsed = (rawRatio * 100).toInt()
                    Text(
                        text = "$percentUsed% del límite",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badge de estado frente al límite diario (menor al límite = positivo, superarlo = alerta)
            val statusIcon = when {
                isExceeded -> Icons.Outlined.WarningAmber
                isNearLimit -> Icons.Outlined.HourglassTop
                else -> Icons.Outlined.CheckCircle
            }
            val statusLabel = when {
                isExceeded -> {
                    val extraText = TimeFormatter.formatMillisToShort((usedMillis - goalMs).coerceAtLeast(0L))
                    "Límite diario superado (+$extraText)"
                }
                isNearLimit -> {
                    val remText = TimeFormatter.formatMillisToShort((goalMs - usedMillis).coerceAtLeast(0L))
                    "Cerca del límite • Quedan $remText"
                }
                else -> {
                    val remText = TimeFormatter.formatMillisToShort((goalMs - usedMillis).coerceAtLeast(0L))
                    "Dentro del límite • $remText disponibles"
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            if (comparisonText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val comparisonColor = when {
                    comparisonText.startsWith("-") -> NeonGreen
                    comparisonText.startsWith("+") -> NeonOrange
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = comparisonText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = comparisonColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun CircularProgressCardPreview() {
    TimeLensTheme(darkTheme = true) {
        CircularProgressCard(
            totalTimeText = "4h 32m",
            progress = 0.75f,
            comparisonText = "-15% vs ayer \uD83C\uDF89"
        )
    }
}
