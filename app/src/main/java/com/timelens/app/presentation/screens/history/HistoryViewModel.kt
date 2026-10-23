package com.timelens.app.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.domain.model.DaySummary
import com.timelens.app.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val weeklyData: List<DaySummary>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadWeeklyData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val data = repository.getWeeklyTrend()
                _uiState.value = HistoryUiState.Success(data)
            } catch (e: Exception) {
                // Keep existing state if background refresh fails
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadWeeklyData() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            try {
                val data = repository.getWeeklyTrend()
                _uiState.value = HistoryUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
