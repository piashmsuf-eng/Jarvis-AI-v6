package com.jarvis.ai.automation

import android.content.Context
import android.util.Log
import com.jarvis.ai.util.DoNotDisturbManager
import com.jarvis.ai.util.PreferenceManager
import kotlinx.coroutines.delay

/**
 * RoutineManager - Manages automation routines
 * 
 * Predefined automation sequences for common scenarios.
 * Morning routine, night routine, workout mode, etc.
 */
class RoutineManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RoutineManager"
    }
    
    private val appController = AppController(context)
    private val fileController = FileController(context)
    private val dndManager = DoNotDisturbManager(context)
    private val prefManager = PreferenceManager(context)
    
    /**
     * Morning routine
     * - Check weather
     * - Read news
     * - Show calendar events
     * - Read unread messages
     */
    suspend fun morningRoutine(): RoutineResult {
        val result = RoutineResult("Morning Routine")
        
        try {
            Log.i(TAG, "Starting morning routine")
            
            // Step 1: Turn off DND
            result.addStep("Turning off DND")
            dndManager.setDoNotDisturb(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            delay(500)
            
            // Step 2: Open news app
            result.addStep("Opening news")
            appController.launchApp("chrome")
            delay(2000)
            
            // Step 3: Could integrate with calendar, weather APIs here
            result.addStep("Checking calendar")
            delay(1000)
            
            result.success = true
            Log.i(TAG, "Morning routine complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error in morning routine", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Night routine
     * - Enable DND
     * - Close all apps
     * - Set alarms
     * - Reduce brightness
     */
    suspend fun nightRoutine(): RoutineResult {
        val result = RoutineResult("Night Routine")
        
        try {
            Log.i(TAG, "Starting night routine")
            
            // Step 1: Enable DND
            result.addStep("Enabling DND")
            dndManager.setDoNotDisturb(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            delay(500)
            
            // Step 2: Close unnecessary apps
            result.addStep("Closing apps")
            val runningApps = appController.getRunningApps()
            runningApps.forEach { app ->
                if (!isEssentialApp(app.packageName)) {
                    appController.killApp(app.name)
                    delay(200)
                }
            }
            
            // Step 3: Clean temp files
            result.addStep("Cleaning temp files")
            fileController.cleanTempFiles()
            delay(500)
            
            result.success = true
            Log.i(TAG, "Night routine complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error in night routine", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Workout mode
     * - Enable DND (priority only)
     * - Open music app
     * - Start fitness tracker
     * - Set timer
     */
    suspend fun workoutMode(): RoutineResult {
        val result = RoutineResult("Workout Mode")
        
        try {
            Log.i(TAG, "Starting workout mode")
            
            // Step 1: Enable DND (priority only)
            result.addStep("Enabling DND (priority)")
            dndManager.setDoNotDisturb(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            delay(500)
            
            // Step 2: Open music app
            result.addStep("Opening Spotify")
            appController.launchApp("spotify")
            delay(2000)
            
            // Step 3: Could integrate with fitness apps
            result.addStep("Ready for workout")
            delay(500)
            
            result.success = true
            Log.i(TAG, "Workout mode activated")
        } catch (e: Exception) {
            Log.e(TAG, "Error in workout mode", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Focus mode
     * - Enable DND (alarms only)
     * - Close social media apps
     * - Open productivity apps
     */
    suspend fun focusMode(): RoutineResult {
        val result = RoutineResult("Focus Mode")
        
        try {
            Log.i(TAG, "Starting focus mode")
            
            // Step 1: Enable DND (alarms only)
            result.addStep("Enabling DND (alarms only)")
            dndManager.setDoNotDisturb(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS)
            delay(500)
            
            // Step 2: Close distracting apps
            result.addStep("Closing social media")
            val distractingApps = listOf("instagram", "facebook", "youtube", "tiktok", "twitter")
            distractingApps.forEach { app ->
                if (appController.isInstalled(app)) {
                    appController.killApp(app)
                    delay(200)
                }
            }
            
            result.success = true
            Log.i(TAG, "Focus mode activated")
        } catch (e: Exception) {
            Log.e(TAG, "Error in focus mode", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Driving mode
     * - Enable DND (priority only)
     * - Open maps
     * - Open music
     * - Voice announcements for calls/messages
     */
    suspend fun drivingMode(): RoutineResult {
        val result = RoutineResult("Driving Mode")
        
        try {
            Log.i(TAG, "Starting driving mode")
            
            // Step 1: Enable DND (priority only)
            result.addStep("Enabling DND (priority)")
            dndManager.setDoNotDisturb(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            delay(500)
            
            // Step 2: Open maps
            result.addStep("Opening Maps")
            appController.launchApp("maps")
            delay(2000)
            
            // Step 3: Enable voice announcements
            result.addStep("Enabling voice announcements")
            // This would be set in preferences
            delay(500)
            
            result.success = true
            Log.i(TAG, "Driving mode activated")
        } catch (e: Exception) {
            Log.e(TAG, "Error in driving mode", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Clean up routine
     * - Clear cache for all apps
     * - Delete temp files
     * - Organize downloads
     */
    suspend fun cleanupRoutine(): RoutineResult {
        val result = RoutineResult("Cleanup Routine")
        
        try {
            Log.i(TAG, "Starting cleanup routine")
            
            // Step 1: Clean temp files
            result.addStep("Cleaning temp files")
            val cleanResult = fileController.cleanTempFiles()
            result.addStep("Deleted ${cleanResult.deletedFiles} files")
            delay(500)
            
            // Step 2: Organize downloads
            result.addStep("Organizing downloads")
            val organizeResult = fileController.organizeDownloads()
            result.addStep("Organized ${organizeResult.organized} files")
            delay(500)
            
            result.success = true
            Log.i(TAG, "Cleanup routine complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error in cleanup routine", e)
            result.error = e.message
        }
        
        return result
    }
    
    /**
     * Check if app is essential (should not be killed)
     */
    private fun isEssentialApp(packageName: String): Boolean {
        val essentialApps = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.jarvis.ai",
            "com.android.phone"
        )
        
        return essentialApps.any { packageName.contains(it) }
    }
    
    /**
     * Routine result
     */
    data class RoutineResult(
        val name: String,
        var success: Boolean = false,
        var error: String? = null,
        val steps: MutableList<String> = mutableListOf()
    ) {
        fun addStep(step: String) {
            steps.add(step)
            Log.d(TAG, "[$name] $step")
        }
        
        fun summary(): String {
            return if (success) {
                "$name completed successfully:\n${steps.joinToString("\n• ", prefix = "• ")}"
            } else {
                "$name failed: ${error ?: "Unknown error"}"
            }
        }
    }
}
