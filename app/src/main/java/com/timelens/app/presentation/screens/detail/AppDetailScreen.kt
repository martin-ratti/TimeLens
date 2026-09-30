package com.timelens.app.presentation.screens.detail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.domain.model.AppCategory
import com.timelens.app.domain.model.AppDetailInfo
import com.timelens.app.presentation.components.HourlyBarChart
import com.timelens.app.presentation.components.StatCard
import com.timelens.app.presentation.components.WeeklyBarChart
import com.timelens.app.presentation.theme.*
import com.timelens.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appLimitMinutes by viewModel.appLimitMinutes.collectAsStateWithLifecycle()
    var showLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de la aplicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AppDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NeonBlue
                    )
                }
                is AppDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            color = NeonRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadAppDetail() },
                            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                        ) {
                            Text("Reintentar", color = NeonBlue)
                        }
                    }
                }
                is AppDetailUiState.Success -> {
                    AppDetailContent(
                        detail = state.appDetail,
                        appLimitMinutes = appLimitMinutes,
                        onOpenLimitDialog = { showLimitDialog = true }
                    )

                    if (showLimitDialog) {
                        AppLimitDialog(
                            appName = state.appDetail.appName,
                            currentLimitMinutes = appLimitMinutes,
                            onSaveLimit = { viewModel.setAppLimit(it) },
                            onRemoveLimit = { viewModel.removeAppLimit() },
                            onDismiss = { showLimitDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailContent(
    detail: AppDetailInfo,
    appLimitMinutes: Int? = null,
    onOpenLimitDialog: () -> Unit = {}
) {
    val context = LocalContext.current

    val categoryColor = when (detail.category) {
        AppCategory.PRODUCTIVITY -> NeonGreen
        AppCategory.SOCIAL -> NeonPurple
        AppCategory.ENTERTAINMENT -> NeonOrange
        AppCategory.COMMUNICATION -> NeonCyan
        AppCategory.GAMING -> NeonRed
        AppCategory.EDUCATION -> NeonBlue
        else -> NeonBlue
    }

    val sharePercent = if (detail.totalDailyScreenTimeMs > 0L) {
        ((detail.totalTimeMs.toFloat() / detail.totalDailyScreenTimeMs) * 100).toInt()
    } else {
        0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = categoryColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (val icon = detail.icon) {
                                    is ImageBitmap -> {
                                        Image(
                                            bitmap = icon,
                                            contentDescription = "Logo de ${detail.appName}",
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                    is ImageVector -> {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Logo de ${detail.appName}",
                                            tint = categoryColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Outlined.Apps,
                                            contentDescription = null,
                                            tint = categoryColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = categoryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = detail.category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = categoryColor,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (sharePercent > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NeonCyan.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "$sharePercent% del día",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de acción rápida: Abrir app y Ajustes del sistema
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(detail.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    Toast.makeText(context, "No se puede abrir directamente", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = categoryColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Abrir app",
                                color = categoryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", detail.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajustes de app",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Límite Diario por App
        item {
            AppLimitCard(
                detail = detail,
                appLimitMinutes = appLimitMinutes,
                onOpenLimitDialog = onOpenLimitDialog
            )
        }

        // Grid de 6 KPIs Enriquecidos (3 filas de 2 tarjetas)
        item {
            val frequencyText = if (detail.sessionCount > 0 && detail.totalTimeMs > 0) {
                val minsPerSession = (detail.totalTimeMs / detail.sessionCount / 60000).coerceAtLeast(1)
                "Frecuencia: ~$minsPerSession min"
            } else {
                "sesiones registradas"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.AccessTime,
                    title = "Tiempo hoy",
                    value = TimeFormatter.formatMillisToShort(detail.totalTimeMs),
                    subtitle = if (sharePercent > 0) "$sharePercent% del tiempo total" else "Consumo de hoy",
                    accentColor = NeonBlue,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.TouchApp,
                    title = "Aperturas",
                    value = "${detail.sessionCount}",
                    subtitle = frequencyText,
                    accentColor = NeonOrange,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Timer,
                    title = "Sesión máx.",
                    value = TimeFormatter.formatMillisToShort(detail.longestSessionMs),
                    subtitle = "Récord continuo hoy",
                    accentColor = NeonPurple,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Equalizer,
                    title = "Promedio/sesión",
                    value = TimeFormatter.formatMillisToShort(detail.avgSessionMs),
                    subtitle = "Por cada ingreso",
                    accentColor = NeonGreen,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val diffPercent = if (detail.weeklyAverageMs > 0L) {
                (((detail.totalTimeMs - detail.weeklyAverageMs).toFloat() / detail.weeklyAverageMs) * 100).toInt()
            } else {
                0
            }

            val trendText = when {
                detail.weeklyAverageMs == 0L -> "Sin histórico"
                diffPercent > 0 -> "+$diffPercent% vs prom."
                diffPercent < 0 -> "$diffPercent% vs prom."
                else -> "Igual al prom."
            }

            val trendColor = when {
                detail.weeklyAverageMs == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                diffPercent > 10 -> NeonOrange
                diffPercent < -10 -> NeonGreen
                else -> NeonBlue
            }

            val trendIcon = when {
                diffPercent > 0 -> Icons.AutoMirrored.Outlined.TrendingUp
                diffPercent < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
                else -> Icons.AutoMirrored.Outlined.TrendingFlat
            }

            val peakHourFormatted = String.format("%02d:00 hs", detail.peakHour)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.Schedule,
                    title = "Horario pico",
                    value = peakHourFormatted,
                    subtitle = "Mayor concentración",
                    accentColor = NeonCyan,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = trendIcon,
                    title = "Tendencia semanal",
                    value = trendText,
                    subtitle = if (detail.weeklyAverageMs > 0L) "Prom: ${TimeFormatter.formatMillisToShort(detail.weeklyAverageMs)}" else "Primeros registros",
                    accentColor = trendColor,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Consejo de Bienestar Contextual para esta App
        item {
            AppWellnessInsightCard(detail = detail)
        }

        // Hourly Usage Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Actividad por hora (Hoy)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (detail.hourlyUsageMs.any { it.value > 0 }) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Pico: ${String.format("%02d:00", detail.peakHour)} hs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HourlyBarChart(
                        hourlyUsageMs = detail.hourlyUsageMs,
                        barColor = NeonBlue,
                        peakHour = detail.peakHour,
                        peakColor = NeonCyan
                    )
                }
            }
        }

        // 7-day Historical Trend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Historial reciente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (detail.weeklyAverageMs > 0L) {
                            Text(
                                text = "Prom: ${TimeFormatter.formatMillisToShort(detail.weeklyAverageMs)}/día",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (detail.weeklyHistory.isNotEmpty()) {
                        WeeklyBarChart(
                            days = detail.weeklyHistory.map { it.first.takeLast(5) },
                            valuesMs = detail.weeklyHistory.map { it.second },
                            barColor = NeonPurple
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no hay días previos guardados para esta app",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitCard(
    detail: AppDetailInfo,
    appLimitMinutes: Int?,
    onOpenLimitDialog: () -> Unit
) {
    val usedMinutes = (detail.totalTimeMs / (60 * 1000L)).toInt()
    val hasLimit = appLimitMinutes != null && appLimitMinutes > 0
    val isExceeded = hasLimit && usedMinutes >= (appLimitMinutes ?: 0)
    val isNearLimit = hasLimit && !isExceeded && usedMinutes >= ((appLimitMinutes ?: 0) * 0.8)

    val accentColor = when {
        isExceeded -> NeonRed
        isNearLimit -> NeonOrange
        hasLimit -> NeonCyan
        else -> NeonBlue
    }

    val progress = if (hasLimit) {
        (usedMinutes.toFloat() / appLimitMinutes!!).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "appLimitProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isExceeded -> Icons.Outlined.WarningAmber
                                isNearLimit -> Icons.Outlined.HourglassTop
                                else -> Icons.Outlined.Timer
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Límite Diario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val remainingMins = if (hasLimit) {
                            appLimitMinutes!! - usedMinutes
                        } else {
                            0
                        }
                        Text(
                            text = when {
                                !hasLimit -> "Sin límite configurado"
                                isExceeded -> "Superado por ${usedMinutes - (appLimitMinutes ?: 0)} min"
                                isNearLimit -> "Te quedan solo $remainingMins min (80% alcanzado)"
                                else -> "Te quedan $remainingMins min para hoy"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExceeded) NeonRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenLimitDialog,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text(
                        text = if (hasLimit) "Modificar" else "Definir",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasLimit) {
                val percentage = (usedMinutes.toFloat() / appLimitMinutes!! * 100).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${usedMinutes}m de ${appLimitMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppWellnessInsightCard(detail: AppDetailInfo) {
    val insight = remember(detail) {
        when {
            detail.totalTimeMs > 2 * 60 * 60 * 1000L -> {
                Triple(
                    Icons.Outlined.Lightbulb,
                    NeonOrange,
                    "Uso elevado hoy (${TimeFormatter.formatMillisToShort(detail.totalTimeMs)}). Establecer un límite diario te ayudará a liberar tiempo para otras actividades."
                )
            }
            detail.longestSessionMs > 45 * 60 * 1000L -> {
                Triple(
                    Icons.Outlined.Visibility,
                    NeonPurple,
                    "Sesión continua prolongada de ${TimeFormatter.formatMillisToShort(detail.longestSessionMs)}. Aplicá la regla 20-20-20: cada 20 minutos mirá a lo lejos 20 segundos para relajar la vista."
                )
            }
            detail.peakHour >= 22 || detail.peakHour < 5 -> {
                Triple(
                    Icons.Outlined.Bedtime,
                    NeonPurple,
                    "Tu mayor actividad en esta app ocurre de noche (${String.format("%02d:00", detail.peakHour)} hs). Reducir el uso antes de dormir mejora el descanso profundo."
                )
            }
            detail.sessionCount >= 15 -> {
                Triple(
                    Icons.Outlined.NotificationsPaused,
                    NeonBlue,
                    "Aperturas muy frecuentes (${detail.sessionCount} veces). Si entrás por impulso, silenciar sus notificaciones puede reducir las interrupciones."
                )
            }
            detail.category == AppCategory.PRODUCTIVITY -> {
                Triple(
                    Icons.Outlined.CenterFocusStrong,
                    NeonGreen,
                    "Herramienta clave de productividad. Mantenés un ritmo de trabajo enfocado y eficiente."
                )
            }
            else -> {
                Triple(
                    Icons.Outlined.CheckCircle,
                    NeonGreen,
                    "Patrón de uso equilibrado y bajo control en esta aplicación."
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, insight.second.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(insight.second.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = insight.first,
                    contentDescription = null,
                    tint = insight.second,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Consejo de Bienestar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = insight.second
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.third,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppLimitDialog(
    appName: String,
    currentLimitMinutes: Int?,
    onSaveLimit: (Int) -> Unit,
    onRemoveLimit: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(15, 30, 45, 60, 90, 120, 180)
    var selectedMinutes by remember { mutableStateOf(currentLimitMinutes ?: 45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = NeonCyan)
                Text("Límite diario: $appName", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Te avisaremos cuando alcances el 80% y cuando superes tu tiempo límite establecido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Duración máxima por día:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { minutes ->
                        val isSelected = selectedMinutes == minutes
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = minutes },
                            label = {
                                Text(
                                    text = if (minutes >= 60) {
                                        val h = minutes / 60
                                        val m = minutes % 60
                                        if (m == 0) "${h}h" else "${h}h ${m}m"
                                    } else {
                                        "${minutes}m"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = NeonCyan,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveLimit(selectedMinutes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Guardar", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentLimitMinutes != null) {
                    TextButton(
                        onClick = {
                            onRemoveLimit()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonRed)
                    ) {
                        Text("Quitar límite")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
