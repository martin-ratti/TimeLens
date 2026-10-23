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
import com.timelens.app.domain.util.UsageEventModel
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
    private val eligibilityCache = ConcurrentHashMap<String, Boolean>()

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

    private fun isRelevantEventType(type: Int): Boolean {
        return type == 1 || type == 2 || type == 16 || type == 17 || type == 18 || type == 26
    }

    fun getDailyEvents(): List<UsageEvents.Event> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return getEventsForRange(calendar.timeInMillis, System.currentTimeMillis())
    }

    fun getEventsForRange(startTime: Long, endTime: Long): List<UsageEvents.Event> {
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return emptyList()
        val eventList = mutableListOf<UsageEvents.Event>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            if (isRelevantEventType(event.eventType)) {
                eventList.add(event)
            }
        }
        return eventList
    }

    fun getDailyEventModels(): List<UsageEventModel> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return getEventModelsForRange(calendar.timeInMillis, System.currentTimeMillis())
    }

    fun getEventModelsForRange(startTime: Long, endTime: Long): List<UsageEventModel> {
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return emptyList()
        val modelList = ArrayList<UsageEventModel>(512)
        val buffer = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(buffer)
            val type = buffer.eventType
            if (isRelevantEventType(type)) {
                modelList.add(
                    UsageEventModel(
                        packageName = buffer.packageName,
                        eventType = type,
                        timeStamp = buffer.timeStamp
                    )
                )
            }
        }
        return modelList
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
                categoryCache.putIfAbsent(packageName, mapCategoryFromAppInfo(appInfo, packageName))
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                categoryCache.putIfAbsent(packageName, inferCategoryFromPackage(packageName))
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

    private val appIconBitmapCache = ConcurrentHashMap<String, ImageBitmap>()
    private val missingIconPackages = ConcurrentHashMap.newKeySet<String>()

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun getAppIconBitmap(packageName: String): ImageBitmap? {
        appIconBitmapCache[packageName]?.let { return it }
        if (missingIconPackages.contains(packageName)) return null
        return try {
            val drawable = getAppIcon(packageName)
            if (drawable == null) {
                missingIconPackages.add(packageName)
                return null
            }
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 84
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 84
            val bitmap = drawable.toBitmap(
                width = width.coerceIn(48, 84),
                height = height.coerceIn(48, 84)
            ).asImageBitmap()
            appIconBitmapCache[packageName] = bitmap
            bitmap
        } catch (e: Exception) {
            missingIconPackages.add(packageName)
            null
        }
    }

    fun isAppEligibleForStats(packageName: String): Boolean {
        return eligibilityCache.computeIfAbsent(packageName) { pkg ->
            val lowerPkg = pkg.lowercase()
            if (lowerPkg.contains("youtube") ||
                lowerPkg.contains("whatsapp") ||
                lowerPkg.contains("instagram") ||
                lowerPkg.contains("musically") ||
                lowerPkg.contains("spotify") ||
                lowerPkg.contains("twitter") ||
                lowerPkg.contains("tiktok") ||
                lowerPkg.contains("chrome")) {
                return@computeIfAbsent true
            }
            if (lowerPkg.contains("systemui") ||
                lowerPkg.contains("launcher") ||
                lowerPkg.contains("digitalwellbeing") ||
                lowerPkg.contains("overlay") ||
                lowerPkg.contains("wallpaper") ||
                lowerPkg.contains("settings") ||
                lowerPkg == "android") {
                return@computeIfAbsent false
            }
            launchablePackages.contains(pkg)
        }
    }

    fun getAppCategory(packageName: String): AppCategory {
        return categoryCache.computeIfAbsent(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                mapCategoryFromAppInfo(appInfo, packageName)
            } catch (e: Exception) {
                inferCategoryFromPackage(packageName)
            }
        }
    }

    private fun mapCategoryFromAppInfo(
        appInfo: android.content.pm.ApplicationInfo,
        packageName: String
    ): AppCategory {
        val inferred = inferCategoryFromPackage(packageName)
        if (inferred != AppCategory.OTHER) {
            return inferred
        }
        return when (appInfo.category) {
            android.content.pm.ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMING
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
            android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
            android.content.pm.ApplicationInfo.CATEGORY_IMAGE,
            android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            android.content.pm.ApplicationInfo.CATEGORY_NEWS,
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            android.content.pm.ApplicationInfo.CATEGORY_MAPS,
            android.content.pm.ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.UTILITY
            else -> AppCategory.OTHER
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
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
        if (appOps != null) {
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            if (mode == android.app.AppOpsManager.MODE_ALLOWED) {
                return true
            }
            if (mode == android.app.AppOpsManager.MODE_IGNORED || mode == android.app.AppOpsManager.MODE_ERRORED) {
                return false
            }
        }
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 7 * 24 * 60 * 60 * 1000L,
            now
        )
        return stats.isNotEmpty()
    }
}
