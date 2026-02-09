package com.jarvis.ai.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Device Compatibility Utilities
 * 
 * Handles manufacturer-specific quirks and workarounds, especially for:
 * - RedMagic devices (nubia/ZTE)
 * - MIUI (Xiaomi)
 * - ColorOS (OPPO)
 * - FunTouch OS (vivo)
 * - One UI (Samsung)
 * 
 * These manufacturers often have aggressive battery optimization and
 * custom permission systems that interfere with accessibility services
 * and notification listeners.
 */
object DeviceCompatibility {
    
    private const val TAG = "DeviceCompat"
    
    /**
     * Device manufacturer brands known to have accessibility issues
     */
    private val PROBLEMATIC_MANUFACTURERS = setOf(
        "redmagic", "nubia", "zte",  // RedMagic devices
        "xiaomi", "redmi", "poco",    // MIUI
        "oppo", "realme", "oneplus",  // ColorOS/OxygenOS
        "vivo", "iqoo",               // FunTouch OS
        "huawei", "honor",            // EMUI
        "samsung"                     // One UI (less problematic but still needs handling)
    )
    
    /**
     * Detect if device is a RedMagic (nubia/ZTE gaming phone)
     */
    fun isRedMagicDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        
        return manufacturer in setOf("redmagic", "nubia", "zte") ||
               brand in setOf("redmagic", "nubia", "zte") ||
               model.contains("redmagic") || model.contains("nx") // RedMagic models start with NX
    }
    
    /**
     * Detect if device is from a manufacturer with known accessibility issues
     */
    fun hasAccessibilityIssues(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return manufacturer in PROBLEMATIC_MANUFACTURERS || 
               brand in PROBLEMATIC_MANUFACTURERS
    }
    
    /**
     * Get device-specific recommendations for fixing accessibility
     */
    fun getAccessibilityFixInstructions(context: Context): String {
        return buildString {
            appendLine("📱 Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            
            when {
                isRedMagicDevice() -> {
                    appendLine("🎮 RedMagic Device Detected")
                    appendLine()
                    appendLine("RedMagic devices have aggressive battery optimization.")
                    appendLine("Follow these steps:")
                    appendLine()
                    appendLine("1. DISABLE BATTERY OPTIMIZATION:")
                    appendLine("   Settings → Battery → Battery Optimization")
                    appendLine("   → Find 'Jarvis AI' → Don't Optimize")
                    appendLine()
                    appendLine("2. ENABLE AUTOSTART:")
                    appendLine("   Settings → Apps → Autostart")
                    appendLine("   → Enable 'Jarvis AI'")
                    appendLine()
                    appendLine("3. LOCK APP IN RECENTS:")
                    appendLine("   Recent Apps → Find Jarvis → Lock icon")
                    appendLine()
                    appendLine("4. ACCESSIBILITY SETTINGS:")
                    appendLine("   Settings → Accessibility → Installed Services")
                    appendLine("   → Enable 'Jarvis AI'")
                    appendLine("   → Grant all permissions")
                    appendLine()
                    appendLine("5. IF ACCESSIBILITY KEEPS TURNING OFF:")
                    appendLine("   Use ADB commands (connect via USB):")
                    appendLine("   adb shell settings put secure enabled_accessibility_services com.jarvis.ai/.accessibility.JarvisAccessibilityService")
                    appendLine("   adb shell settings put secure accessibility_enabled 1")
                }
                
                Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco") -> {
                    appendLine("📱 MIUI Device Detected")
                    appendLine()
                    appendLine("1. Security → Permissions → Autostart")
                    appendLine("   → Enable Jarvis AI")
                    appendLine()
                    appendLine("2. Battery & Performance → Battery Saver")
                    appendLine("   → Add Jarvis AI to exceptions")
                    appendLine()
                    appendLine("3. Settings → Notifications")
                    appendLine("   → Allow all notifications for Jarvis AI")
                }
                
                Build.MANUFACTURER.lowercase() in setOf("oppo", "realme", "oneplus") -> {
                    appendLine("📱 ColorOS/OxygenOS Device Detected")
                    appendLine()
                    appendLine("1. Settings → Battery → Battery Optimization")
                    appendLine("   → Don't optimize Jarvis AI")
                    appendLine()
                    appendLine("2. Settings → App Management → App Auto-Launch")
                    appendLine("   → Enable Jarvis AI")
                }
                
                Build.MANUFACTURER.lowercase() in setOf("vivo", "iqoo") -> {
                    appendLine("📱 FunTouch OS Device Detected")
                    appendLine()
                    appendLine("1. i Manager → App Manager → Autostart")
                    appendLine("   → Enable Jarvis AI")
                    appendLine()
                    appendLine("2. Settings → Battery → Background Activity")
                    appendLine("   → Allow Jarvis AI")
                }
                
                Build.MANUFACTURER.lowercase() == "samsung" -> {
                    appendLine("📱 Samsung Device Detected")
                    appendLine()
                    appendLine("1. Settings → Apps → Jarvis AI → Battery")
                    appendLine("   → Set to 'Unrestricted'")
                    appendLine()
                    appendLine("2. Settings → Device Care → Battery")
                    appendLine("   → App Power Management → Add Jarvis AI to 'Never sleeping apps'")
                }
                
                else -> {
                    appendLine("⚙️ Generic Android Device")
                    appendLine()
                    appendLine("1. Settings → Apps → Special App Access")
                    appendLine("   → Battery Optimization → Don't optimize Jarvis AI")
                    appendLine()
                    appendLine("2. Settings → Accessibility")
                    appendLine("   → Enable Jarvis Accessibility Service")
                }
            }
            
            appendLine()
            appendLine("💡 TIP: Keep the app open in background")
            appendLine("and don't swipe it away from recent apps.")
        }
    }
    
    /**
     * Check if Do Not Disturb access is granted
     */
    fun hasDoNotDisturbAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.isNotificationPolicyAccessGranted
        } else {
            true // No DND on older versions
        }
    }
    
    /**
     * Request Do Not Disturb access
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
     * Check if notification listener is enabled
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return listeners.contains(packageName)
    }
    
    /**
     * Open notification listener settings
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings", e)
        }
    }
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/.accessibility.JarvisAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServices.contains(expectedService)
    }
    
    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }
    
    /**
     * Get ADB commands to force-enable accessibility service
     * Useful for devices that keep disabling it
     */
    fun getAdbCommands(context: Context): String {
        val packageName = context.packageName
        return buildString {
            appendLine("# Connect device via USB with USB Debugging enabled")
            appendLine("# Then run these commands in terminal/command prompt:")
            appendLine()
            appendLine("# Enable accessibility service:")
            appendLine("adb shell settings put secure enabled_accessibility_services $packageName/.accessibility.JarvisAccessibilityService")
            appendLine()
            appendLine("# Enable accessibility globally:")
            appendLine("adb shell settings put secure accessibility_enabled 1")
            appendLine()
            appendLine("# Disable battery optimization:")
            appendLine("adb shell dumpsys deviceidle whitelist +$packageName")
            appendLine()
            appendLine("# Grant notification listener permission:")
            appendLine("adb shell cmd notification allow_listener $packageName/com.jarvis.ai.service.JarvisNotificationListener")
        }
    }
    
    /**
     * Create a summary of current permission status
     */
    fun getPermissionStatus(context: Context): String {
        return buildString {
            appendLine("📊 Permission Status:")
            appendLine()
            appendLine("✓ = Granted  ✗ = Missing")
            appendLine()
            
            val hasA11y = isAccessibilityServiceEnabled(context)
            val hasNotif = isNotificationListenerEnabled(context)
            val hasDnd = hasDoNotDisturbAccess(context)
            
            appendLine("${if (hasA11y) "✓" else "✗"} Accessibility Service")
            appendLine("${if (hasNotif) "✓" else "✗"} Notification Listener")
            appendLine("${if (hasDnd) "✓" else "✗"} Do Not Disturb Access")
            
            appendLine()
            if (!hasA11y || !hasNotif || !hasDnd) {
                appendLine("⚠️  Some permissions are missing!")
                appendLine("Jarvis won't work properly without them.")
            } else {
                appendLine("✅ All critical permissions granted!")
            }
        }
    }
}
