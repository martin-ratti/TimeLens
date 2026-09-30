package com.timelens.app.presentation.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.domain.model.AppCategory
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.presentation.components.*
import com.timelens.app.presentation.theme.*
import com.timelens.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = null,
                            tint = NeonBlue
                        )
                        Text(
                            text = "TimeLens",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ajustes"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
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
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NeonBlue
                    )
                }
                is HomeUiState.MissingPermission -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Se necesita permiso de acceso a uso para mostrar tus estadísticas.",
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                is HomeUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = NeonRed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        summary = state.summary,
                        comparisonText = state.comparisonText,
                        dailyGoalHours = state.dailyGoalHours,
                        onAppClick = onAppClick
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    summary: DaySummary,
    comparisonText: String,
    dailyGoalHours: Int = 6,
    onAppClick: (String) -> Unit = {}
) {
    val dailyGoalMs = dailyGoalHours * 60 * 60 * 1000L
    val progress = if (dailyGoalMs > 0L) {
        (summary.totalScreenTimeMs.toFloat() / dailyGoalMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    val categoryUsage = remember(summary.topApps) {
        summary.topApps
            .groupBy { it.category ?: AppCategory.OTHER }
            .mapValues { entry -> entry.value.sumOf { it.totalTimeMs } }
            .toList()
            .sortedByDescending { it.second }
    }

    var selectedCategory by rememberSaveable { mutableStateOf<AppCategory?>(null) }

    val filteredApps = remember(summary.topApps, selectedCategory) {
        if (selectedCategory == null) {
            summary.topApps
        } else {
            summary.topApps.filter { (it.category ?: AppCategory.OTHER) == selectedCategory }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            CircularProgressCard(
                totalTimeText = TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs),
                progress = progress,
                comparisonText = comparisonText,
                goalHours = dailyGoalHours
            )
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
                    subtitle = "hoy",
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Timer,
                    title = "Sesión máx.",
                    value = summary.longestSession?.durationMs?.let { TimeFormatter.formatMillisToShort(it) } ?: "0m",
                    subtitle = summary.longestSession?.appName ?: "Sin uso",
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Outlined.Schedule,
                    title = "Horario pico",
                    value = TimeFormatter.getTimeOfDayLabel(summary.peakHour),
                    subtitle = String.format(java.util.Locale.getDefault(), "%02d:00", summary.peakHour),
                    accentColor = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Categorías de uso
        if (categoryUsage.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Category,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip "Todas"
                    item(key = "all_categories") {
                        val isSelected = selectedCategory == null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, NeonBlue) else null,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable { selectedCategory = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Todas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) NeonBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${summary.topApps.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) NeonBlue.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    items(items = categoryUsage, key = { it.first.name }) { (category, timeMs) ->
                        val isSelected = selectedCategory == category
                        val catColor = when (category) {
                            AppCategory.SOCIAL -> NeonPurple
                            AppCategory.ENTERTAINMENT -> NeonOrange
                            AppCategory.PRODUCTIVITY -> NeonBlue
                            AppCategory.GAMING -> NeonGreen
                            AppCategory.COMMUNICATION -> NeonCyan
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, catColor) else null,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedCategory = if (selectedCategory == category) null else category
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = TimeFormatter.formatMillisToShort(timeMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) catColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (selectedCategory != null) "Apps: ${selectedCategory?.displayName}" else "Apps más usadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (filteredApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay aplicaciones en esta categoría hoy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(items = filteredApps, key = { it.packageName }) { appInfo ->
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
    }
}
