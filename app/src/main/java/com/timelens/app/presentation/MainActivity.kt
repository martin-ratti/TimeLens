package com.timelens.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.data.local.prefs.UserPreferencesManager
import com.timelens.app.domain.usecase.CheckUsagePermissionUseCase
import com.timelens.app.presentation.navigation.AppNavHost
import com.timelens.app.presentation.navigation.NavRoutes
import com.timelens.app.presentation.theme.TimeLensTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkUsagePermissionUseCase: CheckUsagePermissionUseCase

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    private val targetNavRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        targetNavRoute.value = intent?.getStringExtra("EXTRA_NAV_ROUTE")

        val startDestination = if (checkUsagePermissionUseCase()) {
            NavRoutes.Home.route
        } else {
            NavRoutes.Onboarding.route
        }

        setContent {
            val themeMode by prefsManager.themeMode.collectAsStateWithLifecycle(initialValue = com.timelens.app.domain.model.ThemeMode.DARK)
            val dynamicColor by prefsManager.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = false)
            val navigateToRoute by targetNavRoute
            TimeLensTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AppNavHost(
                    startDestination = startDestination,
                    navigateToRoute = navigateToRoute
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetNavRoute.value = intent.getStringExtra("EXTRA_NAV_ROUTE")
    }
}
