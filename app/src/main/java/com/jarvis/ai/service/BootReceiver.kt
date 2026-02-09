package com.jarvis.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jarvis.ai.util.PreferenceManager
import com.jarvis.ai.voice.WakeWordService

/**
 * BootReceiver — Auto-starts services on device boot.
 * 
 * Starts:
 * 1. ServiceWatchdog (always, to monitor accessibility)
 * 2. WakeWordService (if user enabled it)
 * 
 * This is especially important for RedMagic and other devices
 * that aggressively kill background services.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("BootReceiver", "Device boot completed — starting Jarvis services")

        try {
            // Always start the watchdog to monitor services
            ServiceWatchdog.start(context)
            Log.i("BootReceiver", "ServiceWatchdog started")
            
            // Start wake word service if user had it enabled
            val prefManager = PreferenceManager(context)
            if (prefManager.wakeWordEnabled && prefManager.picovoiceAccessKey.isNotBlank()) {
                Log.i("BootReceiver", "Starting wake word service after boot")
                WakeWordService.start(context)
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Failed to start services on boot", e)
        }
    }
}
