package com.timelens.app.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.fakes.FakePreferencesHelper
import com.timelens.app.fakes.FakeUsageRepository
import com.timelens.app.fakes.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeUsageRepository
    private lateinit var prefsManager: UserPreferencesManager

    @Before
    fun setUp() {
        fakeRepository = FakeUsageRepository()
        prefsManager = FakePreferencesHelper.createFakeUserPreferencesManager()
    }

    @Test
    fun `initialization loads app detail successfully`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("packageName" to "com.instagram.android"))

        val viewModel = AppDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            prefsManager = prefsManager
        )

        assertEquals("com.instagram.android", viewModel.packageName)
        val state = viewModel.uiState.value
        assertTrue(state is AppDetailUiState.Success)
        val success = state as AppDetailUiState.Success
        assertEquals("com.instagram.android", success.appDetail.packageName)
        assertEquals(1800000L, success.appDetail.totalTimeMs)
    }

    @Test
    fun `when repository throws error, uiState becomes Error`() = runTest {
        fakeRepository.shouldThrowError = true
        val savedStateHandle = SavedStateHandle(mapOf("packageName" to "com.buggy.app"))

        val viewModel = AppDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            prefsManager = prefsManager
        )

        val state = viewModel.uiState.value
        assertTrue(state is AppDetailUiState.Error)
        val error = state as AppDetailUiState.Error
        assertEquals("Error en repository", error.message)
    }

    @Test
    fun `setAppLimit and removeAppLimit update preferences correctly`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("packageName" to "com.instagram.android"))

        val viewModel = AppDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            prefsManager = prefsManager
        )

        assertNull(viewModel.appLimitMinutes.value)

        viewModel.setAppLimit(45)
        assertEquals(45, viewModel.appLimitMinutes.value)
        assertEquals(45, prefsManager.getAppLimit("com.instagram.android"))

        viewModel.removeAppLimit()
        assertNull(viewModel.appLimitMinutes.value)
        assertNull(prefsManager.getAppLimit("com.instagram.android"))
    }

    @Test
    fun `missing or empty packageName safely sets Error state without crash`() = runTest {
        val savedStateHandle = SavedStateHandle() // No packageName passed

        val viewModel = AppDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository,
            prefsManager = prefsManager
        )

        assertEquals("", viewModel.packageName)
        val state = viewModel.uiState.value
        assertTrue(state is AppDetailUiState.Error)
        val error = state as AppDetailUiState.Error
        assertEquals("No se especificó la aplicación", error.message)
    }
}
