package com.webtoapp.core.appmodifier

import android.content.Intent
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Note: brief English comment.
 * Note: brief English comment.
 */
class AppListProvider(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun getInstalledApps(
        filter: AppFilterType = AppFilterType.USER,
        searchQuery: String = ""
    ): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        // Since Android 11 package visibility restrictions, avoid relying on
        // QUERY_ALL_PACKAGES and only enumerate launchable apps.
        val packageNames = getLaunchablePackageNames()
        val packages = packageNames.mapNotNull { packageName ->
            getPackageInfoSafely(packageName)
        }

        packages
            .mapNotNull { packageInfo -> packageInfo.toInstalledAppInfo() }
            .filter { app ->
                // Note: brief English comment.
                when (filter) {
                    AppFilterType.ALL -> true
                    AppFilterType.USER -> !app.isSystemApp
                    AppFilterType.SYSTEM -> app.isSystemApp
                }
            }
            .filter { app ->
                // Note: brief English comment.
                if (searchQuery.isBlank()) {
                    true
                } else {
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Note: brief English comment.
     */
    suspend fun getAppInfo(packageName: String): InstalledAppInfo? = withContext(Dispatchers.IO) {
        getPackageInfoSafely(packageName)?.toInstalledAppInfo()
    }

    /**
     * Note: brief English comment.
     */
    private fun PackageInfo.toInstalledAppInfo(): InstalledAppInfo? {
        val appInfo = applicationInfo ?: return null
        
        val appName = try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val icon = try {
            packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            null
        }

        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        
        val apkPath = appInfo.sourceDir
        val apkSize = try {
            File(apkPath).length()
        } catch (e: Exception) {
            0L
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

        return InstalledAppInfo(
            packageName = packageName,
            appName = appName,
            versionName = versionName ?: "1.0",
            versionCode = versionCode,
            icon = icon,
            apkPath = apkPath,
            isSystemApp = isSystemApp,
            installedTime = firstInstallTime,
            updatedTime = lastUpdateTime,
            apkSize = apkSize
        )
    }

    /**
     * Note: brief English comment.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return getPackageInfoSafely(packageName) != null
    }

    /**
     * Note: brief English comment.
     */
    suspend fun getAppCount(filter: AppFilterType = AppFilterType.USER): Int {
        return getInstalledApps(filter).size
    }

    private fun getPackageInfoSafely(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getLaunchablePackageNames(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolves
            .mapNotNull { it.activityInfo?.packageName }
            .toMutableSet()
            .apply { add(context.packageName) }
    }
}
