package com.jarvis.ai.automation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * AppController - Application management and control
 * 
 * Launch apps, kill apps, clear cache, and manage app data.
 * Uses LibSU for root operations.
 */
class AppController(private val context: Context) {
    
    companion object {
        private const val TAG = "AppController"
        
        // Common app package mappings
        val APP_PACKAGES = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "messenger" to "com.facebook.orca",
            "telegram" to "org.telegram.messenger",
            "twitter" to "com.twitter.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "snapchat" to "com.snapchat.android",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera2",
            "gallery" to "com.google.android.apps.photos",
            "settings" to "com.android.settings"
        )
    }
    
    private val packageManager = context.packageManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    data class AppInfo(
        val name: String,
        val packageName: String,
        val isSystemApp: Boolean,
        val isRunning: Boolean = false
    )
    
    /**
     * Launch app by name
     */
    fun launchApp(appName: String): Boolean {
        return try {
            // Try exact package name first
            var packageName = APP_PACKAGES[appName.lowercase()]
            
            // If not found, search installed apps
            if (packageName == null) {
                packageName = findAppPackage(appName)
            }
            
            if (packageName == null) {
                Log.w(TAG, "App not found: $appName")
                return false
            }
            
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Launched app: $appName ($packageName)")
                true
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
            false
        }
    }
    
    /**
     * Kill app (requires root)
     */
    fun killApp(appName: String): Boolean {
        return try {
            var packageName = APP_PACKAGES[appName.lowercase()]
            if (packageName == null) {
                packageName = findAppPackage(appName)
            }
            
            if (packageName == null) {
                Log.w(TAG, "App not found: $appName")
                return false
            }
            
            // Try without root first
            activityManager.killBackgroundProcesses(packageName)
            
            // If root available, force stop
            if (Shell.getShell().isRoot) {
                val result = Shell.cmd("am force-stop $packageName").exec()
                if (result.isSuccess) {
                    Log.i(TAG, "Killed app: $appName ($packageName)")
                    return true
                }
            }
            
            Log.w(TAG, "Could not kill app (no root): $appName")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error killing app", e)
            false
        }
    }
    
    /**
     * Clear app cache (requires root or app permission)
     */
    fun clearCache(appName: String): Boolean {
        return try {
            var packageName = APP_PACKAGES[appName.lowercase()]
            if (packageName == null) {
                packageName = findAppPackage(appName)
            }
            
            if (packageName == null) {
                Log.w(TAG, "App not found: $appName")
                return false
            }
            
            if (Shell.getShell().isRoot) {
                val result = Shell.cmd("pm clear $packageName").exec()
                if (result.isSuccess) {
                    Log.i(TAG, "Cleared cache for: $appName ($packageName)")
                    return true
                }
            } else {
                Log.w(TAG, "Cannot clear cache without root")
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }
    
    /**
     * Get running apps
     */
    fun getRunningApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        
        try {
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val runningPackages = runningApps.map { it.processName }.toSet()
            
            val installedApps = packageManager.getInstalledApplications(0)
            
            installedApps
                .filter { !isSystemApp(it) && runningPackages.contains(it.packageName) }
                .forEach { appInfo ->
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(
                        AppInfo(
                            name = name,
                            packageName = appInfo.packageName,
                            isSystemApp = false,
                            isRunning = true
                        )
                    )
                }
            
            Log.d(TAG, "Found ${apps.size} running apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting running apps", e)
        }
        
        return apps
    }
    
    /**
     * Get installed apps
     */
    fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        
        try {
            val installedApps = packageManager.getInstalledApplications(0)
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val runningPackages = runningApps.map { it.processName }.toSet()
            
            installedApps
                .filter { includeSystem || !isSystemApp(it) }
                .forEach { appInfo ->
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(
                        AppInfo(
                            name = name,
                            packageName = appInfo.packageName,
                            isSystemApp = isSystemApp(appInfo),
                            isRunning = runningPackages.contains(appInfo.packageName)
                        )
                    )
                }
            
            Log.d(TAG, "Found ${apps.size} installed apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
        }
        
        return apps
    }
    
    /**
     * Find app package by name
     */
    private fun findAppPackage(appName: String): String? {
        try {
            val installedApps = packageManager.getInstalledApplications(0)
            val query = appName.lowercase()
            
            return installedApps.firstOrNull { appInfo ->
                val name = packageManager.getApplicationLabel(appInfo).toString().lowercase()
                name.contains(query) || appInfo.packageName.lowercase().contains(query)
            }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error finding app package", e)
        }
        
        return null
    }
    
    /**
     * Check if app is system app
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
    
    /**
     * Check if app is installed
     */
    fun isInstalled(appName: String): Boolean {
        var packageName = APP_PACKAGES[appName.lowercase()]
        if (packageName == null) {
            packageName = findAppPackage(appName)
        }
        
        if (packageName == null) return false
        
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Format app list for voice output
     */
    fun formatForVoice(apps: List<AppInfo>): String {
        if (apps.isEmpty()) {
            return "কোন অ্যাপ নেই" // No apps
        }
        
        return apps.take(10).joinToString(", ") { it.name }
    }
}
