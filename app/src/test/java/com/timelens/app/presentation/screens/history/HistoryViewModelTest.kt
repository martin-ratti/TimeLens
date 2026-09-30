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
}
