package com.timelens.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesManager(
    private val prefs: SharedPreferences
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences("timelens_user_prefs", Context.MODE_PRIVATE)
    )

    private val _dailyGoalHours = MutableStateFlow(prefs.getInt("daily_goal_hours", 6))
    val dailyGoalHours: StateFlow<Int> = _dailyGoalHours.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val savedThemeMode = prefs.getString("theme_mode", null)
        ?: if (prefs.getBoolean("dark_theme_enabled", true)) com.timelens.app.domain.model.ThemeMode.DARK.name else com.timelens.app.domain.model.ThemeMode.LIGHT.name

    private val _themeMode = MutableStateFlow(
        runCatching { com.timelens.app.domain.model.ThemeMode.valueOf(savedThemeMode) }
            .getOrDefault(com.timelens.app.domain.model.ThemeMode.DARK)
    )
    val themeMode: StateFlow<com.timelens.app.domain.model.ThemeMode> = _themeMode.asStateFlow()

    private val _darkThemeEnabled = MutableStateFlow(_themeMode.value != com.timelens.app.domain.model.ThemeMode.LIGHT)
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    init {
        if (prefs.contains("dynamic_color_enabled")) {
            prefs.edit().remove("dynamic_color_enabled").apply()
        }
    }

    private fun loadAppLimits(): Map<String, Int> {
        val rawSet = prefs.getStringSet("app_limits_set", emptySet()) ?: emptySet()
        return rawSet.mapNotNull { entry ->
            val colonIndex = entry.lastIndexOf(':')
            if (colonIndex > 0 && colonIndex < entry.length - 1) {
                val pkg = entry.substring(0, colonIndex)
                val mins = entry.substring(colonIndex + 1).toIntOrNull()
                if (mins != null) pkg to mins else null
            } else null
        }.toMap()
    }

    private val _appLimits = MutableStateFlow(loadAppLimits())
    val appLimits: StateFlow<Map<String, Int>> = _appLimits.asStateFlow()

    fun setAppLimit(packageName: String, minutes: Int) {
        val updated = _appLimits.value.toMutableMap()
        updated[packageName] = minutes
        val set = updated.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet("app_limits_set", set).apply()
        _appLimits.value = updated
    }

    fun removeAppLimit(packageName: String) {
        val updated = _appLimits.value.toMutableMap()
        updated.remove(packageName)
        val set = updated.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet("app_limits_set", set).apply()
        _appLimits.value = updated
    }

    fun getAppLimit(packageName: String): Int? {
        return _appLimits.value[packageName]
    }

    fun setDailyGoalHours(hours: Int) {
        prefs.edit().putInt("daily_goal_hours", hours).apply()
        _dailyGoalHours.value = hours
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setThemeMode(mode: com.timelens.app.domain.model.ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
        val isDark = mode != com.timelens.app.domain.model.ThemeMode.LIGHT
        prefs.edit().putBoolean("dark_theme_enabled", isDark).apply()
        _darkThemeEnabled.value = isDark
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        val mode = if (enabled) com.timelens.app.domain.model.ThemeMode.DARK else com.timelens.app.domain.model.ThemeMode.LIGHT
        setThemeMode(mode)
    }
}
