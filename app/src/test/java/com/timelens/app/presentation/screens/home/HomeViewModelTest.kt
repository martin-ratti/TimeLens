package com.timelens.app.presentation.screens.home

import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.usecase.CheckUsagePermissionUseCase
import com.timelens.app.domain.usecase.GetDailySummaryUseCase
import com.timelens.app.domain.usecase.GetWellnessReportUseCase
import com.timelens.app.fakes.FakePreferencesHelper
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeUsageRepository
    private lateinit var prefsManager: UserPreferencesManager
    private lateinit var getDailySummaryUseCase: GetDailySummaryUseCase
    private lateinit var checkUsagePermissionUseCase: CheckUsagePermissionUseCase
    private lateinit var getWellnessReportUseCase: GetWellnessReportUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeUsageRepository()
        prefsManager = FakePreferencesHelper.createFakeUserPreferencesManager()
        getDailySummaryUseCase = GetDailySummaryUseCase(fakeRepository)
        checkUsagePermissionUseCase = CheckUsagePermissionUseCase(fakeRepository)
        getWellnessReportUseCase = GetWellnessReportUseCase()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getDailySummaryUseCase = getDailySummaryUseCase,
            checkUsagePermissionUseCase = checkUsagePermissionUseCase,
            getWellnessReportUseCase = getWellnessReportUseCase,
            repository = fakeRepository,
            prefsManager = prefsManager
        )
    }

    @Test
    fun `when usage permission is missing, state is MissingPermission`() = runTest {
        fakeRepository.hasPermission = false

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.MissingPermission)
    }

    @Test
    fun `when usage permission granted, loads data and sets Success state`() = runTest {
        fakeRepository.hasPermission = true
        fakeRepository.todaySummary = DaySummary(
            date = LocalDate.now(),
            totalScreenTimeMs = 3 * 3600000L,
            totalUnlocks = 25,
            topApps = emptyList(),
            longestSession = null,
            peakHour = 15,
            productiveHour = 10,
            totalSessions = 12
        )

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(3 * 3600000L, success.summary.totalScreenTimeMs)
        assertEquals(25, success.summary.totalUnlocks)
        assertEquals(prefsManager.dailyGoalHours.value, success.dailyGoalHours)
        assertNotNull(success.wellnessReport)
    }

    @Test
    fun `when goal changes in preferences, state updates goal and wellness report`() = runTest {
        fakeRepository.hasPermission = true
        val viewModel = createViewModel()

        prefsManager.setDailyGoalHours(8)

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(8, success.dailyGoalHours)
    }

    @Test
    fun `when repository throws exception, state transitions to Error`() = runTest {
        fakeRepository.hasPermission = true
        fakeRepository.shouldThrowError = true

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertTrue(error.message.contains("Error en repository"))
    }

    @Test
    fun `refresh updates data and manages isRefreshing state flow`() = runTest {
        fakeRepository.hasPermission = true
        val viewModel = createViewModel()

        assertFalse(viewModel.isRefreshing.value)

        viewModel.refresh()

        assertFalse(viewModel.isRefreshing.value)
        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
    }
}
