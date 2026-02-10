package com.jarvis.ai.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * DoNotDisturbManager — Manages Do Not Disturb settings for Jarvis AI.
 * 
 * Handles:
 * - DND access permission checking
 * - DND mode toggling (Normal, Priority, Total Silence, Alarms Only)
 * - Saving and restoring DND state
 * - Device-specific workarounds for RedMagic and other manufacturers
 * 
 * RedMagic devices often reset DND settings, so we:
 * 1. Persist the user's preferred DND state
 * 2. Periodically check and restore it if changed
 * 3. Provide ADB commands for persistent configuration
 */
object DoNotDisturbManager {
    
    private const val TAG = "DNDManager"
    
    private const val PREF_DND_MODE = "saved_dnd_mode"
    private const val PREF_DND_ENABLED = "dnd_auto_restore_enabled"
    
    /**
     * DND interrupt filter modes
     */
    object Mode {
        const val ALL = NotificationManager.INTERRUPTION_FILTER_ALL
        const val PRIORITY = NotificationManager.INTERRUPTION_FILTER_PRIORITY
        const val NONE = NotificationManager.INTERRUPTION_FILTER_NONE
        const val ALARMS = NotificationManager.INTERRUPTION_FILTER_ALARMS
    }
    
    /**
     * Check if DND access permission is granted
     */
    fun hasDoNotDisturbAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.isNotificationPolicyAccessGranted
        } else {
            true // No DND permission needed on older Android
        }
    }
    
    /**
     * Request DND access permission
     */
    fun requestDoNotDisturbAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasDoNotDisturbAccess(context)) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open DND settings", e)
                }
            }
        }
    }
    
    /**
     * Get current DND mode
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getCurrentMode(context: Context): Int {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.currentInterruptionFilter
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current DND mode", e)
            Mode.ALL
        }
    }
    
    /**
     * Set DND mode
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setMode(context: Context, mode: Int): Boolean {
        if (!hasDoNotDisturbAccess(context)) {
            Log.w(TAG, "No DND access permission")
            return false
        }
        
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.setInterruptionFilter(mode)
            Log.i(TAG, "DND mode set to: ${getModeString(mode)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set DND mode", e)
            false
        }
    }
    
    /**
     * Toggle DND on/off
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun toggleDnd(context: Context): Boolean {
        val current = getCurrentMode(context)
        val newMode = if (current == Mode.ALL) Mode.PRIORITY else Mode.ALL
        return setMode(context, newMode)
    }
    
    /**
     * Enable DND (Priority mode by default)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun enableDnd(context: Context): Boolean {
        return setMode(context, Mode.PRIORITY)
    }
    
    /**
     * Disable DND (back to normal)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun disableDnd(context: Context): Boolean {
        return setMode(context, Mode.ALL)
    }
    
    /**
     * Save current DND mode to preferences
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveCurrentMode(context: Context) {
        val mode = getCurrentMode(context)
        val prefs = context.getSharedPreferences("jarvis_dnd", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_DND_MODE, mode).apply()
        Log.d(TAG, "Saved DND mode: ${getModeString(mode)}")
    }
    
    /**
     * Restore saved DND mode
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun restoreSavedMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences("jarvis_dnd", Context.MODE_PRIVATE)
        val savedMode = prefs.getInt(PREF_DND_MODE, Mode.ALL)
        
        if (savedMode == Mode.ALL) {
            return true // Nothing to restore
        }
        
        val current = getCurrentMode(context)
        if (current != savedMode) {
            Log.i(TAG, "Restoring DND mode from ${getModeString(current)} to ${getModeString(savedMode)}")
            return setMode(context, savedMode)
        }
        
        return true
    }
    
    /**
     * Enable auto-restore of DND mode
     */
    fun setAutoRestoreEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("jarvis_dnd", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_DND_ENABLED, enabled).apply()
    }
    
    /**
     * Check if auto-restore is enabled
     */
    fun isAutoRestoreEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("jarvis_dnd", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_DND_ENABLED, false)
    }
    
    /**
     * Get DND mode as human-readable string
     */
    fun getModeString(mode: Int): String {
        return when (mode) {
            Mode.ALL -> "Normal (All notifications)"
            Mode.PRIORITY -> "Priority only"
            Mode.NONE -> "Total silence"
            Mode.ALARMS -> "Alarms only"
            else -> "Unknown ($mode)"
        }
    }
    
    /**
     * Get current DND status as human-readable string
     */
    fun getStatus(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasDoNotDisturbAccess(context)) {
                "❌ No DND access permission"
            } else {
                val mode = getCurrentMode(context)
                "🔔 ${getModeString(mode)}"
            }
        } else {
            "ℹ️ DND not available on this Android version"
        }
    }
    
    /**
     * Get ADB commands to force-enable DND access
     * Useful for RedMagic and other devices that won't grant permission
     */
    fun getAdbCommands(context: Context): String {
        val packageName = context.packageName
        return buildString {
            appendLine("# Connect device via USB with USB Debugging enabled")
            appendLine("# Then run these commands:")
            appendLine()
            appendLine("# Grant DND access permission:")
            appendLine("adb shell cmd notification allow_dnd $packageName")
            appendLine()
            appendLine("# Verify permission:")
            appendLine("adb shell cmd notification allow_listener $packageName")
        }
    }
    
    /**
     * Check if device is in silent mode (ringer)
     */
    fun isDeviceSilent(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (am.ringerMode) {
                AudioManager.RINGER_MODE_SILENT, AudioManager.RINGER_MODE_VIBRATE -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check ringer mode", e)
            false
        }
    }
    
    /**
     * Set ringer mode
     */
    fun setRingerMode(context: Context, mode: Int): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode = mode
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set ringer mode", e)
            false
        }
    }
}
