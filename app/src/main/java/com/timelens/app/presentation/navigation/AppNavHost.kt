package com.timelens.app.presentation.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.timelens.app.presentation.screens.history.HistoryScreen
import com.timelens.app.presentation.screens.home.HomeScreen
import com.timelens.app.presentation.screens.onboarding.OnboardingScreen
import com.timelens.app.presentation.screens.settings.SettingsScreen
import com.timelens.app.presentation.theme.NeonBlue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startDestination: String = NavRoutes.Home.route,
    navigateToRoute: String? = null
) {
    val navController = rememberNavController()
    var openDebriefOnHome by remember { mutableStateOf(false) }

    LaunchedEffect(navigateToRoute) {
        if (!navigateToRoute.isNullOrBlank()) {
            if (navigateToRoute == "daily_summary" || navigateToRoute == "daily_debrief") {
                openDebriefOnHome = true
                navController.navigate(NavRoutes.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(navigateToRoute) {
                    launchSingleTop = true
                }
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    val showBottomBar = BottomNavItem.entries.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonBlue,
                                selectedTextColor = NeonBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                indicatorColor = NeonBlue.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(NavRoutes.Onboarding.route) {
                OnboardingScreen(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    onPermissionGranted = {
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    onAppClick = { packageName ->
                        navController.navigate(NavRoutes.AppDetail.createRoute(packageName))
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavRoutes.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    openDebriefDirectly = openDebriefOnHome
                )
            }
            composable(
                route = NavRoutes.AppDetail.route,
                arguments = listOf(androidx.navigation.navArgument("packageName") {
                    type = androidx.navigation.NavType.StringType
                })
            ) {
                com.timelens.app.presentation.screens.detail.AppDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.History.route) {
                HistoryScreen()
            }
            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    onNavigateToSummary = {
                        openDebriefOnHome = true
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
