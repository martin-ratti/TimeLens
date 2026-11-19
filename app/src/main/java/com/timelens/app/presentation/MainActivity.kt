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
        
        targetNavRoute.value = sanitizeNavRoute(intent?.getStringExtra("EXTRA_NAV_ROUTE"))

        val startDestination = if (checkUsagePermissionUseCase()) {
            NavRoutes.Home.route
        } else {
            NavRoutes.Onboarding.route
        }

        setContent {
            val themeMode by prefsManager.themeMode.collectAsStateWithLifecycle(initialValue = com.timelens.app.domain.model.ThemeMode.DARK)
            val navigateToRoute by targetNavRoute
            TimeLensTheme(themeMode = themeMode) {
                AppNavHost(
                    startDestination = startDestination,
                    navigateToRoute = navigateToRoute,
                    onNavRouteConsumed = {
                        targetNavRoute.value = null
                        intent?.removeExtra("EXTRA_NAV_ROUTE")
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        com.timelens.app.widget.WidgetUpdateWorker.refreshNow(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetNavRoute.value = sanitizeNavRoute(intent.getStringExtra("EXTRA_NAV_ROUTE"))
    }

    companion object {
        private val ALLOWED_STATIC_ROUTES = setOf(
            NavRoutes.Home.route,
            NavRoutes.History.route,
            NavRoutes.Settings.route,
            NavRoutes.Onboarding.route,
            "daily_summary",
            "daily_debrief",
            "daily_summary_night"
        )
        private val APP_DETAIL_REGEX = Regex("^app_detail/[a-zA-Z0-9_.]+$")

        fun sanitizeNavRoute(rawRoute: String?): String? {
            val candidate = rawRoute?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return if (candidate in ALLOWED_STATIC_ROUTES || APP_DETAIL_REGEX.matches(candidate)) {
                candidate
            } else {
                null
            }
        }
    }
}
