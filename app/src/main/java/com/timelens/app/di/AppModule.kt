package com.timelens.app.di

import android.content.Context
import androidx.room.Room
import com.timelens.app.data.local.db.TimeLensDatabase
import com.timelens.app.data.local.db.dao.AppDailyUsageDao
import com.timelens.app.data.local.db.dao.DailyUsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TimeLensDatabase {
        return Room.databaseBuilder(
            context,
            TimeLensDatabase::class.java,
            "timelens_db"
        )
            .addMigrations(TimeLensDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideDailyUsageDao(db: TimeLensDatabase): DailyUsageDao {
        return db.dailyUsageDao()
    }

    @Provides
    fun provideAppDailyUsageDao(db: TimeLensDatabase): AppDailyUsageDao {
        return db.appDailyUsageDao()
    }
}
