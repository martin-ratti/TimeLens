package com.timelens.app.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.model.AppDetailInfo
import com.timelens.app.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState
    data class Success(val appDetail: AppDetailInfo) : AppDetailUiState
    data class Error(val message: String) : AppDetailUiState
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UsageRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    val packageName: String = checkNotNull(savedStateHandle["packageName"])

    private val _uiState = MutableStateFlow<AppDetailUiState>(AppDetailUiState.Loading)
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    val appLimitMinutes: StateFlow<Int?> = prefsManager.appLimits.map {
        it[packageName]
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = prefsManager.getAppLimit(packageName)
    )

    init {
        loadAppDetail()
    }

    fun loadAppDetail() {
        viewModelScope.launch {
            _uiState.value = AppDetailUiState.Loading
            try {
                val detail = repository.getAppDetail(packageName)
                _uiState.value = AppDetailUiState.Success(detail)
            } catch (e: Exception) {
                _uiState.value = AppDetailUiState.Error(e.message ?: "Error al cargar detalle")
            }
        }
    }

    fun setAppLimit(minutes: Int) {
        prefsManager.setAppLimit(packageName, minutes)
    }

    fun removeAppLimit() {
        prefsManager.removeAppLimit(packageName)
    }
}
