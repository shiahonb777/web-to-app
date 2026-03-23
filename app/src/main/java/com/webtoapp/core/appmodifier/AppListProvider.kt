package com.webtoapp.core.appmodifier

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用列表提供器
 * 获取设备上已安装的应用列表
 */
class AppListProvider(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * 获取已安装应用列表
     * @param filter 筛选类型
     * @param searchQuery 搜索关键词
     * @return 应用列表
     */
    suspend fun getInstalledApps(
        filter: AppFilterType = AppFilterType.USER,
        searchQuery: String = ""
    ): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

        packages
            .mapNotNull { packageInfo -> packageInfo.toInstalledAppInfo() }
            .filter { app ->
                // App类型筛选
                when (filter) {
                    AppFilterType.ALL -> true
                    AppFilterType.USER -> !app.isSystemApp
                    AppFilterType.SYSTEM -> app.isSystemApp
                }
            }
            .filter { app ->
                // Search筛选
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
     * 获取指定包名的应用信息
     */
    suspend fun getAppInfo(packageName: String): InstalledAppInfo? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.toInstalledAppInfo()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * PackageInfo 转换为 InstalledAppInfo
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
     * 检查应用是否已安装
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get app数量
     */
    suspend fun getAppCount(filter: AppFilterType = AppFilterType.USER): Int {
        return getInstalledApps(filter).size
    }
}
