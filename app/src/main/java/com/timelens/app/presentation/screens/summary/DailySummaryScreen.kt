package com.timelens.app.presentation.screens.summary

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.domain.model.*
import com.timelens.app.presentation.components.AppUsageCard
import com.timelens.app.presentation.components.StatCard
import com.timelens.app.presentation.theme.*
import com.timelens.app.util.TimeFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    onNavigateBack: () -> Unit,
    onAppClick: (String) -> Unit = {},
    viewModel: DailySummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Resumen Diario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dateFormatted = remember {
                            java.time.LocalDate.now().format(
                                DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
                            ).replaceFirstChar { it.uppercase() }
                        }
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareSummary(context) }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Compartir Resumen",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DailySummaryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NeonBlue
                    )
                }
                is DailySummaryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                is DailySummaryUiState.Success -> {
                    DailySummaryContent(
                        state = state,
                        onAppClick = onAppClick,
                        onShare = { viewModel.shareSummary(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailySummaryContent(
    state: DailySummaryUiState.Success,
    onAppClick: (String) -> Unit,
    onShare: () -> Unit
) {
    val report = state.wellnessReport
    val summary = state.summary
    val goalMs = state.dailyGoalHours * 3600000L
    val progress = if (goalMs > 0) {
        (summary.totalScreenTimeMs.toFloat() / goalMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    val scoreColor = when {
        report.overallScore >= 80 -> NeonGreen
        report.overallScore >= 60 -> NeonCyan
        report.overallScore >= 40 -> NeonOrange
        else -> NeonRed
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // 1. Tarjeta Hero: Score de Bienestar y Diagnóstico General
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Badge circular de Score
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(scoreColor.copy(alpha = 0.25f), scoreColor.copy(alpha = 0.05f))
                                )
                            )
                            .padding(2.dp),
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

                    // Título y Diagnóstico
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scoreColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = report.overallStatus,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                        }
                        Text(
                            text = "Índice de Bienestar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                report.overallScore >= 80 -> "Excelente equilibrio entre vida y pantalla hoy."
                                report.overallScore >= 60 -> "Buen control general con pequeñas oportunidades de pausa."
                                report.overallScore >= 40 -> "Detectamos varios picos de distracción o fatiga visual."
                                else -> "Excediste tu meta habitual de horas frente a la pantalla."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Tarjeta de Tiempo Total vs Meta
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tiempo en Pantalla",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = state.comparisonText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonPurple
                            )
                        }
                    }

                    // Barra de progreso hacia la meta
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Objetivo: ${state.dailyGoalHours}h diarias",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (progress >= 1f) NeonRed else NeonBlue
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress >= 1f) NeonRed else NeonBlue,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        // 3. Sección de Diagnósticos y Recomendaciones
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Diagnóstico y Consejos de Hoy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        items(report.allInsights, key = { it.id }) { insight ->
            val insightAccent = when (insight.level) {
                InsightLevel.EXCELLENT -> NeonGreen
                InsightLevel.MODERATE -> NeonBlue
                InsightLevel.ATTENTION -> NeonOrange
                InsightLevel.CRITICAL -> NeonRed
            }

            val categoryIcon = when (insight.category) {
                InsightCategory.SCREEN_TIME -> Icons.Outlined.Timer
                InsightCategory.UNLOCKS -> Icons.Outlined.LockOpen
                InsightCategory.SESSION_LENGTH -> Icons.Outlined.AccessTime
                InsightCategory.NIGHT_USAGE -> Icons.Outlined.Bedtime
                InsightCategory.APP_BALANCE -> Icons.Outlined.Apps
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, insightAccent.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(insightAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = insightAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    // Caja con el Tip Accionable / Recomendación
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, insightAccent.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TipsAndUpdates,
                                contentDescription = null,
                                tint = insightAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Recomendación práctica:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = insightAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = insight.actionTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Métricas Clave de la Jornada (KPIs)
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Métricas Clave de Hoy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.LockOpen,
                    title = "Desbloqueos",
                    value = summary.totalUnlocks.toString(),
                    subtitle = "aperturas",
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.AccessTime,
                    title = "Sesión más larga",
                    value = summary.longestSession?.let { TimeFormatter.formatMillisToShort(it.durationMs) } ?: "0m",
                    subtitle = summary.longestSession?.appName ?: "Sin registro",
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Schedule,
                    title = "Horario pico",
                    value = String.format(Locale.getDefault(), "%02d:00", summary.peakHour),
                    subtitle = "mayor actividad",
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Repeat,
                    title = "Sesiones",
                    value = summary.totalSessions.toString(),
                    subtitle = "totales hoy",
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Aplicaciones Más Usadas del Día
        if (summary.topApps.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Aplicaciones del Día",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(summary.topApps, key = { it.packageName }) { appInfo ->
                val appProgress = if (summary.totalScreenTimeMs > 0L) {
                    (appInfo.totalTimeMs.toFloat() / summary.totalScreenTimeMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                AppUsageCard(
                    icon = appInfo.icon ?: Icons.Outlined.Apps,
                    appName = appInfo.appName,
                    usageTime = TimeFormatter.formatMillisToShort(appInfo.totalTimeMs),
                    progress = appProgress,
                    accentColor = NeonPurple,
                    sessionCount = appInfo.sessionCount,
                    category = appInfo.category,
                    onClick = { onAppClick(appInfo.packageName) }
                )
            }
        }

        // 6. Botón para Compartir Resumen
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlue.copy(alpha = 0.15f),
                    contentColor = NeonBlue
                ),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compartir Resumen de Hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
