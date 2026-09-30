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

    private val _darkThemeEnabled = MutableStateFlow(prefs.getBoolean("dark_theme_enabled", true))
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    fun setDailyGoalHours(hours: Int) {
        prefs.edit().putInt("daily_goal_hours", hours).apply()
        _dailyGoalHours.value = hours
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme_enabled", enabled).apply()
        _darkThemeEnabled.value = enabled
    }
}
