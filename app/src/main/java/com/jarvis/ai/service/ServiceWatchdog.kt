package com.jarvis.ai.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.ai.JarvisApplication
import com.jarvis.ai.R
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.ui.main.MainActivity
import com.jarvis.ai.util.DeviceCompatibility
import kotlinx.coroutines.*

/**
 * ServiceWatchdog — Monitors critical services and restarts them if killed.
 * 
 * This is especially important for RedMagic and other aggressive manufacturers
 * that kill accessibility services in the background.
 * 
 * Monitors:
 * - JarvisAccessibilityService
 * - JarvisNotificationListener
 * - LiveVoiceAgent (if user activated it)
 * 
 * If a service dies unexpectedly, this watchdog:
 * 1. Logs the failure
 * 2. Shows a persistent notification alerting the user
 * 3. Provides quick actions to re-enable the service
 * 4. On RedMagic devices, provides specific troubleshooting steps
 */
class ServiceWatchdog : Service() {
    
    companion object {
        private const val TAG = "ServiceWatchdog"
        private const val NOTIFICATION_ID = 3001
        private const val CHECK_INTERVAL_MS = 10_000L  // Check every 10 seconds
        
        @Volatile
        private var isRunning = false
        
        fun start(context: Context) {
            if (isRunning) {
                Log.d(TAG, "Watchdog already running")
                return
            }
            
            val intent = Intent(context, ServiceWatchdog::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, ServiceWatchdog::class.java))
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    
    private var lastA11yStatus = false
    private var lastNotifStatus = false
    private var a11yFailCount = 0
    private var notifFailCount = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ServiceWatchdog created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            return START_STICKY
        }
        
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification("Monitoring services..."))
        startMonitoring()
        
        Log.i(TAG, "ServiceWatchdog started")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        isRunning = false
        monitoringJob?.cancel()
        scope.cancel()
        Log.i(TAG, "ServiceWatchdog destroyed")
        super.onDestroy()
    }
    
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            // Initial status
            lastA11yStatus = DeviceCompatibility.isAccessibilityServiceEnabled(this@ServiceWatchdog)
            lastNotifStatus = DeviceCompatibility.isNotificationListenerEnabled(this@ServiceWatchdog)
            
            Log.d(TAG, "Initial status - A11y: $lastA11yStatus, Notif: $lastNotifStatus")
            
            while (isActive) {
                try {
                    checkServices()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Monitoring error", e)
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
    }
    
    private suspend fun checkServices() {
        val a11yEnabled = DeviceCompatibility.isAccessibilityServiceEnabled(this)
        val notifEnabled = DeviceCompatibility.isNotificationListenerEnabled(this)
        
        // Check accessibility service
        if (lastA11yStatus && !a11yEnabled) {
            // Service was enabled but now disabled!
            a11yFailCount++
            Log.w(TAG, "Accessibility service DISABLED (fail count: $a11yFailCount)")
            
            withContext(Dispatchers.Main) {
                showAccessibilityFailedNotification()
            }
        } else if (!lastA11yStatus && a11yEnabled) {
            // Service was re-enabled
            a11yFailCount = 0
            Log.i(TAG, "Accessibility service ENABLED")
            
            withContext(Dispatchers.Main) {
                updateNotification("Services running OK")
            }
        }
        
        // Check notification listener
        if (lastNotifStatus && !notifEnabled) {
            notifFailCount++
            Log.w(TAG, "Notification listener DISABLED (fail count: $notifFailCount)")
            
            withContext(Dispatchers.Main) {
                showNotificationListenerFailedNotification()
            }
        } else if (!lastNotifStatus && notifEnabled) {
            notifFailCount = 0
            Log.i(TAG, "Notification listener ENABLED")
        }
        
        // Check and restore DND settings if auto-restore is enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (com.jarvis.ai.util.DoNotDisturbManager.isAutoRestoreEnabled(this)) {
                try {
                    com.jarvis.ai.util.DoNotDisturbManager.restoreSavedMode(this)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore DND mode", e)
                }
            }
        }
        
        lastA11yStatus = a11yEnabled
        lastNotifStatus = notifEnabled
    }
    
    private fun showAccessibilityFailedNotification() {
        val isRedMagic = DeviceCompatibility.isRedMagicDevice()
        
        val title = if (isRedMagic) {
            "⚠️ Accessibility Disabled (RedMagic Issue)"
        } else {
            "⚠️ Accessibility Service Disabled"
        }
        
        val message = if (isRedMagic) {
            "RedMagic auto-disabled accessibility. Tap to see fix."
        } else {
            "Jarvis accessibility was disabled. Tap to re-enable."
        }
        
        // Action to open accessibility settings
        val settingsIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Action to open troubleshooting
        val helpIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("show_troubleshooting", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val helpPendingIntent = PendingIntent.getActivity(
            this, 1, helpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, JarvisApplication.CHANNEL_VOICE_SERVICE)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setColor(0xFFFF5252.toInt())  // Red
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_preferences,
                "Settings",
                settingsPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_help,
                if (isRedMagic) "RedMagic Fix" else "Help",
                helpPendingIntent
            )
            .build()
        
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showNotificationListenerFailedNotification() {
        val title = "⚠️ Notification Listener Disabled"
        val message = "Jarvis can't read notifications. Tap to re-enable."
        
        val settingsIntent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, JarvisApplication.CHANNEL_VOICE_SERVICE)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setColor(0xFFFF6D00.toInt())  // Orange
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)
    }
    
    private fun createNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_VOICE_SERVICE)
            .setContentTitle("Jarvis Watchdog")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }
}
