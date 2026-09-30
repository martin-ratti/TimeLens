package com.timelens.app.presentation.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.presentation.components.WeeklyBarChart
import com.timelens.app.presentation.theme.NeonBlue
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Tendencias",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonPurple,
                fontWeight = FontWeight.Bold
            )
        }

        when (val state = uiState) {
            is HistoryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = NeonPurple)
                }
            }
            is HistoryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error al cargar historial: ${state.message}", color = NeonOrange)
                }
            }
            is HistoryUiState.Success -> {
                val data = state.weeklyData
                if (data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay datos históricos aún.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    HistoryContent(data)
                }
            }
        }
    }
}

@Composable
fun HistoryContent(weeklyData: List<DaySummary>) {
    val dayFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale("es", "ES"))
    }
    val fullDateFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", java.util.Locale("es", "ES"))
    }

    val days = remember(weeklyData) {
        weeklyData.map { it.date.format(dayFormatter).replaceFirstChar { char -> char.uppercase() } }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total semanal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TimeFormatter.formatMillisToShort(totalWeeklyMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Promedio diario",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TimeFormatter.formatMillisToShort(avgTimeMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Desbloqueos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalUnlocks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonOrange
                        )
                    }
                }
            }
        }

        // Custom Bar Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Actividad (Últimos ${weeklyData.size} días)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    WeeklyBarChart(
                        days = days,
                        valuesMs = timesMs,
                        barColor = NeonPurple
                    )
                }
            }
        }

        // Best / Worst Day Highlights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Menor uso",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bestDay?.date?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "-",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bestDay?.let { TimeFormatter.formatMillisToShort(it.totalScreenTimeMs) } ?: "0m",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mayor uso",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = worstDay?.date?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "-",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = worstDay?.let { TimeFormatter.formatMillisToShort(it.totalScreenTimeMs) } ?: "0m",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section Title: Desglose por día
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
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
        }

        // Daily Breakdown Items (Most recent first)
        items(
            items = weeklyData.reversed(),
            key = { it.date.toString() }
        ) { day ->
            val fraction = (day.totalScreenTimeMs.toFloat() / maxDailyTimeMs).coerceIn(0f, 1f)
            val topApp = day.topApps.firstOrNull()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = day.date.format(fullDateFormatter).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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

                        Text(
                            text = TimeFormatter.formatMillisToShort(day.totalScreenTimeMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp)),
                        color = NeonPurple,
                        trackColor = NeonPurple.copy(alpha = 0.15f)
                    )
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
