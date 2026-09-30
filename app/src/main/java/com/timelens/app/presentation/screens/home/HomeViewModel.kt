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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val summary: DaySummary,
        val comparisonText: String = "",
        val dailyGoalHours: Int = 6,
        val wellnessReport: WellnessReport? = null
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

    init {
        loadData()
        viewModelScope.launch {
            prefsManager.dailyGoalHours.collect { newGoal ->
                val current = _uiState.value
                if (current is HomeUiState.Success && current.dailyGoalHours != newGoal) {
                    val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
                    val updatedReport = getWellnessReportUseCase(current.summary, yesterday, newGoal)
                    _uiState.value = current.copy(dailyGoalHours = newGoal, wellnessReport = updatedReport)
                }
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
                val comparisonText = if (yesterday != null && yesterday.totalScreenTimeMs > 0) {
                    TimeFormatter.formatPercentageChange(summary.totalScreenTimeMs, yesterday.totalScreenTimeMs)
                } else {
                    "Meta diaria: ${goal}h"
                }

                val wellnessReport = getWellnessReportUseCase(summary, yesterday, goal)

                _uiState.value = HomeUiState.Success(
                    summary = summary,
                    comparisonText = comparisonText,
                    dailyGoalHours = goal,
                    wellnessReport = wellnessReport
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
