package com.timelens.app.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.AppDailyUsageEntity
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DatabaseMigrationAndTransactionTest {

    @Test
    fun `MIGRATION_1_2 executes schema upgrade and deduplication statements in order`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        TimeLensDatabase.MIGRATION_1_2.migrate(db)

        verifyOrder {
            db.execSQL("ALTER TABLE `daily_usage` ADD COLUMN `productiveHour` INTEGER NOT NULL DEFAULT 9")
            db.execSQL("DELETE FROM `app_daily_usage` WHERE `id` NOT IN (SELECT MAX(`id`) FROM `app_daily_usage` GROUP BY `date`, `packageName`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_daily_usage_date_packageName` ON `app_daily_usage` (`date`, `packageName`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_daily_usage_packageName` ON `app_daily_usage` (`packageName`)")
        }
    }

    @Test
    fun `saveDayWithApps executes insert, delete by date, and deduplicated app insert atomically`() = runTest {
        val operations = mutableListOf<String>()
        var insertedApps: List<AppDailyUsageEntity> = emptyList()

        val fakeDao = object : DailyUsageDao {
            override suspend fun insertOrUpdate(dailyUsage: DailyUsageEntity) {
                operations.add("insertDaily:${dailyUsage.date}:${dailyUsage.productiveHour}")
            }

            override suspend fun insertAppUsages(apps: List<AppDailyUsageEntity>) {
                operations.add("insertApps:${apps.size}")
                insertedApps = apps
            }

            override suspend fun deleteAppUsagesByDate(date: String) {
                operations.add("deleteApps:$date")
            }

            override suspend fun getByDate(date: String): DailyUsageEntity? = null
            override suspend fun getLastDays(limit: Int): List<DailyUsageEntity> = emptyList()
            override fun observeWeeklyTrend(): Flow<List<DailyUsageEntity>> = emptyFlow()
            override suspend fun getAverageScreenTime(fromDate: String): Long? = null
            override suspend fun deleteOlderThan(beforeDate: String) {}
            override suspend fun deleteByDate(date: String) {}
            override suspend fun deleteArtificialRecords() {}
        }

        val daily = DailyUsageEntity(
            date = "2026-10-02",
            totalScreenTimeMs = 4_000_000L,
            totalUnlocks = 12,
            longestSessionMs = 1_000_000L,
            longestSessionApp = "com.whatsapp",
            topAppPackage = "com.whatsapp",
            topAppTimeMs = 2_500_000L,
            totalSessions = 6,
            peakHour = 18,
            productiveHour = 11
        )
        val duplicateApps = listOf(
            AppDailyUsageEntity(
                date = "2026-10-02",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                totalTimeMs = 2_500_000L,
                sessionCount = 4,
                longestSessionMs = 1_000_000L,
                category = "COMMUNICATION"
            ),
            AppDailyUsageEntity(
                date = "2026-10-02",
                packageName = "com.whatsapp",
                appName = "WhatsApp Duplicate",
                totalTimeMs = 2_500_000L,
                sessionCount = 4,
                longestSessionMs = 1_000_000L,
                category = "COMMUNICATION"
            )
        )

        fakeDao.saveDayWithApps(daily, duplicateApps)

        assertEquals(
            listOf("insertDaily:2026-10-02:11", "deleteApps:2026-10-02", "insertApps:1"),
            operations
        )
        assertEquals(1, insertedApps.size)
        assertEquals("com.whatsapp", insertedApps.first().packageName)
    }
}
