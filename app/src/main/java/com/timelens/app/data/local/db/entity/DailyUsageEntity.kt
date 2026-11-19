package com.timelens.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey
    val date: String,
    val totalScreenTimeMs: Long,
    val totalUnlocks: Int,
    val longestSessionMs: Long,
    val longestSessionApp: String,
    val topAppPackage: String,
    val topAppTimeMs: Long,
    val totalSessions: Int,
    val peakHour: Int,
    val productiveHour: Int = 9
)
