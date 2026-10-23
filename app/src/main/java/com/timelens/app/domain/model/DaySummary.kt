package com.timelens.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class DaySummary(
    val date: LocalDate,
    val totalScreenTimeMs: Long,
    val totalUnlocks: Int,
    val topApps: List<AppUsageInfo>,
    val longestSession: Session?,
    val peakHour: Int,
    val productiveHour: Int = 9,
    val totalSessions: Int
)
