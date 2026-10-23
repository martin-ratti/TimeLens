package com.timelens.app.presentation.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.presentation.components.StatCard
import com.timelens.app.presentation.components.WeeklyBarChart
import com.timelens.app.presentation.theme.NeonBlue
import com.timelens.app.presentation.theme.NeonCyan
import com.timelens.app.presentation.theme.NeonGreen
import com.timelens.app.presentation.theme.NeonOrange
import com.timelens.app.presentation.theme.NeonPurple
import com.timelens.app.presentation.theme.TimeLensTheme
import com.timelens.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val rangeDays by viewModel.rangeDays.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tendencias",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonPurple
                            )
                            Text(
                                text = "Evolución de los últimos 7, 14 o 30 días",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is HistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonPurple)
                    }
                }
                is HistoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error al cargar historial: ${state.message}",
                            color = NeonOrange,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                        ) {
                            Text("Reintentar", color = NeonPurple)
                        }
                    }
                }
                is HistoryUiState.Success -> {
                    val data = state.weeklyData
                    val pullRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refresh()
                        },
                        state = pullRefreshState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullRefreshState,
                                isRefreshing = isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                                color = NeonPurple
                            )
                        }
                    ) {
                        if (data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay datos históricos aún.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            HistoryContent(
                                weeklyData = data,
                                selectedRangeDays = rangeDays,
                                onRangeSelected = viewModel::setRange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryContent(
    weeklyData: List<DaySummary>,
    selectedRangeDays: Int = 7,
    onRangeSelected: (Int) -> Unit = {}
) {
    val dayFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale("es", "ES"))
    }
    val fullDateFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", java.util.Locale("es", "ES"))
    }
    val shortDateFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("d/M", java.util.Locale("es", "ES"))
    }

    val days = remember(weeklyData) {
        weeklyData.map { it.date.format(fullDateFormatter).replaceFirstChar { char -> char.uppercase() } }
    }
    val chartLabels = remember(weeklyData) {
        if (weeklyData.size > 10) {
            weeklyData.map { it.date.format(shortDateFormatter) }
        } else {
            weeklyData.map { it.date.format(dayFormatter).replaceFirstChar { char -> char.uppercase() }.take(3) }
        }
    }
    val timesMs = remember(weeklyData) {
        weeklyData.map { it.totalScreenTimeMs }
    }

    val totalWeeklyMs = remember(timesMs) { timesMs.sum() }
    val avgTimeMs = remember(timesMs) { if (weeklyData.isNotEmpty()) totalWeeklyMs / weeklyData.size else 0L }
    val totalUnlocks = remember(weeklyData) { weeklyData.sumOf { it.totalUnlocks } }

    val bestDay = remember(weeklyData) { weeklyData.minByOrNull { it.totalScreenTimeMs } }
    val worstDay = remember(weeklyData) { weeklyData.maxByOrNull { it.totalScreenTimeMs } }
    val maxDailyTimeMs = remember(timesMs) { timesMs.maxOrNull()?.coerceAtLeast(1L) ?: 1L }

    var expandedDateKey by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Selector de rango (7 / 14 / 30 días)
        item(key = "range_selector") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistoryViewModel.AVAILABLE_RANGES.forEach { range ->
                    FilterChip(
                        selected = selectedRangeDays == range,
                        onClick = { onRangeSelected(range) },
                        label = {
                            Text(
                                text = "$range días",
                                fontWeight = if (selectedRangeDays == range) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple.copy(alpha = 0.18f),
                            selectedLabelColor = NeonPurple
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedRangeDays == range,
                            selectedBorderColor = NeonPurple,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
        }

        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    icon = Icons.Outlined.DateRange,
                    title = "Total",
                    value = TimeFormatter.formatMillisToShort(totalWeeklyMs),
                    subtitle = if (weeklyData.size == 1) "1 día registrado" else "${weeklyData.size} días reg.",
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Speed,
                    title = "Promedio",
                    value = TimeFormatter.formatMillisToShort(avgTimeMs),
                    subtitle = "por día",
                    accentColor = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.LockOpen,
                    title = "Desbloqueos",
                    value = "$totalUnlocks",
                    subtitle = "total",
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Custom Bar Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (weeklyData.size == 1) "Actividad (1 día registrado)" else "Actividad (Últimos ${weeklyData.size} días)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tocá una barra",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    WeeklyBarChart(
                        days = days,
                        valuesMs = timesMs,
                        barColor = NeonPurple,
                        xLabels = chartLabels
                    )
                }
            }
        }

        // Best / Worst Day Highlights (shown when 2 or more days are recorded)
        if (weeklyData.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Outlined.EmojiEvents,
                        title = "Menor uso",
                        value = bestDay?.let { TimeFormatter.formatMillisToShort(it.totalScreenTimeMs) } ?: "0m",
                        subtitle = bestDay?.date?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "-",
                        accentColor = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Outlined.Warning,
                        title = "Mayor uso",
                        value = worstDay?.let { TimeFormatter.formatMillisToShort(it.totalScreenTimeMs) } ?: "0m",
                        subtitle = worstDay?.date?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "-",
                        accentColor = NeonOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section Title: Desglose por día
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Desglose por día",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Tocá un día para ver apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily Breakdown Items (Most recent first)
        items(
            items = weeklyData.reversed(),
            key = { it.date.toString() }
        ) { day ->
            val dateKey = day.date.toString()
            val isExpanded = expandedDateKey == dateKey
            val fraction = (day.totalScreenTimeMs.toFloat() / maxDailyTimeMs).coerceIn(0f, 1f)
            val topApp = day.topApps.firstOrNull()
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "chevronRotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        expandedDateKey = if (isExpanded) null else dateKey
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) NeonPurple.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = day.date.format(fullDateFormatter).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.totalUnlocks} desbloqueos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (topApp != null) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Top: ${topApp.appName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonPurple,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = TimeFormatter.formatMillisToShort(day.totalScreenTimeMs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = if (isExpanded) "Contraer" else "Expandir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonPurple,
                        trackColor = NeonPurple.copy(alpha = 0.14f)
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeonBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Pico: ${String.format(java.util.Locale.getDefault(), "%02d:00", day.peakHour)} hs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val maxSessionMs = day.longestSession?.durationMs ?: 0L
                                if (maxSessionMs > 0L) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NeonCyan.copy(alpha = 0.12f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Timer,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Sesión máx: ${TimeFormatter.formatMillisToShort(maxSessionMs)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (day.topApps.isNotEmpty()) {
                                Text(
                                    text = "Apps principales del día",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                day.topApps.take(4).forEachIndexed { idx, app ->
                                    val share = if (day.totalScreenTimeMs > 0L) {
                                        ((app.totalTimeMs.toFloat() / day.totalScreenTimeMs) * 100).toInt()
                                    } else 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonPurple
                                            )
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "(${app.sessionCount} ap.)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${TimeFormatter.formatMillisToShort(app.totalTimeMs)} ($share%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HistoryScreenPreview() {
    TimeLensTheme {
        HistoryScreen()
    }
}
