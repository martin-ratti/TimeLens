package com.timelens.app.data.repository

import com.timelens.app.data.local.db.dao.AppDailyUsageDao
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.AppDailyUsageEntity
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import com.timelens.app.data.local.usage.UsageDataSource
import com.timelens.app.domain.model.AppCategory
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.Session
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class UsageRepositoryImplTest {

    private lateinit var dataSource: UsageDataSource
    private lateinit var dailyUsageDao: DailyUsageDao
    private lateinit var appDailyUsageDao: AppDailyUsageDao
    private lateinit var repository: UsageRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk(relaxed = true)
        dailyUsageDao = mockk(relaxed = true)
        appDailyUsageDao = mockk(relaxed = true)

        repository = UsageRepositoryImpl(
            dataSource = dataSource,
            dailyUsageDao = dailyUsageDao,
            appDailyUsageDao = appDailyUsageDao
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `hasUsagePermission delegates directly to dataSource`() {
        every { dataSource.hasUsagePermission() } returns true
        assertTrue(repository.hasUsagePermission())

        every { dataSource.hasUsagePermission() } returns false
        assertFalse(repository.hasUsagePermission())
    }

    @Test
    fun `getDaySummary returns null when entity not found in db`() = runTest {
        coEvery { dailyUsageDao.getByDate(any()) } returns null

        val result = repository.getDaySummary(LocalDate.of(2026, 9, 30))

        assertNull(result)
    }

    @Test
    fun `getDaySummary maps entity and app details correctly when found`() = runTest {
        val date = LocalDate.of(2026, 9, 30)
        val entity = DailyUsageEntity(
            date = "2026-09-30",
            totalScreenTimeMs = 3600000L,
            totalUnlocks = 20,
            longestSessionMs = 1800000L,
            longestSessionApp = "com.app.one",
            topAppPackage = "com.app.one",
            topAppTimeMs = 3600000L,
            totalSessions = 5,
            peakHour = 14
        )
        val appEntity = AppDailyUsageEntity(
            date = "2026-09-30",
            packageName = "com.app.one",
            appName = "App One",
            totalTimeMs = 3600000L,
            sessionCount = 5,
            longestSessionMs = 1800000L,
            category = "PRODUCTIVITY"
        )

        coEvery { dailyUsageDao.getByDate("2026-09-30") } returns entity
        coEvery { appDailyUsageDao.getByDate("2026-09-30") } returns listOf(appEntity)
        every { dataSource.getAppName("com.app.one") } returns "App One"

        val result = repository.getDaySummary(date)

        assertNotNull(result)
        assertEquals(date, result?.date)
        assertEquals(3600000L, result?.totalScreenTimeMs)
        assertEquals(20, result?.totalUnlocks)
        assertEquals(1, result?.topApps?.size)
        assertEquals("App One", result?.topApps?.first()?.appName)
    }

    @Test
    fun `saveDaySummary inserts entity and app usage records into DAOs`() = runTest {
        val summary = DaySummary(
            date = LocalDate.of(2026, 9, 30),
            totalScreenTimeMs = 5000000L,
            totalUnlocks = 15,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 12,
            productiveHour = 9,
            totalSessions = 10
        )

        repository.saveDaySummary(summary)

        coVerify { dailyUsageDao.insertOrUpdate(any()) }
        coVerify { appDailyUsageDao.insertAll(any()) }
    }

    @Test
    fun `getSessions returns emptyList`() = runTest {
        val sessions = repository.getSessions("com.app.one", LocalDate.now())
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `getWeeklyTrend returns only real existing days and does not invent fake days`() = runTest {
        // Given that db has only today, and android has no historical events
        val today = LocalDate.now()
        val todayEntity = DailyUsageEntity(
            date = today.toString(),
            totalScreenTimeMs = 3600000L,
            totalUnlocks = 25,
            longestSessionMs = 1200000L,
            longestSessionApp = "com.app.one",
            topAppPackage = "com.app.one",
            topAppTimeMs = 3600000L,
            totalSessions = 4,
            peakHour = 15
        )

        coEvery { dailyUsageDao.getByDate(today.toString()) } returns todayEntity
        // For past days, db returns null
        coEvery { dailyUsageDao.getByDate(neq(today.toString())) } returns null
        // Android has NO past events
        every { dataSource.getEventsForRange(any(), any()) } returns emptyList()
        // db getLastDays returns only today
        coEvery { dailyUsageDao.getLastDays(7) } returns listOf(todayEntity)
        coEvery { appDailyUsageDao.getByDate(today.toString()) } returns emptyList()

        val trend = repository.getWeeklyTrend()

        assertEquals(1, trend.size)
        assertEquals(today, trend[0].date)
        assertEquals(25, trend[0].totalUnlocks)
        assertEquals(3600000L, trend[0].totalScreenTimeMs)
        // Verify no fake inserts occurred for past days
        coVerify(exactly = 0) { dailyUsageDao.insertOrUpdate(match { it.date != today.toString() }) }
    }

    @Test
    fun `getWeeklyTrend purges artificial records and old records automatically`() = runTest {
        every { dataSource.getEventsForRange(any(), any()) } returns emptyList()
        coEvery { dailyUsageDao.getLastDays(7) } returns emptyList()

        repository.getWeeklyTrend()

        // Verify that the artificial and orphaned records were purged
        coVerify { dailyUsageDao.deleteArtificialRecords() }
        coVerify { appDailyUsageDao.deleteOrphanedRecords() }
        coVerify { dailyUsageDao.deleteOlderThan(any()) }
        coVerify { appDailyUsageDao.deleteOlderThan(any()) }
    }
}
