package com.timelens.app.data.local.prefs

import com.timelens.app.fakes.FakePreferencesHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserPreferencesManagerTest {

    private lateinit var prefsManager: UserPreferencesManager

    @Before
    fun setUp() {
        prefsManager = FakePreferencesHelper.createFakeUserPreferencesManager()
    }

    @Test
    fun `default preferences values are initialized correctly`() {
        assertEquals(6, prefsManager.dailyGoalHours.value)
        assertTrue(prefsManager.notificationsEnabled.value)
        assertTrue(prefsManager.darkThemeEnabled.value)
    }

    @Test
    fun `setDailyGoalHours updates state flow correctly`() {
        prefsManager.setDailyGoalHours(8)
        assertEquals(8, prefsManager.dailyGoalHours.value)

        prefsManager.setDailyGoalHours(4)
        assertEquals(4, prefsManager.dailyGoalHours.value)
    }

    @Test
    fun `setNotificationsEnabled toggles state flow`() {
        prefsManager.setNotificationsEnabled(false)
        assertFalse(prefsManager.notificationsEnabled.value)

        prefsManager.setNotificationsEnabled(true)
        assertTrue(prefsManager.notificationsEnabled.value)
    }

    @Test
    fun `setDarkThemeEnabled toggles state flow`() {
        prefsManager.setDarkThemeEnabled(false)
        assertFalse(prefsManager.darkThemeEnabled.value)

        prefsManager.setDarkThemeEnabled(true)
        assertTrue(prefsManager.darkThemeEnabled.value)
    }
}
