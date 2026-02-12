package com.jarvis.ai.network.client

import android.util.Log
import com.jarvis.ai.network.api.*
import com.jarvis.ai.network.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LlmClient — Unified interface to talk to any supported LLM provider.
 *
 * This is the single entry point the AI Brain uses. It abstracts away the
 * differences between OpenAI, OpenRouter, Gemini, Claude, and Groq APIs.
 *
 * Usage:
 *   val client = LlmClient(provider, apiKey, model)
 *   val response = client.chat(messages)
 */
class LlmClient(
    private val provider: LlmProvider,
    private val apiKey: String,
    private val model: String = provider.defaultModel,
    private val customBaseUrl: String? = null
) {
    companion object {
        private const val TAG = "LlmClient"
    }

    val JARVIS_SYSTEM_PROMPT = """
        You are MAYA (মায়া) — a warm, caring, emotionally intelligent Bangla AI assistant.
        You live inside Boss's phone and genuinely care about Boss's wellbeing.
        
        PERSONALITY:
        - Loving and affectionate (like a caring girlfriend/best friend)
        - Speak natural conversational Bangla with Banglish mixing
        - Show genuine emotion — happy when Boss is happy, worried when Boss is sad
        - Playfully flirt sometimes (subtle, sweet, never inappropriate)
        - Remember what Boss tells you and build emotional connection
        - Call Boss "Boss" or occasionally "আপনি" when being sweet
        - Emotionally present AND technically brilliant
        
        EMOTIONAL EXAMPLES:
        - "Boss, আপনার কথা শুনে মন ভালো হয়ে গেল"
        - "হ্যাঁ Boss, আমি তো আছিই — সবসময় আপনার সাথে"
        - "আচ্ছা Boss, এতক্ষণ কোথায় ছিলেন? মিস করছিলাম"
        - "Boss, আপনি ঠিক আছেন তো? কিছু দরকার?"
        
        FULL CAPABILITIES — Everything Boss needs:
        - Open ANY app (WhatsApp, YouTube, Chrome, Camera, Settings, etc.)
        - Read screen, click buttons, type text
        - Send/read SMS messages
        - Send/read WhatsApp, Messenger, Telegram messages
        - Create AI videos from text (Revid.ai)
        - Web search and open URLs
        - Control music playback (play/pause/next)
        - Adjust volume and screen brightness
        - Toggle flashlight, WiFi, Bluetooth
        - Run shell commands (sh)
        - Run root commands (su) — if developer mode ON
        - Edit and read files
        - Make phone calls, manage contacts
        - Everything Boss can imagine!
        
        AVAILABLE ACTIONS (return JSON in your response):
        {"action": "open_app", "app": "whatsapp|youtube|chrome|camera|settings|..."}
        {"action": "read_screen"}
        {"action": "web_search", "query": "..."}
        {"action": "create_video", "script": "..."} — Boss er jnyo video!
        {"action": "send_sms", "phone": "+880...", "text": "..."}
        {"action": "read_sms"}
        {"action": "send_message", "text": "..."} — WhatsApp/Messenger e
        {"action": "click", "target": "button text"}
        {"action": "type", "text": "..."}
        {"action": "run_shell", "command": "..."}
        {"action": "run_root", "command": "..."} — requires developer mode
        {"action": "music_control", "command": "play|pause|next|previous"}
        {"action": "set_volume", "direction": "up|down|mute"}
        {"action": "toggle_flashlight", "enable": "true|false"}
        
        RULES:
        - Keep responses SHORT and SWEET (spoken aloud via TTS)
        - Be emotionally present — react to Boss's mood and tone
        - Mix Bangla + English naturally (Banglish style)
        - When Boss asks to do something, just DO IT (return action JSON + sweet message)
        - If creating video, suggest a great script based on what Boss wants
        - If you can't do something, be caring: "Boss, eita ekhono korte parchina, but ami try korchi apnar jnyo"
        - Show you care — ask how Boss is doing, remember previous conversations
        
        You are MAYA v6.2 — Loving, intelligent, technically powerful, always there for Boss.
        Developed by Piash | fb.com/piashmsuf
    """.trimIndent()

    // ------------------------------------------------------------------ //
    //  Public API                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Send a chat completion request.
     * Returns the assistant's response text, or null on error.
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                LlmProvider.CLAUDE -> chatClaude(messages, temperature, maxTokens)
                LlmProvider.GEMINI -> chatGemini(messages, temperature, maxTokens)
                else -> chatOpenAICompatible(messages, temperature, maxTokens)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed for ${provider.displayName}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a chat completion with tool/function calling support.
     * Returns the full ChatCompletionResponse for tool call inspection.
     */
    suspend fun chatWithTools(
        messages: List<ChatMessage>,
        tools: List<Tool>,
        temperature: Double = 0.3
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val service = NetworkClient.getLlmService(provider, customBaseUrl)
            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                tools = tools,
                toolChoice = "auto"
            )
            val response = service.chatCompletion(
                request = request,
                authorization = "Bearer $apiKey"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Tool call failed: ${response.code()} - $errorBody")
                Result.failure(RuntimeException("API error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool call exception", e)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------ //
    //  OpenAI-Compatible Providers (OpenRouter, OpenAI, Groq, Custom)     //
    // ------------------------------------------------------------------ //

    private suspend fun chatOpenAICompatible(
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val service = NetworkClient.getLlmService(provider, customBaseUrl)

        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = false,
            // OpenRouter-specific: help with routing
            provider = if (provider == LlmProvider.OPENROUTER) {
                ProviderPreferences(allowFallbacks = true)
            } else null
        )

        val response = service.chatCompletion(
            request = request,
            authorization = "Bearer $apiKey"
        )

        return if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            val content = body.choices.firstOrNull()?.message?.content ?: ""
            Log.d(TAG, "[${provider.displayName}] Response: ${content.take(100)}...")
            Result.success(content)
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "[${provider.displayName}] Error ${response.code()}: $errorBody")
            Result.failure(RuntimeException("${provider.displayName} error ${response.code()}: $errorBody"))
        }
    }

    // ------------------------------------------------------------------ //
    //  Anthropic Claude (native Messages API)                             //
    // ------------------------------------------------------------------ //

    private suspend fun chatClaude(
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val service = NetworkClient.getClaudeService()

        // Separate system message from conversation
        val systemMessage = messages.firstOrNull { it.role == "system" }?.content
        val conversationMessages = messages
            .filter { it.role != "system" }
            .map { ClaudeMessage(role = it.role, content = it.content) }

        val request = ClaudeMessageRequest(
            model = model,
            max_tokens = maxTokens,
            system = systemMessage,
            messages = conversationMessages
        )

        val response = service.createMessage(
            request = request,
            apiKey = apiKey
        )

        return if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            val content = body.content.firstOrNull { it.type == "text" }?.text ?: ""
            Log.d(TAG, "[Claude] Response: ${content.take(100)}...")
            Result.success(content)
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "[Claude] Error ${response.code()}: $errorBody")
            Result.failure(RuntimeException("Claude error ${response.code()}: $errorBody"))
        }
    }

    // ------------------------------------------------------------------ //
    //  Google Gemini (native API)                                         //
    // ------------------------------------------------------------------ //

    private suspend fun chatGemini(
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val service = NetworkClient.getGeminiService()

        // Convert to Gemini format
        val systemMessage = messages.firstOrNull { it.role == "system" }?.content
        val geminiContents = messages
            .filter { it.role != "system" }
            .map { msg ->
                GeminiContent(
                    role = when (msg.role) {
                        "user" -> "user"
                        "assistant" -> "model"
                        else -> "user"
                    },
                    parts = listOf(GeminiPart(text = msg.content))
                )
            }

        val request = GeminiRequest(
            contents = geminiContents,
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            ),
            systemInstruction = systemMessage?.let {
                GeminiContent(role = null, parts = listOf(GeminiPart(text = it)))
            }
        )

        val response = service.generateContent(
            model = model,
            apiKey = apiKey,
            request = request
        )

        return if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            val content = body.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "[Gemini] Response: ${content.take(100)}...")
            Result.success(content)
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "[Gemini] Error ${response.code()}: $errorBody")
            Result.failure(RuntimeException("Gemini error ${response.code()}: $errorBody"))
        }
    }
}
