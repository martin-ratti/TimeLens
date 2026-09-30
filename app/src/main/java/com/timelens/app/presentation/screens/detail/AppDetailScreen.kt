package com.timelens.app.presentation.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
                    Text(
                        text = "Error: ${state.message}",
                        color = NeonRed,
                        modifier = Modifier.align(Alignment.Center)
                    )
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // App Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = NeonBlue.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (detail.icon != null) {
                                AsyncImage(
                                    model = detail.icon,
                                    contentDescription = detail.appName,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Apps,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detail.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = detail.category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonPurple,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

        // 4 KPI Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.AccessTime,
                    title = "Tiempo hoy",
                    value = TimeFormatter.formatMillisToShort(detail.totalTimeMs),
                    accentColor = NeonBlue,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.TouchApp,
                    title = "Aperturas",
                    value = "${detail.sessionCount}",
                    subtitle = "sesiones",
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
                    accentColor = NeonPurple,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Equalizer,
                    title = "Promedio/sesión",
                    value = TimeFormatter.formatMillisToShort(detail.avgSessionMs),
                    accentColor = NeonGreen,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
            }
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

                    Spacer(modifier = Modifier.height(12.dp))

                    HourlyBarChart(
                        hourlyUsageMs = detail.hourlyUsageMs,
                        barColor = NeonBlue
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
                            text = "Historial reciente (Últimos días)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
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
                        Text(
                            text = when {
                                !hasLimit -> "Sin límite configurado"
                                isExceeded -> "⚠️ Superado por ${usedMinutes - (appLimitMinutes ?: 0)} min"
                                isNearLimit -> "⏳ Al 80% de tu meta máxima"
                                else -> "Meta activa para hoy"
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

            if (hasLimit && appLimitMinutes != null && appLimitMinutes > 0) {
                val progress = (usedMinutes.toFloat() / appLimitMinutes).coerceIn(0f, 1f)
                val percentage = (usedMinutes.toFloat() / appLimitMinutes * 100).toInt()

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
                        progress = { progress },
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
