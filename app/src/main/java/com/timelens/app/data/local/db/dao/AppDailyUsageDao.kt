package com.timelens.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timelens.app.data.local.db.entity.AppDailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppDailyUsageEntity>)

    @Query("SELECT * FROM app_daily_usage WHERE date = :date ORDER BY totalTimeMs DESC")
    suspend fun getByDate(date: String): List<AppDailyUsageEntity>

    @Query("SELECT * FROM app_daily_usage WHERE date = :date ORDER BY totalTimeMs DESC LIMIT :limit")
    suspend fun getTopApps(date: String, limit: Int = 5): List<AppDailyUsageEntity>

    @Query("SELECT * FROM app_daily_usage WHERE packageName = :packageName ORDER BY date DESC LIMIT :limit")
    suspend fun getAppHistory(packageName: String, limit: Int = 7): List<AppDailyUsageEntity>

    @Query("SELECT * FROM app_daily_usage WHERE date = :date ORDER BY totalTimeMs DESC")
    fun observeByDate(date: String): Flow<List<AppDailyUsageEntity>>

    @Query("DELETE FROM app_daily_usage WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)

    @Query("DELETE FROM app_daily_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM app_daily_usage WHERE date NOT IN (SELECT date FROM daily_usage)")
    suspend fun deleteOrphanedRecords()
}
