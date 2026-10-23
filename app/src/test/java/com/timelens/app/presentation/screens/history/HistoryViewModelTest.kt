package com.timelens.app.presentation.screens.history

import com.timelens.app.domain.model.DaySummary
import com.timelens.app.fakes.FakeUsageRepository
import com.timelens.app.fakes.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeUsageRepository

    @Before
    fun setUp() {
        fakeRepository = FakeUsageRepository()
    }

    @Test
    fun `initialization loads weekly trend data successfully`() = runTest {
        val weekData = (1..7).map { i ->
            DaySummary(
                date = LocalDate.now().minusDays(i.toLong()),
                totalScreenTimeMs = i * 3600000L,
                totalUnlocks = 15,
                topApps = emptyList(),
                longestSession = null,
                peakHour = 18,
                productiveHour = 9,
                totalSessions = 5
            )
        }
        fakeRepository.weeklyTrendList.addAll(weekData)

        val viewModel = HistoryViewModel(fakeRepository)

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Success)
        val success = state as HistoryUiState.Success
        assertEquals(7, success.weeklyData.size)
    }

    @Test
    fun `when repository throws error, uiState becomes Error`() = runTest {
        fakeRepository.shouldThrowError = true

        val viewModel = HistoryViewModel(fakeRepository)

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Error)
        val error = state as HistoryUiState.Error
        assertEquals("Error en repository", error.message)
    }

    @Test
    fun `refresh updates weekly data and resets isRefreshing`() = runTest {
        val viewModel = HistoryViewModel(fakeRepository)
        assertFalse(viewModel.isRefreshing.value)

        viewModel.refresh()

        assertFalse(viewModel.isRefreshing.value)
        assertTrue(viewModel.uiState.value is HistoryUiState.Success)
    }

    private fun buildDays(count: Int): List<DaySummary> = (1..count).map { i ->
        DaySummary(
            date = LocalDate.now().minusDays((count - i).toLong()),
            totalScreenTimeMs = i * 1800000L,
            totalUnlocks = 10,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 12,
            productiveHour = 9,
            totalSessions = 3
        )
    }

    @Test
    fun `default range is 7 days`() = runTest {
        fakeRepository.weeklyTrendList.addAll(buildDays(30))

        val viewModel = HistoryViewModel(fakeRepository)

        assertEquals(7, viewModel.rangeDays.value)
        assertEquals(7, fakeRepository.lastRequestedTrendDays)
        val success = viewModel.uiState.value as HistoryUiState.Success
        assertEquals(7, success.weeklyData.size)
    }

    @Test
    fun `setRange 14 and 30 reloads data with the wider window`() = runTest {
        fakeRepository.weeklyTrendList.addAll(buildDays(30))
        val viewModel = HistoryViewModel(fakeRepository)

        viewModel.setRange(14)
        assertEquals(14, viewModel.rangeDays.value)
        assertEquals(14, (viewModel.uiState.value as HistoryUiState.Success).weeklyData.size)

        viewModel.setRange(30)
        assertEquals(30, viewModel.rangeDays.value)
        assertEquals(30, (viewModel.uiState.value as HistoryUiState.Success).weeklyData.size)
    }

    @Test
    fun `setRange ignores unsupported values`() = runTest {
        fakeRepository.weeklyTrendList.addAll(buildDays(30))
        val viewModel = HistoryViewModel(fakeRepository)

        viewModel.setRange(99)

        assertEquals(7, viewModel.rangeDays.value)
        assertEquals(7, fakeRepository.lastRequestedTrendDays)
    }

    @Test
    fun `refresh keeps the selected range`() = runTest {
        fakeRepository.weeklyTrendList.addAll(buildDays(30))
        val viewModel = HistoryViewModel(fakeRepository)
        viewModel.setRange(30)

        viewModel.refresh()

        assertEquals(30, fakeRepository.lastRequestedTrendDays)
        assertEquals(30, (viewModel.uiState.value as HistoryUiState.Success).weeklyData.size)
    }
}
