package com.jarvis.ai.ui.settings

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.databinding.ActivitySettingsBinding
import com.jarvis.ai.network.model.LlmProvider
import com.jarvis.ai.network.model.TtsProvider
import com.jarvis.ai.service.JarvisNotificationListener
import com.jarvis.ai.util.PreferenceManager
import com.jarvis.ai.voice.WakeWordService

/**
 * Settings UI for configuring API keys, providers, permissions,
 * wake word detection, and TTS mode (WebSocket vs HTTP).
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"

        /**
         * The AppOps constant for ACCESS_RESTRICTED_SETTINGS.
         * Not in the public SDK until later, but the string is stable since Android 13.
         */
        private const val OP_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"
    }

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefManager: PreferenceManager

    private val llmProviders = LlmProvider.entries.toTypedArray()
    private val ttsProviders = TtsProvider.entries.toTypedArray()

    // Language codes → display names
    private val languageCodes = arrayOf("bn-BD", "en-US", "en-IN", "hi-IN", "bn-IN", "ar-SA", "ur-PK")
    private val languageNames = arrayOf(
        "বাংলা (Bangla)",
        "English (US)",
        "English (India)",
        "हिन्दी (Hindi)",
        "বাংলা (India)",
        "العربية (Arabic)",
        "اردو (Urdu)"
    )

    // Voice sensitivity levels
    private val sensitivityLabels = arrayOf(
        "Low (quiet environment)",
        "Normal",
        "High (noisy environment)",
        "Max Focus"
    )

    private val voiceMoodLabels = arrayOf(
        "Normal",
        "Sad (slow)",
        "Happy (energetic)",
        "Romantic (soft)",
        "Angry (intense)",
        "Echo/Reverb"
    )

    private val memoryStorageLabels = arrayOf(
        "Internal Storage",
        "External SD Card"
    )

    private var suppressDeveloperSwitch = false

    // Theme presets
    private val themeLabels = arrayOf(
        "Jarvis Cyan (#00E5FF)",
        "Iron Man Red (#FF1744)",
        "Hulk Green (#00E676)",
        "Thanos Purple (#AA00FF)",
        "Custom (enter hex below)"
    )

    /**
     * Tracks which permission the user was trying to enable before being
     * redirected to App Info to allow restricted settings.
     * When they come back (onResume), we auto-retry opening that permission page.
     */
    private var pendingPermissionAction: PendingPermission? = null

    /** Flag: user went to App Info to allow restricted settings. */
    private var awaitingRestrictedSettingsReturn = false

    private enum class PendingPermission {
        ACCESSIBILITY,
        NOTIFICATION_LISTENER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupProviderSpinner()
        setupTtsSpinner()
        setupLanguageSpinners()
        setupThemeSpinner()
        setupDeveloperSwitch()
        loadSavedSettings()
        setupPermissionButtons()
        setupSaveButton()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()

        // --- Auto-retry: user just came back from App Info ---
        if (awaitingRestrictedSettingsReturn && pendingPermissionAction != null) {
            awaitingRestrictedSettingsReturn = false

            val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                areRestrictedSettingsAllowed()
            } else {
                true
            }

            if (allowed) {
                // Restricted settings are now allowed — go straight to the permission page
                when (pendingPermissionAction) {
                    PendingPermission.ACCESSIBILITY -> {
                        Toast.makeText(
                            this,
                            "Restricted settings allowed! Opening Accessibility...",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    PendingPermission.NOTIFICATION_LISTENER -> {
                        Toast.makeText(
                            this,
                            "Restricted settings allowed! Opening Notification Access...",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    null -> { /* nothing */ }
                }
                pendingPermissionAction = null
            } else {
                // Still not allowed — let the user know
                Toast.makeText(
                    this,
                    "Restricted settings still NOT allowed.\n" +
                        "\"Allow restricted settings\" অপশনটি এখনও চালু হয়নি।",
                    Toast.LENGTH_LONG
                ).show()
                pendingPermissionAction = null
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Setup                                                              //
    // ------------------------------------------------------------------ //

    private fun setupProviderSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            llmProviders.map { it.displayName }
        )
        binding.spinnerProvider.adapter = adapter

        binding.spinnerProvider.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = llmProviders[pos]
                // Show custom URL field for CUSTOM and FREEDOMGPT providers
                binding.layoutCustomUrl.visibility =
                    if (selected == LlmProvider.CUSTOM || selected == LlmProvider.FREEDOMGPT) View.VISIBLE else View.GONE

                // Auto-fill the URL when FreedomGPT is selected
                if (selected == LlmProvider.FREEDOMGPT) {
                    binding.etCustomUrl.setText(LlmProvider.FREEDOMGPT.defaultBaseUrl)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupTtsSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ttsProviders.map { it.displayName }
        )
        binding.spinnerTts.adapter = adapter
    }

    private fun setupThemeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeLabels)
        binding.spinnerTheme.adapter = adapter
    }

    private fun setupLanguageSpinners() {
        val langAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languageNames
        )
        binding.spinnerSttLanguage.adapter = langAdapter
        binding.spinnerTtsLanguage.adapter = langAdapter

        val sensitivityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            sensitivityLabels
        )
        binding.spinnerVoiceSensitivity.adapter = sensitivityAdapter

        val moodAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            voiceMoodLabels
        )
        binding.spinnerVoiceMood.adapter = moodAdapter

        val storageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            memoryStorageLabels
        )
        binding.spinnerMemoryStorage.adapter = storageAdapter
    }

    private fun setupDeveloperSwitch() {
        binding.switchDeveloperMode.setOnCheckedChangeListener { _, isChecked ->
            if (suppressDeveloperSwitch) return@setOnCheckedChangeListener
            if (isChecked) {
                showDeveloperPasswordDialog()
            } else {
                prefManager.developerModeEnabled = false
            }
        }
    }

    private fun loadSavedSettings() {
        // Provider
        val providerIndex = llmProviders.indexOf(prefManager.selectedLlmProvider)
        if (providerIndex >= 0) binding.spinnerProvider.setSelection(providerIndex)

        // TTS
        val ttsIndex = ttsProviders.indexOf(prefManager.selectedTtsProvider)
        if (ttsIndex >= 0) binding.spinnerTts.setSelection(ttsIndex)

        // Model
        binding.etModel.setText(prefManager.selectedModel)
        binding.etCustomUrl.setText(prefManager.customBaseUrl)

        // API Keys
        binding.etOpenRouterKey.setText(prefManager.openRouterApiKey)
        binding.etOpenAiKey.setText(prefManager.openAiApiKey)
        binding.etGeminiKey.setText(prefManager.geminiApiKey)
        binding.etClaudeKey.setText(prefManager.claudeApiKey)
        binding.etGroqKey.setText(prefManager.groqApiKey)
        binding.etFreedomGptKey.setText(prefManager.freedomGptApiKey)
        binding.etRevidKey.setText(prefManager.revidApiKey)

        // TTS Keys
        binding.etCartesiaKey.setText(prefManager.cartesiaApiKey)
        binding.etCartesiaVoiceId.setText(prefManager.cartesiaVoiceId)
        binding.switchCartesiaWs.isChecked = prefManager.useCartesiaWebSocket
        binding.etSpeechifyKey.setText(prefManager.speechifyApiKey)

        // Wake Word
        binding.switchWakeWord.isChecked = prefManager.wakeWordEnabled
        binding.etPicovoiceKey.setText(prefManager.picovoiceAccessKey)

        // Language & Voice
        val sttIndex = languageCodes.indexOf(prefManager.sttLanguage)
        if (sttIndex >= 0) binding.spinnerSttLanguage.setSelection(sttIndex)

        val ttsLangIndex = languageCodes.indexOf(prefManager.ttsLanguage)
        if (ttsLangIndex >= 0) binding.spinnerTtsLanguage.setSelection(ttsLangIndex)

        val sensIndex = prefManager.voiceSensitivity.coerceIn(0, sensitivityLabels.size - 1)
        binding.spinnerVoiceSensitivity.setSelection(sensIndex)

        binding.etSystemPrompt.setText(prefManager.customSystemPrompt)
        suppressDeveloperSwitch = true
        binding.switchDeveloperMode.isChecked = prefManager.developerModeEnabled
        suppressDeveloperSwitch = false
        val moodIndex = prefManager.emotionalVoicePreset.coerceIn(0, voiceMoodLabels.size - 1)
        binding.spinnerVoiceMood.setSelection(moodIndex)
        val storageIndex = if (prefManager.memoryStorage == "external") 1 else 0
        binding.spinnerMemoryStorage.setSelection(storageIndex)

        // Theme
        binding.spinnerTheme.setSelection(prefManager.themeIndex.coerceIn(0, themeLabels.size - 1))
        binding.etCustomAccentColor.setText(prefManager.customAccentColor)
    }

    // ------------------------------------------------------------------ //
    //  Permission Buttons — with Restricted Settings fix                  //
    // ------------------------------------------------------------------ //

    private fun setupPermissionButtons() {

        // ---------- ACCESSIBILITY ----------
        binding.btnEnableAccessibility.setOnClickListener {
            if (JarvisAccessibilityService.isRunning) {
                Toast.makeText(this, "Already enabled!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // On Android 13+, sideloaded APKs get blocked ("Restricted setting").
            // Check the AppOps flag first — only show the guide if NOT yet allowed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !areRestrictedSettingsAllowed()) {
                showRestrictedSettingsGuide(
                    permissionName = "Accessibility Service",
                    pendingPermission = PendingPermission.ACCESSIBILITY
                ) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            } else {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        // ---------- NOTIFICATION LISTENER ----------
        binding.btnEnableNotificationListener.setOnClickListener {
            if (JarvisNotificationListener.isRunning) {
                Toast.makeText(this, "Already enabled!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !areRestrictedSettingsAllowed()) {
                showRestrictedSettingsGuide(
                    permissionName = "Notification Access",
                    pendingPermission = PendingPermission.NOTIFICATION_LISTENER
                ) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            } else {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        // ---------- OVERLAY ----------
        binding.btnEnableOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        // ---------- ALLOW RESTRICTED SETTINGS (direct shortcut) ----------
        binding.btnAllowRestricted.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && areRestrictedSettingsAllowed()) {
                Toast.makeText(
                    this,
                    "Restricted settings already allowed!\n\u0987\u09A4\u09BF\u09AE\u09A7\u09CD\u09AF\u09C7 \u0985\u09A8\u09C1\u09AE\u09A4\u09BF \u09A6\u09C7\u0993\u09AF\u09BC\u09BE \u09B9\u09AF\u09BC\u09C7\u099B\u09C7!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            openAppInfoPage(pendingPermission = null)
        }
    }

    // ------------------------------------------------------------------ //
    //  Restricted Settings detection (Android 13+ / API 33+)              //
    // ------------------------------------------------------------------ //

    /**
     * Checks whether this app has been granted the
     * ACCESS_RESTRICTED_SETTINGS appop by the user.
     *
     * On Android 13 (TIRAMISU) and above, sideloaded APKs are blocked
     * from enabling Accessibility Service and Notification Listener
     * until the user explicitly allows "restricted settings" from App Info.
     *
     * Returns `true` if:
     *   - The device is below Android 13 (restriction doesn't exist), OR
     *   - The appop is MODE_ALLOWED.
     *
     * Returns `false` if the appop is not allowed or we can't determine.
     */
    private fun areRestrictedSettingsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true // restriction doesn't exist below Android 13
        }
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                OP_ACCESS_RESTRICTED_SETTINGS,
                android.os.Process.myUid(),
                packageName
            )
            Log.d(TAG, "ACCESS_RESTRICTED_SETTINGS mode=$mode (0=ALLOWED, 1=IGNORED, 2=ERRORED, 3=DEFAULT)")
            // MODE_ALLOWED = 0;  anything else means not yet allowed
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.w(TAG, "Could not check ACCESS_RESTRICTED_SETTINGS appop", e)
            // If we can't check, assume not allowed so the guide is shown
            false
        }
    }

    // ------------------------------------------------------------------ //
    //  Restricted Settings guide dialog                                    //
    // ------------------------------------------------------------------ //

    /**
     * Shows a detailed AlertDialog explaining HOW to fix the
     * "Restricted Setting" block on Android 13+.
     *
     * Includes step-by-step instructions in both English and Bangla.
     * If the user taps "Open App Info", we track [pendingPermission]
     * so that [onResume] can auto-navigate to the right settings page
     * after the user comes back.
     */
    private fun showRestrictedSettingsGuide(
        permissionName: String,
        pendingPermission: PendingPermission,
        onProceed: () -> Unit
    ) {
        val restrictedAllowed = areRestrictedSettingsAllowed()

        // If already allowed, just go straight to the settings page
        if (restrictedAllowed) {
            onProceed()
            return
        }

        val message = buildString {
            appendLine("Android 13+ e sideloaded app er jnyo ei permission block kore.")
            appendLine()
            appendLine("SOLVE korar upay:")
            appendLine()
            appendLine("STEP 1: Prothome ACCESSIBILITY/NOTIFICATION page e jan")
            appendLine("  - 'Go to Settings' button e tap koren")
            appendLine("  - Jarvis AI te tap koren (greyed out thakbe)")
            appendLine("  - 'Restricted setting' dialog ashbe -> OK tap koren")
            appendLine()
            appendLine("STEP 2: App Info page e jan")
            appendLine("  - Niche 'Open App Info' button e tap koren")
            appendLine("  - Uporer 3-dot menu (\u22EE) te tap koren")
            appendLine("  - 'Allow restricted settings' select koren")
            appendLine("  - PIN/Fingerprint dien")
            appendLine()
            appendLine("STEP 3: Abar ACCESSIBILITY/NOTIFICATION enable koren")
            appendLine("  - Fire eshe abar ei button e tap koren")
            appendLine("  - Ekhon enable korte parben!")
            appendLine()
            appendLine("IMPORTANT: Step 1 FIRST korte hobe! Na korle 3-dot menu te option dekhabe na.")
            appendLine()
            appendLine("═══ NUBIA / REDMAGIC PHONE: ═══")
            appendLine("Settings > Apps > Jarvis AI > Permissions > ALL allow koren")
            appendLine("Game Space OFF koren (Game Space block kore)")
            appendLine("Battery > Unrestricted select koren")
            appendLine()
            appendLine("═══ ADB BYPASS (PC theke): ═══")
            appendLine("adb shell appops set com.jarvis.ai ACCESS_RESTRICTED_SETTINGS allow")
            appendLine("adb shell settings put secure enabled_accessibility_services com.jarvis.ai/.accessibility.JarvisAccessibilityService")
            appendLine("adb shell settings put secure accessibility_enabled 1")
        }

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
            .setTitle("Android 13 Restricted Settings Fix")
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                // User claims they already allowed — try directly
                onProceed()
            }
            .setNeutralButton("Open App Info") { _, _ ->
                openAppInfoPage(pendingPermission)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    /**
     * Opens the system App Info page for this package.
     *
     * If [pendingPermission] is non-null, we remember it so that
     * [onResume] can automatically navigate to the correct settings
     * page once the user returns after allowing restricted settings.
     */
    private fun openAppInfoPage(pendingPermission: PendingPermission?) {
        this.pendingPermissionAction = pendingPermission
        this.awaitingRestrictedSettingsReturn = pendingPermission != null

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)

            if (pendingPermission != null) {
                Toast.makeText(
                    this,
                    "\u22EE \u09AE\u09C7\u09A8\u09C1 \u09A5\u09C7\u0995\u09C7 \"Allow restricted settings\" \u099A\u09BE\u09B2\u09C1 \u0995\u09B0\u09C1\u09A8\n" +
                        "Tap \u22EE menu > \"Allow restricted settings\"",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not open App Info", e)
            Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
            this.pendingPermissionAction = null
            this.awaitingRestrictedSettingsReturn = false
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        // Root auto-enable button
        binding.btnRootEnable?.setOnClickListener {
            rootAutoEnable()
        }
    }
    
    private fun rootAutoEnable() {
        AlertDialog.Builder(this)
            .setTitle("Root Auto-Enable")
            .setMessage("Phone rooted hole automatic Accessibility + Notification ON korbe.\n\nRoot access lagbe — su permission dite hobe.\n\nContinue?")
            .setPositiveButton("Yes, Enable") { _, _ ->
                Thread {
                    try {
                        // Enable Accessibility Service
                        val cmd1 = "settings put secure enabled_accessibility_services com.jarvis.ai/.accessibility.JarvisAccessibilityService"
                        val cmd2 = "settings put secure accessibility_enabled 1"
                        
                        // Enable Notification Listener
                        val cmd3 = "settings put secure enabled_notification_listeners com.jarvis.ai/.service.JarvisNotificationListener"
                        
                        // Allow restricted settings
                        val cmd4 = "appops set com.jarvis.ai ACCESS_RESTRICTED_SETTINGS allow"
                        
                        val commands = arrayOf(cmd1, cmd2, cmd3, cmd4)
                        
                        for (cmd in commands) {
                            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                            val exit = process.waitFor()
                            Log.d(TAG, "Root cmd: $cmd [exit=$exit]")
                        }
                        
                        runOnUiThread {
                            Toast.makeText(this, "Root commands executed! Restart app to verify.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Root enable failed", e)
                        runOnUiThread {
                            Toast.makeText(this, "Root failed: ${e.message}. Phone rooted ache?", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeveloperPasswordDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Enter password"
        }
        AlertDialog.Builder(this)
            .setTitle("Developer Mode")
            .setMessage("Password lagbe (default: piashkw)")
            .setView(input)
            .setPositiveButton("Unlock") { dialog, _ ->
                val text = input.text.toString()
                if (text == "piashkw") {
                    prefManager.developerModeEnabled = true
                    Toast.makeText(this, "Developer mode enabled", Toast.LENGTH_SHORT).show()
                } else {
                    suppressDeveloperSwitch = true
                    binding.switchDeveloperMode.isChecked = false
                    suppressDeveloperSwitch = false
                    prefManager.developerModeEnabled = false
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                suppressDeveloperSwitch = true
                binding.switchDeveloperMode.isChecked = false
                suppressDeveloperSwitch = false
                prefManager.developerModeEnabled = false
                dialog.dismiss()
            }
            .setOnCancelListener {
                suppressDeveloperSwitch = true
                binding.switchDeveloperMode.isChecked = false
                suppressDeveloperSwitch = false
                prefManager.developerModeEnabled = false
            }
            .show()
    }

    // ------------------------------------------------------------------ //
    //  Save                                                               //
    // ------------------------------------------------------------------ //

    private fun saveSettings() {
        // Provider
        prefManager.selectedLlmProvider = llmProviders[binding.spinnerProvider.selectedItemPosition]
        prefManager.selectedTtsProvider = ttsProviders[binding.spinnerTts.selectedItemPosition]

        // Model
        prefManager.selectedModel = binding.etModel.text.toString().trim()
        prefManager.customBaseUrl = binding.etCustomUrl.text.toString().trim()

        // API Keys
        prefManager.openRouterApiKey = binding.etOpenRouterKey.text.toString().trim()
        prefManager.openAiApiKey = binding.etOpenAiKey.text.toString().trim()
        prefManager.geminiApiKey = binding.etGeminiKey.text.toString().trim()
        prefManager.claudeApiKey = binding.etClaudeKey.text.toString().trim()
        prefManager.groqApiKey = binding.etGroqKey.text.toString().trim()
        prefManager.freedomGptApiKey = binding.etFreedomGptKey.text.toString().trim()
        prefManager.revidApiKey = binding.etRevidKey.text.toString().trim()

        // TTS Keys
        prefManager.cartesiaApiKey = binding.etCartesiaKey.text.toString().trim()
        prefManager.cartesiaVoiceId = binding.etCartesiaVoiceId.text.toString().trim()
        prefManager.useCartesiaWebSocket = binding.switchCartesiaWs.isChecked
        prefManager.speechifyApiKey = binding.etSpeechifyKey.text.toString().trim()

        // Wake Word
        val wakeWordWasEnabled = prefManager.wakeWordEnabled
        prefManager.wakeWordEnabled = binding.switchWakeWord.isChecked
        prefManager.picovoiceAccessKey = binding.etPicovoiceKey.text.toString().trim()

        // Language & Voice
        prefManager.sttLanguage = languageCodes[binding.spinnerSttLanguage.selectedItemPosition]
        prefManager.ttsLanguage = languageCodes[binding.spinnerTtsLanguage.selectedItemPosition]
        prefManager.voiceSensitivity = binding.spinnerVoiceSensitivity.selectedItemPosition
        prefManager.customSystemPrompt = binding.etSystemPrompt.text.toString().trim()
        prefManager.developerModeEnabled = binding.switchDeveloperMode.isChecked
        prefManager.emotionalVoicePreset = binding.spinnerVoiceMood.selectedItemPosition
        prefManager.memoryStorage = if (binding.spinnerMemoryStorage.selectedItemPosition == 1) "external" else "internal"

        // Theme
        prefManager.themeIndex = binding.spinnerTheme.selectedItemPosition
        val customColor = binding.etCustomAccentColor.text.toString().trim()
        if (customColor.startsWith("#") && customColor.length >= 7) {
            prefManager.customAccentColor = customColor
        }

        // Start/stop wake word service based on toggle
        if (binding.switchWakeWord.isChecked && prefManager.picovoiceAccessKey.isNotBlank()) {
            if (!WakeWordService.isRunning) {
                WakeWordService.start(this)
                Toast.makeText(this, "Wake word detection started", Toast.LENGTH_SHORT).show()
            }
        } else if (wakeWordWasEnabled && !binding.switchWakeWord.isChecked) {
            WakeWordService.stop(this)
            Toast.makeText(this, "Wake word detection stopped", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ------------------------------------------------------------------ //
    //  Permission Status                                                  //
    // ------------------------------------------------------------------ //

    private fun updatePermissionStatuses() {
        val a11yEnabled = JarvisAccessibilityService.isRunning
        binding.btnEnableAccessibility.text = if (a11yEnabled) {
            "Accessibility: ENABLED"
        } else {
            "Enable Accessibility Service"
        }

        val notifEnabled = JarvisNotificationListener.isRunning
        binding.btnEnableNotificationListener.text = if (notifEnabled) {
            "Notification Access: ENABLED"
        } else {
            "Enable Notification Access"
        }

        val overlayEnabled = Settings.canDrawOverlays(this)
        binding.btnEnableOverlay.text = if (overlayEnabled) {
            "Overlay Permission: ENABLED"
        } else {
            "Enable Overlay Permission"
        }

        // Show/hide the restricted settings button based on Android version.
        // Also update its label to reflect current status.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.btnAllowRestricted.visibility = View.VISIBLE
            val restricted = areRestrictedSettingsAllowed()
            binding.btnAllowRestricted.text = if (restricted) {
                "Restricted Settings: ALLOWED \u2705"
            } else {
                "Allow Restricted Settings (\u26A0\uFE0F Required)"
            }
        } else {
            binding.btnAllowRestricted.visibility = View.GONE
        }
    }
}
