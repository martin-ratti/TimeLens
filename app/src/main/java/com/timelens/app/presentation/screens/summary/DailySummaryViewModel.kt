package com.timelens.app.presentation.screens.summary

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.model.WellnessReport
import com.timelens.app.domain.repository.UsageRepository
import com.timelens.app.domain.usecase.GetDailySummaryUseCase
import com.timelens.app.domain.usecase.GetWellnessReportUseCase
import com.timelens.app.util.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed interface DailySummaryUiState {
    data object Loading : DailySummaryUiState
    data class Success(
        val summary: DaySummary,
        val yesterdaySummary: DaySummary?,
        val wellnessReport: WellnessReport,
        val dailyGoalHours: Int,
        val comparisonText: String
    ) : DailySummaryUiState
    data class Error(val message: String) : DailySummaryUiState
}

@HiltViewModel
class DailySummaryViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val getWellnessReportUseCase: GetWellnessReportUseCase,
    private val repository: UsageRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailySummaryUiState>(DailySummaryUiState.Loading)
    val uiState: StateFlow<DailySummaryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = DailySummaryUiState.Loading
            try {
                val summary = getDailySummaryUseCase()
                val yesterday = repository.getDaySummary(LocalDate.now().minusDays(1))
                val goal = prefsManager.dailyGoalHours.value

                val comparisonText = if (yesterday != null && yesterday.totalScreenTimeMs > 0) {
                    TimeFormatter.formatPercentageChange(summary.totalScreenTimeMs, yesterday.totalScreenTimeMs)
                } else {
                    "Meta: ${goal}h"
                }

                val report = getWellnessReportUseCase(summary, yesterday, goal)

                _uiState.value = DailySummaryUiState.Success(
                    summary = summary,
                    yesterdaySummary = yesterday,
                    wellnessReport = report,
                    dailyGoalHours = goal,
                    comparisonText = comparisonText
                )
            } catch (e: Exception) {
                _uiState.value = DailySummaryUiState.Error(e.localizedMessage ?: "Error al cargar resumen")
            }
        }
    }

    fun shareSummary(context: Context) {
        val state = _uiState.value as? DailySummaryUiState.Success ?: return
        val dateFormatted = state.summary.date.format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
        ).replaceFirstChar { it.uppercase() }

        val totalTime = TimeFormatter.formatMillisToShort(state.summary.totalScreenTimeMs)
        val topApp = state.summary.topApps.firstOrNull()?.appName ?: "Sin uso"
        val topAppTime = state.summary.topApps.firstOrNull()?.let {
            TimeFormatter.formatMillisToShort(it.totalTimeMs)
        } ?: "0m"

        val shareText = buildString {
            append("📊 Resumen Diario de Bienestar — TimeLens\n")
            append("📅 $dateFormatted\n\n")
            append("⏱️ Tiempo de pantalla: $totalTime (${state.comparisonText})\n")
            append("🌱 Diagnóstico: ${state.wellnessReport.overallStatus} (Score: ${state.wellnessReport.overallScore}/100)\n")
            append("🔓 Desbloqueos: ${state.summary.totalUnlocks} veces\n")
            append("📱 App principal: $topApp ($topAppTime)\n")
            if (state.summary.longestSession != null) {
                append("⏳ Sesión más larga: ${TimeFormatter.formatMillisToShort(state.summary.longestSession.durationMs)} en ${state.summary.longestSession.appName}\n")
            }
            append("\n💡 Consejo destacado:\n")
            append("${state.wellnessReport.primaryInsight.title}: ${state.wellnessReport.primaryInsight.actionTip}\n\n")
            append("Seguimiento y bienestar con #TimeLens")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Compartir resumen de bienestar")
        context.startActivity(chooser)
    }
}
