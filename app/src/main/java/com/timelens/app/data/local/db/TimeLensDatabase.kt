package com.timelens.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.timelens.app.data.local.db.dao.AppDailyUsageDao
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.AppDailyUsageEntity
import com.timelens.app.data.local.db.entity.DailyUsageEntity

@Database(
    entities = [
        DailyUsageEntity::class,
        AppDailyUsageEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TimeLensDatabase : RoomDatabase() {
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun appDailyUsageDao(): AppDailyUsageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_usage` ADD COLUMN `productiveHour` INTEGER NOT NULL DEFAULT 9")
                db.execSQL("DELETE FROM `app_daily_usage` WHERE `id` NOT IN (SELECT MAX(`id`) FROM `app_daily_usage` GROUP BY `date`, `packageName`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_app_daily_usage_date_packageName` ON `app_daily_usage` (`date`, `packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_daily_usage_packageName` ON `app_daily_usage` (`packageName`)")
            }
        }
    }
}
