package com.timelens.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.WellnessReport
import com.timelens.app.domain.repository.UsageRepository
import com.timelens.app.domain.usecase.CheckUsagePermissionUseCase
import com.timelens.app.domain.usecase.GetDailySummaryUseCase
import com.timelens.app.domain.usecase.GetWellnessReportUseCase
import com.timelens.app.util.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val summary: DaySummary,
        val comparisonText: String = "",
        val dailyGoalHours: Int = 6,
        val wellnessReport: WellnessReport? = null,
        val nightWellnessReport: WellnessReport? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data object MissingPermission : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase,
    private val getWellnessReportUseCase: GetWellnessReportUseCase,
    private val repository: UsageRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private fun currentDayElapsedFraction(): Float {
        val now = LocalTime.now()
        val minutesOfDay = now.hour * 60 + now.minute
        return (minutesOfDay.toFloat() / 1440f).coerceIn(0.15f, 1f)
    }

    private fun buildComparisonText(
        todayMs: Long,
        yesterdayMs: Long?,
        goalHours: Int,
        fraction: Float
    ): String {
        if (yesterdayMs == null || yesterdayMs <= 0L) {
            return "Límite diario: ${goalHours}h"
        }
        val baselineMs = (yesterdayMs * fraction).toLong().coerceAtLeast(1L)
        return TimeFormatter.formatPercentageChange(todayMs, baselineMs)
    }

    init {
        loadData()
        viewModelScope.launch {
            prefsManager.dailyGoalHours.collect { newGoal ->
                val current = _uiState.value
                if (current is HomeUiState.Success && current.dailyGoalHours != newGoal) {
                    val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
                    val fraction = currentDayElapsedFraction()
                    val updatedReport = getWellnessReportUseCase(
                        current.summary,
                        yesterday,
                        newGoal,
                        isNightReview = false,
                        dayElapsedFraction = fraction
                    )
                    val updatedNightReport = getWellnessReportUseCase(
                        current.summary,
                        yesterday,
                        newGoal,
                        isNightReview = true,
                        dayElapsedFraction = 1f
                    )
                    val comparisonText = buildComparisonText(
                        todayMs = current.summary.totalScreenTimeMs,
                        yesterdayMs = yesterday?.totalScreenTimeMs,
                        goalHours = newGoal,
                        fraction = fraction
                    )
                    _uiState.value = current.copy(
                        dailyGoalHours = newGoal,
                        comparisonText = comparisonText,
                        wellnessReport = updatedReport,
                        nightWellnessReport = updatedNightReport
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (checkUsagePermissionUseCase()) {
                    val summary = getDailySummaryUseCase()
                    val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
                    val goal = prefsManager.dailyGoalHours.value
                    val fraction = currentDayElapsedFraction()
                    val comparisonText = buildComparisonText(
                        todayMs = summary.totalScreenTimeMs,
                        yesterdayMs = yesterday?.totalScreenTimeMs,
                        goalHours = goal,
                        fraction = fraction
                    )
                    val wellnessReport = getWellnessReportUseCase(
                        summary,
                        yesterday,
                        goal,
                        isNightReview = false,
                        dayElapsedFraction = fraction
                    )
                    val nightWellnessReport = getWellnessReportUseCase(
                        summary,
                        yesterday,
                        goal,
                        isNightReview = true,
                        dayElapsedFraction = 1f
                    )
                    _uiState.value = HomeUiState.Success(
                        summary = summary,
                        comparisonText = comparisonText,
                        dailyGoalHours = goal,
                        wellnessReport = wellnessReport,
                        nightWellnessReport = nightWellnessReport
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Mantener estado actual si falla el refresh en background
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                if (!checkUsagePermissionUseCase()) {
                    _uiState.value = HomeUiState.MissingPermission
                    return@launch
                }
                val summary = getDailySummaryUseCase()
                val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
                val goal = prefsManager.dailyGoalHours.value
                val fraction = currentDayElapsedFraction()
                val comparisonText = buildComparisonText(
                    todayMs = summary.totalScreenTimeMs,
                    yesterdayMs = yesterday?.totalScreenTimeMs,
                    goalHours = goal,
                    fraction = fraction
                )

                val wellnessReport = getWellnessReportUseCase(
                    summary,
                    yesterday,
                    goal,
                    isNightReview = false,
                    dayElapsedFraction = fraction
                )
                val nightWellnessReport = getWellnessReportUseCase(
                    summary,
                    yesterday,
                    goal,
                    isNightReview = true,
                    dayElapsedFraction = 1f
                )

                _uiState.value = HomeUiState.Success(
                    summary = summary,
                    comparisonText = comparisonText,
                    dailyGoalHours = goal,
                    wellnessReport = wellnessReport,
                    nightWellnessReport = nightWellnessReport
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
