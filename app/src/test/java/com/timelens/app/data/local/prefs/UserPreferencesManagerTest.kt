package com.timelens.app.data.local.prefs

import com.timelens.app.domain.model.ThemeMode
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
        assertEquals(ThemeMode.DARK, prefsManager.themeMode.value)
        assertFalse(prefsManager.dynamicColorEnabled.value)
        assertTrue(prefsManager.appLimits.value.isEmpty())
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
        assertEquals(ThemeMode.LIGHT, prefsManager.themeMode.value)

        prefsManager.setDarkThemeEnabled(true)
        assertTrue(prefsManager.darkThemeEnabled.value)
        assertEquals(ThemeMode.DARK, prefsManager.themeMode.value)
    }

    @Test
    fun `setThemeMode updates themeMode and darkThemeEnabled correctly`() {
        prefsManager.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, prefsManager.themeMode.value)
        assertFalse(prefsManager.darkThemeEnabled.value)

        prefsManager.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, prefsManager.themeMode.value)
        assertTrue(prefsManager.darkThemeEnabled.value)

        prefsManager.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefsManager.themeMode.value)
        assertTrue(prefsManager.darkThemeEnabled.value)
    }

    @Test
    fun `setDynamicColorEnabled updates state flow`() {
        prefsManager.setDynamicColorEnabled(true)
        assertTrue(prefsManager.dynamicColorEnabled.value)

        prefsManager.setDynamicColorEnabled(false)
        assertFalse(prefsManager.dynamicColorEnabled.value)
    }

    @Test
    fun `app limits can be added retrieved and removed`() {
        assertNull(prefsManager.getAppLimit("com.youtube"))

        prefsManager.setAppLimit("com.youtube", 60)
        assertEquals(60, prefsManager.getAppLimit("com.youtube"))
        assertEquals(1, prefsManager.appLimits.value.size)
        assertEquals(60, prefsManager.appLimits.value["com.youtube"])

        prefsManager.setAppLimit("com.instagram", 45)
        assertEquals(2, prefsManager.appLimits.value.size)

        prefsManager.removeAppLimit("com.youtube")
        assertNull(prefsManager.getAppLimit("com.youtube"))
        assertEquals(1, prefsManager.appLimits.value.size)
        assertEquals(45, prefsManager.appLimits.value["com.instagram"])
    }
}
