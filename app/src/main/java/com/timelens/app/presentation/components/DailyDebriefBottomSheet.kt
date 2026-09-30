package com.timelens.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelens.app.domain.model.*
import com.timelens.app.presentation.theme.*
import com.timelens.app.util.TimeFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDebriefBottomSheet(
    report: WellnessReport,
    summary: DaySummary,
    comparisonText: String,
    dailyGoalHours: Int,
    isNightReview: Boolean = false,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scoreColor = when {
        report.overallScore >= 80 -> NeonGreen
        report.overallScore >= 60 -> NeonCyan
        report.overallScore >= 40 -> NeonOrange
        else -> NeonRed
    }

    val statusIcon = when {
        report.overallScore >= 80 -> Icons.Outlined.CheckCircle
        report.overallScore >= 60 -> Icons.Outlined.Balance
        report.overallScore >= 40 -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.ErrorOutline
    }

    val dateFormatted = remember {
        java.time.LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
        ).replaceFirstChar { it.uppercase() }
    }

    val headerIcon = if (isNightReview) Icons.Outlined.Bedtime else Icons.Outlined.Insights
    val headerIconTint = if (isNightReview) NeonPurple else NeonCyan
    val headerTitle = if (isNightReview) "Tu Cierre del Día" else "Diagnóstico de Hábitos de Hoy"
    val headerSubtitle = if (isNightReview) "$dateFormatted • Revisión Nocturna" else dateFormatted

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Título, ícono contextual y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(headerIconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = headerSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Tarjeta Hero de Score
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.2.dp, scoreColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(scoreColor.copy(alpha = 0.25f), scoreColor.copy(alpha = 0.05f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${report.overallScore}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                            Text(
                                text = "pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scoreColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = scoreColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = report.overallStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor
                                )
                            }
                        }
                        val statusDesc = if (isNightReview) {
                            when {
                                report.overallScore >= 80 -> "¡Gran autocontrol! Cerrás la jornada con excelente balance."
                                report.overallScore >= 60 -> "Jornada estable con buen ritmo frente a tu objetivo diario."
                                report.overallScore >= 40 -> "Se observaron momentos de dispersión o fatiga acumulada hoy."
                                else -> "Día intenso con exceso de pantalla; momento ideal para desconectar."
                            }
                        } else {
                            when {
                                report.overallScore >= 80 -> "¡Gran autocontrol! Mantenés un uso consciente y equilibrado."
                                report.overallScore >= 60 -> "Ritmo de pantalla estable y balanceado frente a tu objetivo."
                                report.overallScore >= 40 -> "Atención a momentos de distracción o fatiga visual acumulada."
                                else -> "Uso intenso con exceso de pantalla; considerá hacer pausas activas."
                            }
                        }
                        Text(
                            text = statusDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 2. Resumen Cualitativo de Hábitos Clave
            Text(
                text = "Puntos destacados de tu jornada:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitHighlightItem(
                    icon = Icons.Outlined.Timer,
                    iconTint = NeonBlue,
                    title = "Tiempo total: ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)}",
                    subtitle = "$comparisonText • Objetivo: ${dailyGoalHours}h"
                )

                HabitHighlightItem(
                    icon = Icons.Outlined.LockOpen,
                    iconTint = if (summary.totalUnlocks > 60) NeonOrange else NeonGreen,
                    title = "${summary.totalUnlocks} desbloqueos",
                    subtitle = if (summary.totalUnlocks > 60) "Frecuencia alta (micro-interrupciones)" else "Nivel controlado de aperturas"
                )

                summary.longestSession?.let { longest ->
                    if (longest.durationMs >= 20 * 60 * 1000L) {
                        HabitHighlightItem(
                            icon = Icons.Outlined.AccessTime,
                            iconTint = if (longest.durationMs >= 40 * 60 * 1000L) NeonOrange else NeonPurple,
                            title = "Sesión más larga: ${TimeFormatter.formatMillisToShort(longest.durationMs)}",
                            subtitle = "En ${longest.appName}"
                        )
                    }
                }

                HabitHighlightItem(
                    icon = Icons.Outlined.CenterFocusStrong,
                    iconTint = NeonGreen,
                    title = "Momento más productivo: ${String.format(Locale.getDefault(), "%02d:00", summary.productiveHour)} hs",
                    subtitle = "Franja de menor uso de pantalla y mayor desconexión"
                )
            }

            // 3. Recomendaciones Personalizadas Dinámicas (Múltiples)
            Text(
                text = if (isNightReview) "Revisión nocturna y recomendaciones:" else "Recomendaciones personalizadas para hoy:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val displayInsights = report.allInsights.ifEmpty { listOf(report.primaryInsight) }.take(4)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayInsights.forEach { insight ->
                    RecommendationCard(insight = insight)
                }
            }

            // 4. Botones de Acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Compartir",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isNightReview) NeonPurple else NeonBlue)
                ) {
                    Text(
                        text = if (isNightReview) "A descansar" else "Entendido",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    insight: WellnessInsight,
    modifier: Modifier = Modifier
) {
    val accentColor = when (insight.level) {
        InsightLevel.EXCELLENT -> NeonGreen
        InsightLevel.MODERATE -> NeonCyan
        InsightLevel.ATTENTION -> NeonOrange
        InsightLevel.CRITICAL -> NeonRed
    }

    val icon: ImageVector = when (insight.category) {
        InsightCategory.SCREEN_TIME -> Icons.Outlined.Timer
        InsightCategory.UNLOCKS -> Icons.Outlined.LockOpen
        InsightCategory.SESSION_LENGTH -> Icons.Outlined.Visibility
        InsightCategory.APP_BALANCE -> Icons.Outlined.Apps
        InsightCategory.NIGHT_USAGE -> Icons.Outlined.Bedtime
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Tip accionable concreto
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.08f),
                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = insight.actionTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitHighlightItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
