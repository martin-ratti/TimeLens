package com.timelens.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import com.timelens.app.data.local.db.dao.DailyUsageDao
import com.timelens.app.data.local.db.entity.DailyUsageEntity
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.fakes.FakePreferencesHelper
import com.timelens.app.fakes.MainDispatcherRule
import com.timelens.app.notification.TimeLensNotificationManager
import com.timelens.app.worker.NotificationWorkScheduler
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var prefsManager: UserPreferencesManager
    private lateinit var dailyUsageDao: DailyUsageDao
    private lateinit var notificationManager: TimeLensNotificationManager
    private lateinit var context: Context
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        mockkObject(NotificationWorkScheduler)

        every { NotificationWorkScheduler.scheduleAll(any()) } just Runs
        every { NotificationWorkScheduler.cancelAll(any()) } just Runs
        every { NotificationWorkScheduler.triggerImmediateDailySummary(any()) } just Runs
        every { NotificationWorkScheduler.triggerImmediateAlertCheck(any()) } just Runs

        prefsManager = FakePreferencesHelper.createFakeUserPreferencesManager()
        dailyUsageDao = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        viewModel = SettingsViewModel(
            prefsManager = prefsManager,
            dailyUsageDao = dailyUsageDao,
            notificationManager = notificationManager,
            context = context
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial preferences match defaults`() {
        assertEquals(6, viewModel.dailyGoalHours.value)
        assertTrue(viewModel.notificationsEnabled.value)
        assertTrue(viewModel.darkThemeEnabled.value)
    }

    @Test
    fun `setDailyGoal updates preferences and state flow`() {
        viewModel.setDailyGoal(5)
        assertEquals(5, viewModel.dailyGoalHours.value)

        viewModel.setDailyGoal(9)
        assertEquals(9, viewModel.dailyGoalHours.value)
    }

    @Test
    fun `toggleDarkTheme updates preferences and state flow`() {
        viewModel.toggleDarkTheme(false)
        assertFalse(viewModel.darkThemeEnabled.value)

        viewModel.toggleDarkTheme(true)
        assertTrue(viewModel.darkThemeEnabled.value)
    }

    @Test
    fun `toggleNotifications true enables notifications and schedules workers`() {
        viewModel.toggleNotifications(true)

        assertTrue(viewModel.notificationsEnabled.value)
        verify { notificationManager.createNotificationChannels() }
        verify { NotificationWorkScheduler.scheduleAll(context) }
    }

    @Test
    fun `toggleNotifications false disables notifications and cancels workers`() {
        viewModel.toggleNotifications(false)

        assertFalse(viewModel.notificationsEnabled.value)
        verify { NotificationWorkScheduler.cancelAll(context) }
        verify { notificationManager.cancelAll() }
    }

    @Test
    fun `sendTestNotification delegates to notification manager and returns result`() {
        every { notificationManager.showTestNotification() } returns true
        assertTrue(viewModel.sendTestNotification())

        every { notificationManager.showTestNotification() } returns false
        assertFalse(viewModel.sendTestNotification())
    }

    @Test
    fun `triggerDailySummaryTest calls NotificationWorkScheduler triggerImmediateDailySummary`() {
        viewModel.triggerDailySummaryTest()
        verify { NotificationWorkScheduler.triggerImmediateDailySummary(context) }
    }

    @Test
    fun `triggerAlertCheckTest calls NotificationWorkScheduler triggerImmediateAlertCheck`() {
        viewModel.triggerAlertCheckTest()
        verify { NotificationWorkScheduler.triggerImmediateAlertCheck(context) }
    }

    @Test
    fun `shareApp invokes callback with valid share intent`() {
        mockkStatic(Intent::class)
        val mockChooser = mockk<Intent>(relaxed = true)
        every { Intent.createChooser(any(), any()) } returns mockChooser

        var capturedIntent: Intent? = null
        viewModel.shareApp { chooserIntent ->
            capturedIntent = chooserIntent
        }

        assertNotNull(capturedIntent)
        assertEquals(mockChooser, capturedIntent)
        unmockkStatic(Intent::class)
    }

    @Test
    fun `exportDataCsv calls onEmpty when database has no records`() {
        coEvery { dailyUsageDao.getLastDays(30) } returns emptyList()

        var emptyCalled = false
        viewModel.exportDataCsv(
            onSuccess = {},
            onEmpty = { emptyCalled = true }
        )

        assertTrue(emptyCalled)
    }

    @Test
    fun `sanitizeCsvCell neutralizes formula injection prefixes and escapes commas and quotes`() {
        assertEquals("'=cmd|' /C calc'!A0", SettingsViewModel.sanitizeCsvCell("=cmd|' /C calc'!A0"))
        assertEquals("'+SUM(A1:A2)", SettingsViewModel.sanitizeCsvCell("+SUM(A1:A2)"))
        assertEquals("'-100", SettingsViewModel.sanitizeCsvCell("-100"))
        assertEquals("'@IMPORTXML()", SettingsViewModel.sanitizeCsvCell("@IMPORTXML()"))
        assertEquals("\"App, Inc.\"", SettingsViewModel.sanitizeCsvCell("App, Inc."))
        assertEquals("\"App \"\"Pro\"\"\"", SettingsViewModel.sanitizeCsvCell("App \"Pro\""))
        assertEquals("com.whatsapp", SettingsViewModel.sanitizeCsvCell("com.whatsapp"))
    }

    @Test
    fun `buildCsvContent generates valid CSV with productiveHour and sanitized cells`() {
        val days = listOf(
            DailyUsageEntity(
                date = "2026-10-01",
                totalScreenTimeMs = 3_600_000L,
                totalUnlocks = 18,
                longestSessionMs = 1_200_000L,
                longestSessionApp = "com.whatsapp",
                topAppPackage = "=malicious.pkg",
                topAppTimeMs = 1_800_000L,
                totalSessions = 9,
                peakHour = 20,
                productiveHour = 10
            )
        )

        val csv = SettingsViewModel.buildCsvContent(days)
        val lines = csv.trim().lines()

        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("HoraProductiva"))
        assertTrue(lines[1].contains("'=malicious.pkg"))
        assertTrue(lines[1].contains(",10:00,"))
    }
}
