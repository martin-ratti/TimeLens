package com.timelens.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object AppDetail : NavRoutes("app_detail/{packageName}") {
        fun createRoute(packageName: String) = "app_detail/$packageName"
    }
    data object History : NavRoutes("history")
    data object Settings : NavRoutes("settings")
    data object Onboarding : NavRoutes("onboarding")
}

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(NavRoutes.Home.route, Icons.Filled.Home, "Inicio"),
    HISTORY(NavRoutes.History.route, Icons.AutoMirrored.Filled.ShowChart, "Tendencias"),
    SETTINGS(NavRoutes.Settings.route, Icons.Filled.Settings, "Ajustes")
}
