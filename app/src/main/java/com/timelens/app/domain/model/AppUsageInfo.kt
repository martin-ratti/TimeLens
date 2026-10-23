package com.timelens.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Any? = null,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val longestSessionMs: Long,
    val category: AppCategory? = null
)

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    ENTERTAINMENT("Entretenimiento"),
    PRODUCTIVITY("Productividad"),
    COMMUNICATION("Comunicación"),
    GAMING("Juegos"),
    EDUCATION("Educación"),
    UTILITY("Utilidades"),
    OTHER("Otros")
}

@Immutable
data class AppDetailInfo(
    val packageName: String,
    val appName: String,
    val icon: Any? = null,
    val category: AppCategory = AppCategory.OTHER,
    val totalTimeMs: Long = 0L,
    val sessionCount: Int = 0,
    val longestSessionMs: Long = 0L,
    val avgSessionMs: Long = 0L,
    val hourlyUsageMs: Map<Int, Long> = emptyMap(), // 0..23 hours -> ms
    val weeklyHistory: List<Pair<String, Long>> = emptyList(), // Date -> totalTimeMs
    val totalDailyScreenTimeMs: Long = 0L,
    val peakHour: Int = 0,
    val weeklyAverageMs: Long = 0L,
    val lastUsedTimestampMs: Long = 0L
)
