package com.timelens.app.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
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

    @Before
    fun setUp() {
        fakeRepository = FakeUsageRepository()
    }

    @Test
    fun `initialization loads app detail successfully`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("packageName" to "com.instagram.android"))

        val viewModel = AppDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = fakeRepository
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
            repository = fakeRepository
        )

        val state = viewModel.uiState.value
        assertTrue(state is AppDetailUiState.Error)
        val error = state as AppDetailUiState.Error
        assertEquals("Error en repository", error.message)
    }
}
