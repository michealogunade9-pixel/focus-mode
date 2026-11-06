package com.example.focuslock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.focuslock.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledLauncherApps(blockedPackages: Set<String>): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
            }

            val ownPackage = context.packageName
            val appList = mutableListOf<InstalledAppInfo>()
            val seenPackages = mutableSetOf<String>()

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == ownPackage || seenPackages.contains(pkgName)) {
                    continue
                }
                seenPackages.add(pkgName)

                val appName = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    pkgName
                }

                val appInfo = resolveInfo.activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = determineCategory(pkgName, appInfo)

                appList.add(
                    InstalledAppInfo(
                        packageName = pkgName,
                        appName = appName,
                        isBlocked = blockedPackages.contains(pkgName),
                        category = category,
                        isSystemApp = isSystem
                    )
                )
            }

            // Sort alphabetical by app name
            appList.sortedBy { it.appName.lowercase() }
        }

    fun getAppName(packageName: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun determineCategory(packageName: String, appInfo: ApplicationInfo): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("instagram") || lower.contains("facebook") || lower.contains("tiktok") ||
                    lower.contains("twitter") || lower.contains("snapchat") || lower.contains("reddit") ||
                    lower.contains("threads") || lower.contains("discord") || lower.contains("telegram") ||
                    lower.contains("whatsapp") -> "Social"

            lower.contains("youtube") || lower.contains("netflix") || lower.contains("spotify") ||
                    lower.contains("twitch") || lower.contains("disney") || lower.contains("hulu") ||
                    lower.contains("primevideo") || lower.contains("music") || lower.contains("video") -> "Entertainment"

            lower.contains("game") || lower.contains("play") || lower.contains("arcade") ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) -> "Games"

            lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") ||
                    lower.contains("opera") || lower.contains("edge") -> "Browsers"

            lower.contains("docs") || lower.contains("sheets") || lower.contains("notes") ||
                    lower.contains("calendar") || lower.contains("mail") || lower.contains("gmail") ||
                    lower.contains("outlook") || lower.contains("slack") -> "Productivity"

            else -> "Other"
        }
    }
}
