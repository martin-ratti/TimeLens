package com.timelens.app.data.local.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.timelens.app.domain.model.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val packageManager: PackageManager = context.packageManager

    private val appNameCache = ConcurrentHashMap<String, String>()
    private val appIconCache = ConcurrentHashMap<String, Drawable?>()
    private val categoryCache = ConcurrentHashMap<String, AppCategory>()

    // One-time fast batch query for all user-launchable apps
    private val launchablePackages: Set<String> by lazy {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun getDailyUsageStats(): List<UsageStats> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        ).filter { it.totalTimeInForeground > 0 }
    }

    fun getDailyEvents(): List<UsageEvents.Event> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val events = usageStatsManager.queryEvents(
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        val eventList = mutableListOf<UsageEvents.Event>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            eventList.add(event)
        }
        return eventList
    }

    fun getUsageStatsForRange(startTime: Long, endTime: Long): List<UsageStats> {
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ).filter { it.totalTimeInForeground > 0 }
    }

    fun getAppName(packageName: String): String {
        return appNameCache.computeIfAbsent(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                when (packageName) {
                    "com.zhiliaoapp.musically" -> "TikTok"
                    "com.google.android.youtube" -> "YouTube"
                    "com.whatsapp" -> "WhatsApp"
                    "com.instagram.android" -> "Instagram"
                    "com.twitter.android", "com.x.android" -> "X"
                    "com.spotify.music" -> "Spotify"
                    "com.facebook.katana" -> "Facebook"
                    "com.google.android.apps.messaging" -> "Mensajes"
                    "com.google.android.dialer" -> "Teléfono"
                    "com.android.chrome" -> "Chrome"
                    else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                }
            }
        }
    }

    private val appIconBitmapCache = ConcurrentHashMap<String, ImageBitmap?>()

    fun getAppIcon(packageName: String): Drawable? {
        return appIconCache.computeIfAbsent(packageName) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getAppIconBitmap(packageName: String): ImageBitmap? {
        return appIconBitmapCache.computeIfAbsent(packageName) {
            try {
                val drawable = getAppIcon(packageName) ?: return@computeIfAbsent null
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                val bitmap = drawable.toBitmap(
                    width = width.coerceIn(48, 144),
                    height = height.coerceIn(48, 144)
                )
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun isAppEligibleForStats(packageName: String): Boolean {
        val lowerPkg = packageName.lowercase()
        // Always allow well-known user apps
        if (lowerPkg.contains("youtube") || 
            lowerPkg.contains("whatsapp") || 
            lowerPkg.contains("instagram") || 
            lowerPkg.contains("musically") || 
            lowerPkg.contains("spotify") || 
            lowerPkg.contains("twitter") || 
            lowerPkg.contains("tiktok") || 
            lowerPkg.contains("chrome")) {
            return true
        }
        // Ignore system launchers, system UI, and known internal packages
        if (lowerPkg.contains("systemui") || 
            lowerPkg.contains("launcher") || 
            lowerPkg.contains("digitalwellbeing") || 
            lowerPkg.contains("overlay") || 
            lowerPkg.contains("wallpaper") || 
            lowerPkg.contains("settings") || 
            lowerPkg == "android") {
            return false
        }
        // Fast O(1) in-memory lookup instead of blocking Binder IPC
        return launchablePackages.contains(packageName)
    }

    fun getAppCategory(packageName: String): AppCategory {
        return categoryCache.computeIfAbsent(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMING
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE,
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS,
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS,
                    android.content.pm.ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.UTILITY
                    else -> inferCategoryFromPackage(packageName)
                }
            } catch (e: Exception) {
                inferCategoryFromPackage(packageName)
            }
        }
    }

    private fun inferCategoryFromPackage(packageName: String): AppCategory {
        val lower = packageName.lowercase()
        return when {
            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("messenger") || lower.contains("messaging") || lower.contains("dialer") -> AppCategory.COMMUNICATION
            lower.contains("instagram") || lower.contains("tiktok") || lower.contains("musically") || lower.contains("twitter") || lower.contains("facebook") || lower.contains("x.android") || lower.contains("linkedin") || lower.contains("reddit") -> AppCategory.SOCIAL
            lower.contains("youtube") || lower.contains("spotify") || lower.contains("netflix") || lower.contains("twitch") || lower.contains("primevideo") || lower.contains("disney") -> AppCategory.ENTERTAINMENT
            lower.contains("chrome") || lower.contains("drive") || lower.contains("docs") || lower.contains("sheets") || lower.contains("notion") || lower.contains("slack") || lower.contains("gmail") || lower.contains("outlook") -> AppCategory.PRODUCTIVITY
            lower.contains("duolingo") || lower.contains("learn") -> AppCategory.EDUCATION
            lower.contains("game") || lower.contains("candycrush") || lower.contains("supercell") || lower.contains("roblox") || lower.contains("minecraft") -> AppCategory.GAMING
            else -> AppCategory.OTHER
        }
    }

    fun hasUsagePermission(): Boolean {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
        return stats.isNotEmpty()
    }
}
