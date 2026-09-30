package com.timelens.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dailyUsage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getByDate(date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :limit")
    suspend fun getLastDays(limit: Int = 7): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT 7")
    fun observeWeeklyTrend(): Flow<List<DailyUsageEntity>>

    @Query("SELECT AVG(totalScreenTimeMs) FROM daily_usage WHERE date >= :fromDate")
    suspend fun getAverageScreenTime(fromDate: String): Long?

    @Query("DELETE FROM daily_usage WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)

    @Query("DELETE FROM daily_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM daily_usage WHERE longestSessionMs = 0 AND totalScreenTimeMs > 0")
    suspend fun deleteArtificialRecords()
}
