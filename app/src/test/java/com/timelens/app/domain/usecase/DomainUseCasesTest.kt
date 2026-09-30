package com.timelens.app.domain.usecase

import com.timelens.app.domain.model.AppCategory
import com.timelens.app.domain.model.AppUsageInfo
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.fakes.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DomainUseCasesTest {

    private lateinit var fakeRepository: FakeUsageRepository

    @Before
    fun setUp() {
        fakeRepository = FakeUsageRepository()
    }

    @Test
    fun `GetDailySummaryUseCase returns today summary from repository`() = runTest {
        val expected = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 4500000L,
            totalUnlocks = 33,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 18,
            productiveHour = 11,
            totalSessions = 14
        )
        fakeRepository.todaySummary = expected

        val useCase = GetDailySummaryUseCase(fakeRepository)
        val result = useCase()

        assertEquals(expected, result)
        assertEquals(4500000L, result.totalScreenTimeMs)
        assertEquals(33, result.totalUnlocks)
        assertEquals(11, result.productiveHour)
    }

    @Test
    fun `GetTopAppsUseCase respects limit and returns sorted apps`() = runTest {
        val apps = (1..10).map { i ->
            AppUsageInfo(
                packageName = "com.app.$i",
                appName = "App $i",
                totalTimeMs = i * 100000L,
                sessionCount = i,
                longestSessionMs = i * 50000L,
                category = AppCategory.PRODUCTIVITY
            )
        }.reversed()
        fakeRepository.topAppsList.addAll(apps)

        val useCase = GetTopAppsUseCase(fakeRepository)

        val top3 = useCase(limit = 3)
        assertEquals(3, top3.size)
        assertEquals("App 10", top3[0].appName)

        val defaultTop5 = useCase()
        assertEquals(5, defaultTop5.size)
    }

    @Test
    fun `GetWeeklyTrendUseCase returns weekly trend from repository`() = runTest {
        val weekly = (1..7).map { day ->
            DaySummary(
                date = LocalDate.now().minusDays(day.toLong()),
                totalScreenTimeMs = day * 3600000L,
                totalUnlocks = day * 10,
                topApps = emptyList(),
                longestSession = null,
                peakHour = 15,
                productiveHour = 9,
                totalSessions = day * 5
            )
        }
        fakeRepository.weeklyTrendList.addAll(weekly)

        val useCase = GetWeeklyTrendUseCase(fakeRepository)
        val result = useCase()

        assertEquals(7, result.size)
        assertEquals(7 * 3600000L, result.last().totalScreenTimeMs)
    }

    @Test
    fun `CheckUsagePermissionUseCase delegates to repository permission check`() {
        val useCase = CheckUsagePermissionUseCase(fakeRepository)

        fakeRepository.hasPermission = true
        assertTrue(useCase())

        fakeRepository.hasPermission = false
        assertFalse(useCase())
    }
}
