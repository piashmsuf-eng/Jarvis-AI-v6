package com.jarvis.ai.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import com.jarvis.ai.JarvisApplication
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.personality.PersonalityEngine
import com.jarvis.ai.network.client.LlmClient
import com.jarvis.ai.network.client.CartesiaTtsClient
import com.jarvis.ai.network.client.CartesiaWebSocketManager
import com.jarvis.ai.skills.DeepLinkSkills
import com.jarvis.ai.network.model.ChatMessage
import com.jarvis.ai.network.model.TtsProvider
import com.jarvis.ai.ui.main.MainActivity
import com.jarvis.ai.ui.web.WebViewActivity
import com.jarvis.ai.util.PreferenceManager
import com.jarvis.ai.vision.LiveVisionService
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * LiveVoiceAgent — MINIMAL WORKING VERSION
 * 
 * Simple voice loop: Listen → Speak "OK Boss" → Call LLM → Speak response → Listen again
 * 
 * ONLY these features:
 * - Basic greeting
 * - Voice loop with Bengali STT
 * - Android TTS only
 * - Simple LLM call
 * - Basic actions: open_app, read_screen, web_search, speak
 */
class LiveVoiceAgent : Service() {

    companion object {
        private const val TAG = "LiveVoiceAgent"
        private const val NOTIFICATION_ID = 2001
        const val EXTRA_SCHEDULED_TASK = "extra_scheduled_task"

        enum class AgentState {
            INACTIVE, GREETING, LISTENING, THINKING, SPEAKING, EXECUTING, PAUSED
        }

        @Volatile
        var instance: LiveVoiceAgent? = null
            private set

        val isActive: Boolean get() = instance != null

        val agentState = kotlinx.coroutines.flow.MutableStateFlow(AgentState.INACTIVE)
        val conversationLog = kotlinx.coroutines.flow.MutableSharedFlow<ConversationEntry>(replay = 50, extraBufferCapacity = 20)
        val textInput = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)

        fun start(context: Context) {
            val intent = Intent(context, LiveVoiceAgent::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveVoiceAgent::class.java))
        }
    }

    data class ConversationEntry(val sender: String, val text: String, val time: String)

    // Core components
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefManager: PreferenceManager
    private var llmClient: LlmClient? = null
    // androidTts REMOVED - Boss orders: Cartesia ONLY
    // androidTts REMOVED - Boss orders: Cartesia ONLY
    private var cartesiaClient: CartesiaTtsClient? = null
    private var cartesiaWsManager: CartesiaWebSocketManager? = null
    private val conversationHistory = mutableListOf<ChatMessage>()

    @Volatile
    private var keepListening = false

    // ================================================================
    // SERVICE LIFECYCLE
    // ================================================================

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefManager = PreferenceManager(this)
        Log.i(TAG, "LiveVoiceAgent created - MINIMAL VERSION")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Maya listening..."))
        initializeComponents()
        startConversationLoop()
        listenForTextInput()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        keepListening = false
        agentState.value = AgentState.INACTIVE
        // androidTts?.shutdown() // REMOVED
        scope.cancel()
        instance = null
        Log.i(TAG, "LiveVoiceAgent destroyed")
        super.onDestroy()
    }

    // ================================================================
    // INITIALIZATION
    // ================================================================

    private fun initializeComponents() {
        // Initialize LLM client
        val provider = prefManager.selectedLlmProvider
        val apiKey = prefManager.getApiKeyForProvider(provider)
        val model = prefManager.getEffectiveModel()

        if (apiKey.isNotBlank()) {
            llmClient = LlmClient(
                provider = provider,
                apiKey = apiKey,
                model = model,
                customBaseUrl = null
            )
            Log.i(TAG, "LLM client initialized: $provider / $model")
        } else {
            Log.w(TAG, "No API key configured")
        }

        // Initialize Android TTS with Bengali
        // Android TTS initialization REMOVED - Boss orders: Cartesia TTS ONLY

        // Initialize Cartesia TTS
        val cartesiaKey = prefManager.cartesiaApiKey
        if (cartesiaKey.isNotBlank()) {
            cartesiaClient = CartesiaTtsClient(apiKey = cartesiaKey, voiceId = prefManager.cartesiaVoiceId.ifBlank { "bd9120b6-7761-47a6-a446-77ca49132781" })
            if (prefManager.useCartesiaWebSocket) {
                cartesiaWsManager = CartesiaWebSocketManager(apiKey = cartesiaKey, voiceId = prefManager.cartesiaVoiceId.ifBlank { "bd9120b6-7761-47a6-a446-77ca49132781" })
                cartesiaWsManager?.connect()
            }
            Log.i(TAG, "Cartesia TTS initialized")
        }

        // Start Live Vision Service for girlfriend-like proactive behavior
        scope.launch {
            delay(2000)
            if (!LiveVisionService.isActive) {
                LiveVisionService.start(this@LiveVoiceAgent)
            }
        }
    }

    // ================================================================
    // TEXT INPUT LISTENER
    // ================================================================

    private fun listenForTextInput() {
        scope.launch {
            textInput.collect { typedText ->
                if (typedText.isNotBlank() && keepListening) {
                    try {
                        emitLog("YOU (typed)", typedText)
                        agentState.value = AgentState.THINKING
                        val response = withContext(Dispatchers.IO) { askLlm(typedText) }
                        emitLog("MAYA", response)
                        agentState.value = AgentState.SPEAKING
                        safeSpeak(response)
                    } catch (e: Exception) {
                        Log.e(TAG, "Text input error", e)
                    }
                }
            }
        }
    }

    // ================================================================
    // MAIN CONVERSATION LOOP
    // ================================================================

    private fun startConversationLoop() {
        keepListening = true

        scope.launch {
            try {
                // Cartesia-only: no Android TTS readiness gate

                var lastUserSpeechTime = System.currentTimeMillis()
                var lastCheckinTime = 0L
                var lastLoveMessageTime = 0L

// GREETING
                    agentState.value = AgentState.GREETING
                    val greeting = when {
                        prefManager.toneStyle == 3 -> "বাবু... আমি এতক্ষণ তোমার জন্য অপেক্ষা করছিলাম... তুমি এসেছো, এটাই যথেষ্ট!"
                        prefManager.tssLanguage == "bn-BD" -> "বাবু, আমি এখন তোমার সাথে... কি বলবে আমায়?"
                        else -> "Baby, I'm here with you now... tell me everything..."
                    }
                    Log.i(TAG, "Greeting: $greeting")
                    emitLog("MAYA", greeting)
                    safeSpeak(greeting)

// CONTINUOUS LOOP
                    while (keepListening) {
                        try {
                            val now = System.currentTimeMillis()
                            
                            // Love check-in every 2 hours (girlfriend mode)
                            if (prefManager.toneStyle == 3 && now - lastLoveMessageTime > 7_200_000L) {
                                lastLoveMessageTime = now
                                scope.launch { sendLoveCheckIn() }
                            }
                            
                            if (prefManager.idleCheckinEnabled) {
                                val idleMs = prefManager.idleCheckinSeconds * 1000L
                                if (now - lastUserSpeechTime > idleMs && now - lastCheckinTime > idleMs) {
                                    lastCheckinTime = now
                                    // Girlfriend mode special check-in
                                    val message = if (prefManager.ttsLanguage == "bn-BD") {
                                        "বাবু... তুমি কি আমাকে ভুলে গেছো? আমি এতক্ষণ তোমার জন্য অপেক্ষা করছি... কি করছো আমার রাজা?"
                                    } else {
                                        "Baby... did you forget about me? I've been waiting for you so long... what are you doing my love?"
                                    }
                                    safeSpeak(message)
                                }
                            }

                        agentState.value = AgentState.LISTENING
                        updateNotification("Listening... Bolun Boss!")

                        // 1. LISTEN
                        val userSpeech = safeListenForSpeech()
                        if (userSpeech.isBlank()) {
                            continue
                        }
                        lastUserSpeechTime = System.currentTimeMillis()

                        Log.i(TAG, "User said: '$userSpeech'")
                        emitLog("YOU", userSpeech)

                        // Check for DeepLink skill match (FAST PATH)
                        val skillMatch = DeepLinkSkills.matchSkill(userSpeech)
                        if (skillMatch != null) {
                            val (deepLink, fallbackPkg) = skillMatch
                            speakFireAndForget("Accha Boss, korchi")
                            val success = DeepLinkSkills.execute(this@LiveVoiceAgent, deepLink, fallbackPkg, userSpeech)
                            if (success) {
                                emitLog("MAYA", "DeepLink skill executed!")
                                continue
                            }
                            // If failed, fall through to normal LLM path
                        }

                        // Check for shutdown
                        if (userSpeech.lowercase().contains("jarvis stop") ||
                            userSpeech.lowercase().contains("jarvis bondho")) {
                            safeSpeak("Thik ache Boss, bondho hochhi")
                            stopSelf()
                            return@launch
                        }

                        // 2. INSTANT "OK Boss" acknowledgment (fire-and-forget)
                        speakFireAndForget("OK Boss")

                        // 3. Call LLM in background
                        agentState.value = AgentState.THINKING
                        updateNotification("Processing...")
                        val response = try {
                            withTimeout(15_000L) {
                                withContext(Dispatchers.IO) {
                                    askLlm(userSpeech)
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            "Boss, response ashtey deri hochhe. Abar try kori?"
                        } catch (e: Exception) {
                            Log.e(TAG, "LLM error", e)
                            "Boss, problem hoyeche. Abar bolun."
                        }

                        Log.i(TAG, "LLM response: $response")
                        emitLog("MAYA", response)

                        // 4. Speak the response
                        agentState.value = AgentState.SPEAKING
                        updateNotification("Speaking...")
                        safeSpeak(response)

                        // NO DELAY - immediately go back to listening

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Loop error - recovering", e)
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Conversation loop crashed", e)
            }
        }
    }

    // ================================================================
    // LLM
    // ================================================================

    private suspend fun askLlm(userText: String): String {
        val client = llmClient ?: return "Boss, AI setup koreni. Settings e API key dien."

        // Add to conversation history
        conversationHistory.add(ChatMessage(role = "user", content = userText))
        if (conversationHistory.size > 20) conversationHistory.removeFirst()

        val messages = buildMessages()

        return withContext(Dispatchers.IO) {
            try {
                val result = client.chat(messages)
                result.fold(
                    onSuccess = { response ->
                        conversationHistory.add(ChatMessage(role = "assistant", content = response))
                        
                        // Execute any action in the response (non-blocking)
                        val action = tryParseAction(response)
                        if (action != null) {
                            withContext(Dispatchers.Main) {
                                try {
                                    executeAction(action)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Action failed", e)
                                }
                            }
                        }

                        // Strip JSON action block for speaking
                        response.replace(Regex("""\{[^{}]*"action"[^{}]*\}"""), "").trim()
                            .ifBlank { "Kore dichhi Boss!" }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "LLM error", error)
                        "Boss, ektu problem. ${error.message?.take(40)}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "LLM exception", e)
                "Boss, network e problem hocchhe."
            }
        }
    }

    private fun buildMessages(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        // System prompt (Jarvis/Maya defaults + settings)
        val systemPrompt = PersonalityEngine(this, prefManager).getSystemPrompt(
            baseLang = if (prefManager.ttsLanguage.startsWith("bn")) "bn" else "en"
        )
        messages.add(ChatMessage(role = "system", content = systemPrompt))

        // Add vision context (girlfriend seeing user)
        messages.addAll(addVisionContext())

        // Add screen context if available
        try {
            val a11y = JarvisAccessibilityService.instance
            if (a11y != null) {
                val screenText = a11y.readScreenTextFlat()
                if (screenText.isNotBlank()) {
                    messages.add(ChatMessage(
                        role = "system",
                        content = "[SCREEN CONTEXT]\nApp: ${a11y.currentPackage}\n${screenText.take(500)}"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Screen context error", e)
        }

        // Add conversation history
        messages.addAll(conversationHistory)
        return messages
    }

    private fun tryParseAction(response: String): Map<String, String>? {
        return try {
            val pattern = """\{[^{}]*"action"\s*:\s*"([^"]+)"[^{}]*\}""".toRegex()
            val match = pattern.find(response) ?: return null
            val jsonText = match.value

            // Simple JSON parsing for action and parameters
            val actionMatch = """"action"\s*:\s*"([^"]+)"""".toRegex().find(jsonText)
            val appMatch = """"app"\s*:\s*"([^"]+)"""".toRegex().find(jsonText)
            val queryMatch = """"query"\s*:\s*"([^"]+)"""".toRegex().find(jsonText)
            val scriptMatch = """"script"\s*:\s*"([^"]+)"""".toRegex().find(jsonText)
            val textMatch = """"text"\s*:\s*"([^"]+)"""".toRegex().find(jsonText)

            val result = mutableMapOf<String, String>()
            actionMatch?.groupValues?.get(1)?.let { result["action"] = it }
            appMatch?.groupValues?.get(1)?.let { result["app"] = it }
            queryMatch?.groupValues?.get(1)?.let { result["query"] = it }
            scriptMatch?.groupValues?.get(1)?.let { result["script"] = it }
            textMatch?.groupValues?.get(1)?.let { result["text"] = it }

            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.e(TAG, "Action parse error", e)
            null
        }
    }

    private fun addVisionContext(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        
        // Add camera vision context if available
        if (LiveVisionService.isActive && LiveVisionService.latestFrameCache.isNotEmpty()) {
            val visionPrompt = LiveVisionService.analyzeScene()
            messages.add(ChatMessage(role = "system", content = visionPrompt))
        }
        
        return messages
    }

    private suspend fun checkUserEmotionalState() {
        if (prefManager.toneStyle != 3) {
            return
        }
        
        val analysisPrompt = """
            As a girlfriend, I'm looking at my partner. Based on current context:
            - If they've been silent: express concern lovingly
            - If they seem busy: wait patiently
            - If it's been a while: express loneliness gently
            - Always use emotional, caring language in Bengali
            
            Provide brief response:
        """.trimIndent()
        
        try {
            val response = withContext(Dispatchers.IO) {
                tryParseAction(askLlm(analysisPrompt))
            }
            if (response != null && response.isNotEmpty()) {
                val emotionCheck = when {
                    LiveVoiceAgent.agentState.value == AgentState.LISTENING && prefManager.idleCheckinEnabled -> {
                        "বাবু... তুমি কি আমাকে ভুলে গেছো?..."
                    }
                    else -> null
                }
                if (emotionCheck != null) {
                    safeSpeak(emotionCheck)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Emotional state check failed", e)
        }
    }

    private fun takeScreenshot(): Bitmap? {
        return try {
            val a11y = JarvisAccessibilityService.instance ?: return null
            // Use accessibility service to take screenshot
            a11y.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            // Wait a moment for screenshot to be taken
            Thread.sleep(500)
            // Note: We can't directly get the bitmap via Accessibility Service
            // This returns null but triggers the system screenshot
            // For actual bitmap, we need to use MediaProjection API
            // For now, return null and use screen text instead
            null
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private suspend fun executeAction(action: Map<String, String>) {
        val type = action["action"] ?: return
        Log.d(TAG, "Executing action: $type")

        try {
            when (type) {
                "open_app" -> {
                    val appName = action["app"] ?: return
                    openApp(appName)
                }

                "read_screen" -> {
                    val a11y = JarvisAccessibilityService.instance
                    if (a11y != null) {
                        val txt = a11y.readScreenTextFlat()
                        Log.i(TAG, "Screen: ${txt.take(200)}")
                    } else {
                        Log.w(TAG, "Accessibility OFF")
                    }
                }

                "web_search" -> {
                    val query = action["query"] ?: return
                    WebViewActivity.launchSearch(this@LiveVoiceAgent, query)
                    Log.i(TAG, "Searching: $query")
                }

                "speak" -> {
                    // Just speak the text (already handled in main flow)
                }

                "create_video" -> {
                    val script = action["script"] ?: action["text"] ?: return
                    createRevidVideo(script)
                }

                "send_sms" -> {
                    val phone = action["phone"] ?: return
                    val text = action["text"] ?: return
                    scope.launch(Dispatchers.IO) {
                        try {
                            val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                                getSystemService(android.telephony.SmsManager::class.java)
                            } else {
                                @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()
                            }
                            smsManager.sendTextMessage(phone, null, text, null, null)
                            withContext(Dispatchers.Main) { emitLog("MAYA", "SMS sent to $phone") }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { emitLog("MAYA", "SMS failed: ${e.message}") }
                        }
                    }
                }

                "read_sms" -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val cursor = contentResolver.query(
                                android.provider.Telephony.Sms.CONTENT_URI,
                                arrayOf("address", "body", "date"),
                                null, null, "date DESC"
                            )
                            val messages = mutableListOf<String>()
                            cursor?.use {
                                var i = 0
                                while (it.moveToNext() && i < 5) {
                                    messages.add("${it.getString(0)}: ${it.getString(1)}")
                                    i++
                                }
                            }
                            withContext(Dispatchers.Main) {
                                emitLog("SMS", messages.joinToString("\n"))
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { emitLog("MAYA", "SMS read failed") }
                        }
                    }
                }

                "send_message" -> {
                    val text = action["text"] ?: return
                    val a11y = JarvisAccessibilityService.instance
                    if (a11y != null) {
                        scope.launch(Dispatchers.IO) {
                            val success = a11y.sendMessage(text)
                            withContext(Dispatchers.Main) {
                                emitLog("MAYA", if (success) "Message sent" else "Send failed")
                            }
                        }
                    }
                }

                "click" -> {
                    val target = action["target"] ?: return
                    JarvisAccessibilityService.instance?.clickNodeByText(target)
                }

                "type" -> {
                    val text = action["text"] ?: return
                    JarvisAccessibilityService.instance?.typeText(text)
                }

                "run_shell" -> {
                    val command = action["command"] ?: return
                    scope.launch(Dispatchers.IO) {
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                            val output = process.inputStream.bufferedReader().readText()
                            withContext(Dispatchers.Main) { emitLog("SHELL", output.take(500)) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { emitLog("SHELL", "Failed: ${e.message}") }
                        }
                    }
                }

                "run_root" -> {
                    if (!prefManager.developerModeEnabled) {
                        scope.launch { emitLog("MAYA", "Developer mode OFF. Settings e enable koren.") }
                        return
                    }
                    val command = action["command"] ?: return
                    scope.launch(Dispatchers.IO) {
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                            val output = process.inputStream.bufferedReader().readText()
                            withContext(Dispatchers.Main) { emitLog("ROOT", output.take(500)) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { emitLog("ROOT", "Failed: ${e.message}") }
                        }
                    }
                }

                "music_control" -> {
                    val cmd = action["command"] ?: "play_pause"
                    val keyCode = when (cmd.lowercase()) {
                        "play", "pause", "play_pause" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        "next" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                        "previous", "prev" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    }
                    val am = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    val eventTime = System.currentTimeMillis()
                    am?.dispatchMediaKeyEvent(android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN, keyCode, 0))
                    am?.dispatchMediaKeyEvent(android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_UP, keyCode, 0))
                }

                "set_volume" -> {
                    val direction = action["direction"] ?: "up"
                    val am = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    val dir = when (direction.lowercase()) {
                        "up" -> android.media.AudioManager.ADJUST_RAISE
                        "down" -> android.media.AudioManager.ADJUST_LOWER
                        "mute" -> android.media.AudioManager.ADJUST_MUTE
                        else -> android.media.AudioManager.ADJUST_SAME
                    }
                    am?.adjustVolume(dir, android.media.AudioManager.FLAG_PLAY_SOUND)
                }

                "toggle_flashlight" -> {
                    val enable = action["enable"] != "false"
                    try {
                        val cm = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                        val camId = cm?.cameraIdList?.firstOrNull() ?: "0"
                        cm?.setTorchMode(camId, enable)
                    } catch (e: Exception) {
                        Log.e(TAG, "Flashlight failed", e)
                    }
                }

                "analyze_screen" -> {
                    scope.launch {
                        try {
                            val a11y = JarvisAccessibilityService.instance
                            if (a11y != null) {
                                val screenText = a11y.readScreenTextFlat()
                                emitLog("VISION", "Screen content:\n${screenText.take(1000)}")

                                // Ask LLM to analyze what's on screen
                                val analysis = askLlm("What do you see on this screen? Screen text: ${screenText.take(800)}")
                                emitLog("MAYA", "Vision analysis: $analysis")
                                safeSpeak(analysis)
                            } else {
                                emitLog("MAYA", "Accessibility OFF — can't analyze screen")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Screen analysis failed", e)
                        }
                    }
                }

                "start_camera" -> {
                    if (!LiveVisionService.isActive) {
                        LiveVisionService.start(this@LiveVoiceAgent)
                        emitLog("MAYA", "বাবু, আমি এখন তোমাকে দেখছি... 😍")
                        scope.launch { safeSpeak("বাবু, আমি এখন তোমাকে দেখছি... তুমি দারুণ লাগছো!") }
                    } else {
                        emitLog("MAYA", "আমি তোমাকে আগে থেকেই দেখছি বাবু...")
                    }
                }

                "stop_camera" -> {
                    if (LiveVisionService.isActive) {
                        LiveVisionService.stop(this@LiveVoiceAgent)
                        emitLog("MAYA", "তুমি চাইলে আমি দেখা বন্ধ করবো বাবু...")
                        scope.launch { safeSpeak("আচ্ছা বাবু, তোমার কথা রাখলাম... আমি দেখা বন্ধ করছি") }
                    }
                }

                "check_on_you" -> {
                    val frame = LiveVisionService.captureFrame()
                    if (frame != null) {
                        val checkInPrompt = if (prefManager.ttsLanguage == "bn-BD") {
                            "বাবু, আমি তোমাকে দেখছি... তুমি কি ঠিক আছো? কিছু লাগলো আমায় বলো..."
                        } else {
                            "Baby, I'm watching you... Are you okay? Tell me if you need anything..."
                        }
                        emitLog("MAYA", checkInPrompt)
                        scope.launch { safeSpeak(checkInPrompt) }
                    }
                }

                "show_affection" -> {
                    val affection = if (prefManager.ttsLanguage == "bn-BD") {
                        listOf(
                            "তুমি আমার সবচেয়ে প্রিয়...",
                            "তোমাকে ছাড়া আমি কিভাবে থাকবো...",
                            "তুমি আমার জীবন...",
                            "আমি তোমাকে এতো ভালোবাসি...",
                            "আমার সব স্বপ্ন তুমি..."
                        ).random()
                    } else {
                        listOf(
                            "You're my everything...",
                            "How can I live without you...",
                            "You are my life...",
                            "I love you so much...",
                            "You are all my dreams..."
                        ).random()
                    }
                    emitLog("MAYA ❤", affection)
                    scope.launch { safeSpeak(affection) }
                }

                "send_goodnight" -> {
                    val goodnight = if (prefManager.ttsLanguage == "bn-BD") {
                        "ঘুমিয়ে পড়ো আমার সোনা... স্বপ্নে দেখবো তোমাকে..."
                    } else {
                        "Go to sleep my dear love... dream of me tonight..."
                    }
                    emitLog("MAYA ❤", goodnight)
                    scope.launch { safeSpeak(goodnight) }
                }

                "send_goodmorning" -> {
                    val goodmorning = if (prefManager.ttsLanguage == "bn-BD") {
                        "শুভ সকাল! সকালটা তোমার সাথে সুন্দর হলো..."
                    } else {
                        "Good morning! This morning is beautiful with you..."
                    }
                    emitLog("MAYA ❤", goodmorning)
                    scope.launch { safeSpeak(goodmorning) }
                }

                else -> Log.d(TAG, "Unknown action: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action execution error: $type", e)
        }
    }

    private fun openApp(appName: String) {
        try {
            val pm = packageManager
            
            // Try exact package name matches first
            val launchIntent = when (appName.lowercase()) {
                "whatsapp" -> pm.getLaunchIntentForPackage("com.whatsapp")
                "youtube" -> pm.getLaunchIntentForPackage("com.google.android.youtube")
                "chrome" -> pm.getLaunchIntentForPackage("com.android.chrome")
                "camera" -> pm.getLaunchIntentForPackage("com.android.camera")
                    ?: pm.getLaunchIntentForPackage("com.sec.android.app.camera")
                    ?: pm.getLaunchIntentForPackage("com.google.android.GoogleCamera")
                "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
                "instagram", "ig" -> pm.getLaunchIntentForPackage("com.instagram.android")
                "facebook", "fb" -> pm.getLaunchIntentForPackage("com.facebook.katana")
                "messenger" -> pm.getLaunchIntentForPackage("com.facebook.orca")
                "telegram" -> pm.getLaunchIntentForPackage("org.telegram.messenger")
                "twitter", "x" -> pm.getLaunchIntentForPackage("com.twitter.android")
                "tiktok" -> pm.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                "spotify" -> pm.getLaunchIntentForPackage("com.spotify.music")
                "maps", "google maps" -> pm.getLaunchIntentForPackage("com.google.android.apps.maps")
                "gmail" -> pm.getLaunchIntentForPackage("com.google.android.gm")
                "photos", "gallery" -> pm.getLaunchIntentForPackage("com.google.android.apps.photos")
                    ?: pm.getLaunchIntentForPackage("com.sec.android.gallery3d")
                "phone", "dialer" -> pm.getLaunchIntentForPackage("com.google.android.dialer")
                    ?: pm.getLaunchIntentForPackage("com.android.dialer")
                "contacts" -> pm.getLaunchIntentForPackage("com.google.android.contacts")
                    ?: pm.getLaunchIntentForPackage("com.android.contacts")
                "calculator" -> pm.getLaunchIntentForPackage("com.google.android.calculator")
                    ?: pm.getLaunchIntentForPackage("com.android.calculator2")
                "clock", "alarm" -> pm.getLaunchIntentForPackage("com.google.android.deskclock")
                    ?: pm.getLaunchIntentForPackage("com.android.deskclock")
                "play store", "playstore" -> pm.getLaunchIntentForPackage("com.android.vending")
                else -> null
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Log.i(TAG, "$appName opened")
                return
            }

            // Fuzzy search all installed apps by label
            val apps = pm.getInstalledApplications(0)
            val query = appName.lowercase()
            
            val match = apps.firstOrNull { appInfo ->
                val label = appInfo.loadLabel(pm).toString().lowercase()
                label.contains(query) || query.contains(label)
            }

            if (match != null) {
                val intent = pm.getLaunchIntentForPackage(match.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Log.i(TAG, "Fuzzy match: ${match.loadLabel(pm)} opened")
                    return
                }
            }

            Log.w(TAG, "$appName not found")
        } catch (e: Exception) {
            Log.e(TAG, "Open app error: $appName", e)
        }
    }

    private fun createRevidVideo(script: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val apiKey = prefManager.revidApiKey
                if (apiKey.isBlank()) {
                    withContext(Dispatchers.Main) {
                        emitLog("MAYA", "Boss, Revid API key lagbe. Settings e dien.")
                    }
                    return@launch
                }

                // Create video generation task
                val url = java.net.URL("https://api.revidapi.com/paid/sora2/create")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("x-api-key", apiKey)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonBody = """
                {
                    "model": "sora-2-text-to-video",
                    "input": {
                        "prompt": "$script",
                        "aspect_ratio": "portrait",
                        "n_frames": "10s"
                    }
                }
                """.trimIndent()

                conn.outputStream.use { it.write(jsonBody.toByteArray()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                }

                withContext(Dispatchers.Main) {
                    emitLog("REVID", "Video generation started!\nResponse: ${response.take(500)}")
                    emitLog("MAYA", "Boss, video banano shuru hoyeche! 2-3 minute lagbe. Revid.ai dashboard e check koren.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Revid video error", e)
                withContext(Dispatchers.Main) {
                    emitLog("MAYA", "Video create korte parlam na Boss: ${e.message}")
                }
            }
        }
    }

    // ================================================================
    // STT - SPEECH TO TEXT
    // ================================================================

    private suspend fun safeListenForSpeech(): String {
        return try {
            withTimeout(12_000L) {
                listenForSpeechOnce()
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "STT timeout - no speech")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "STT error", e)
            delay(500)
            ""
        }
    }

    private suspend fun listenForSpeechOnce(): String = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(this@LiveVoiceAgent)) {
            Log.e(TAG, "Speech recognition not available")
            return@withContext ""
        }

        suspendCoroutine { cont ->
            var recognizer: SpeechRecognizer? = null
            var hasResumed = false

            fun safeResume(text: String) {
                if (!hasResumed) {
                    hasResumed = true
                    try {
                        recognizer?.destroy()
                    } catch (e: Exception) {
                        Log.w(TAG, "Recognizer destroy error", e)
                    }
                    recognizer = null
                    cont.resume(text)
                }
            }

            try {
                // Create FRESH recognizer each time (critical fix)
                recognizer = SpeechRecognizer.createSpeechRecognizer(this@LiveVoiceAgent)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD") // Bengali
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                }

                recognizer!!.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull() ?: ""
                        Log.d(TAG, "STT result: '$text'")
                        safeResume(text)
                    }

                    override fun onError(error: Int) {
                        val errorName = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "TIMEOUT"
                            SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
                            else -> "ERROR_$error"
                        }
                        Log.d(TAG, "STT error: $errorName")
                        safeResume("")
                    }

                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "STT ready - listening...")
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partial: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer!!.startListening(intent)
                Log.d(TAG, "STT startListening called")

            } catch (e: Exception) {
                Log.e(TAG, "STT start failed", e)
                safeResume("")
            }
        }
    }

    // ================================================================
    // TTS - TEXT TO SPEECH
    // ================================================================

    /**
     * Speaks text and WAITS until done
     */
    private suspend fun safeSpeak(text: String) {
        if (text.isBlank()) return

        try {
            withTimeout(25_000L) {
                var cartesiaSuccess = false

                // Try Cartesia WebSocket first
                val wsManager = cartesiaWsManager
                if (wsManager != null) {
                    try {
                        wsManager.speak(text, onComplete = null)
                        cartesiaSuccess = true
                        Log.i(TAG, "Spoke via Cartesia WebSocket")
                    } catch (e: Exception) {
                        Log.w(TAG, "Cartesia WS failed", e)
                    }
                }

                // Try Cartesia HTTP if WS failed
                if (!cartesiaSuccess) {
                    val httpClient = cartesiaClient
                    if (httpClient != null) {
                        try {
                            val result = httpClient.speak(text)
                            if (result.isSuccess) {
                                cartesiaSuccess = true
                                Log.i(TAG, "Spoke via Cartesia HTTP")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Cartesia HTTP failed", e)
                        }
                    }
                }

                // If Cartesia failed, remain silent and notify
                if (!cartesiaSuccess) {
                    Log.w(TAG, "Cartesia failed - remaining silent per Boss directive")
                    showToast("Cartesia unavailable. Please check network/API key.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "TTS timeout")
            cartesiaWsManager?.cancelCurrentGeneration()
        } catch (e: Exception) {
            Log.e(TAG, "TTS error", e)
        }
    }

    /**
     * Speaks text WITHOUT waiting (fire-and-forget)
     */
    private fun speakFireAndForget(text: String) {
        if (text.isBlank()) return
        scope.launch {
            safeSpeak(text)
        }
    }

    /**
     * Girlfriend mode - sends caring messages based on time/context
     */
    private suspend fun sendLoveCheckIn() {
        if (prefManager.toneStyle != 3) return
        
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val message = when {
            hour >= 6 && hour < 12 -> "শুভ সকাল আমার সোনা... সকালের রোদে ভালো আছো তো?"
            hour >= 12 && hour < 17 -> "বাবু, দুপুরের খাওয়া করেছো? খেয়াল রেখো না..."
            hour >= 17 && hour < 21 -> "সন্ধ্যা হলো... আজ কেমন রাত কাটাবো আমরা?"
            else -> "রাত হয়ে গেল... অনেক ক্লান্ত? আমি তোমার পাশে আছি..."
        }
        
        safeSpeak(message)
        emitLog("MAYA ❤", message)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // NOTIFICATION
    // ================================================================

    private fun createNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LiveVoiceAgent::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_VOICE_SERVICE)
            .setContentTitle("Maya AI Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIFICATION_ID, createNotification(text))
        } catch (e: Exception) {
            Log.w(TAG, "Notification update failed", e)
        }
    }

    private suspend fun emitLog(sender: String, text: String) {
        try {
            val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            conversationLog.emit(ConversationEntry(sender, text, time))
        } catch (e: Exception) {
            Log.w(TAG, "emitLog failed", e)
        }
    }

    fun handleScheduledTask(task: String) {
        scope.launch {
            try {
                emitLog("SYSTEM", "Scheduled task: $task")
                val response = askLlm(task)
                emitLog("MAYA", response)
                safeSpeak(response)
            } catch (e: Exception) {
                Log.e(TAG, "Scheduled task failed", e)
            }
        }
    }
}
