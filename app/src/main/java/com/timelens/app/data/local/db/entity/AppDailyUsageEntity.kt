package com.timelens.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_daily_usage",
    indices = [
        Index(value = ["date", "packageName"], unique = true),
        Index(value = ["packageName"])
    ]
)
data class AppDailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val longestSessionMs: Long,
    val category: String?
)
