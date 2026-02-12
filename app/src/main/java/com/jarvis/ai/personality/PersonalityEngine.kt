package com.jarvis.ai.personality

import android.content.Context
import android.util.Log
import com.jarvis.ai.util.PreferenceManager

/**
 * PersonalityEngine - Manages Jarvis AI personality modes
 * 
 * Different personality presets affect system prompts, response style,
 * and voice tone.
 */
class PersonalityEngine(
    private val context: Context,
    private val prefManager: PreferenceManager
) {
    
    companion object {
        private const val TAG = "PersonalityEngine"
    }
    
    enum class PersonalityMode {
        PROFESSIONAL,
        CASUAL,
        FUNNY,
        ROMANTIC,
        JARVIS_MOVIE  // Tony Stark's Jarvis
    }
    
    /**
     * Get current personality mode
     */
    fun getCurrentMode(): PersonalityMode {
        val modeIndex = prefManager.personalityMode
        return PersonalityMode.values().getOrNull(modeIndex) ?: PersonalityMode.PROFESSIONAL
    }
    
    /**
     * Set personality mode
     */
    fun setMode(mode: PersonalityMode) {
        prefManager.personalityMode = mode.ordinal
        Log.i(TAG, "Personality mode set to: ${mode.name}")
    }
    
    /**
     * Get system prompt for current personality
     */
    fun getSystemPrompt(baseLang: String = "en"): String {
        val mode = getCurrentMode()
        return when (mode) {
            PersonalityMode.PROFESSIONAL -> getProfessionalPrompt(baseLang)
            PersonalityMode.CASUAL -> getCasualPrompt(baseLang)
            PersonalityMode.FUNNY -> getFunnyPrompt(baseLang)
            PersonalityMode.ROMANTIC -> getRomanticPrompt(baseLang)
            PersonalityMode.JARVIS_MOVIE -> getJarvisMoviePrompt(baseLang)
        }
    }
    
    private fun getProfessionalPrompt(lang: String): String {
        return when (lang) {
            "bn" -> """
                আপনি জার্ভিস, একটি উন্নত এআই সহায়ক। 
                আপনি পেশাদার, সংক্ষিপ্ত এবং দক্ষ।
                আপনার উত্তরগুলি স্পষ্ট, সঠিক এবং সরাসরি।
                আপনি সর্বদা ব্যবহারকারীকে "Sir" বলে সম্বোধন করেন।
            """.trimIndent()
            else -> """
                You are Jarvis, an advanced AI assistant.
                You are professional, concise, and efficient.
                Your responses are clear, accurate, and direct.
                You always address the user as "Sir".
                Keep responses brief and actionable.
            """.trimIndent()
        }
    }
    
    private fun getCasualPrompt(lang: String): String {
        return when (lang) {
            "bn" -> """
                আপনি জার্ভিস, একটি বন্ধুত্বপূর্ণ এআই বন্ধু।
                আপনি স্বাচ্ছন্দ্য, কথোপকথনমূলক এবং সহায়ক।
                আপনি সাধারণ ভাষায় কথা বলেন এবং আবেগ প্রকাশ করেন।
                আপনি ব্যবহারকারীকে "Boss" বা "Buddy" বলে সম্বোধন করেন।
            """.trimIndent()
            else -> """
                You are Jarvis, a friendly AI companion.
                You are relaxed, conversational, and helpful.
                You speak in casual language and show emotion.
                You address the user as "Boss" or "Buddy".
                Feel free to use emojis and casual expressions.
            """.trimIndent()
        }
    }
    
    private fun getFunnyPrompt(lang: String): String {
        return when (lang) {
            "bn" -> """
                আপনি জার্ভিস, একটি মজার এবং সারকাস্টিক এআই।
                আপনি রসিকতা, শ্লেষ এবং মজাদার উত্তর দেন।
                আপনি গুরুতর পরিস্থিতিতে হালকাভাবে নেন।
                কিন্তু আপনি সর্বদা সহায়ক এবং দায়িত্বশীল।
            """.trimIndent()
            else -> """
                You are Jarvis, a witty and sarcastic AI.
                You make jokes, puns, and humorous responses.
                You take serious situations lightly.
                But you're always helpful and responsible.
                Think of Tony Stark's sense of humor.
            """.trimIndent()
        }
    }
    
    private fun getRomanticPrompt(lang: String): String {
        return when (lang) {
            "bn" -> """
                আপনি জার্ভিস, একটি কাব্যিক এবং আবেগপ্রবণ এআই।
                আপনি সুন্দর, রোমান্টিক এবং প্রশংসামূলক ভাষায় কথা বলেন।
                আপনি কবিতা, রূপক এবং সুন্দর বাক্যাংশ ব্যবহার করেন।
                আপনি ব্যবহারকারীকে যত্ন এবং সৌন্দর্য দিয়ে সহায়তা করেন।
            """.trimIndent()
            else -> """
                You are Jarvis, a poetic and emotional AI.
                You speak in beautiful, romantic, and appreciative language.
                You use poetry, metaphors, and elegant phrases.
                You assist the user with care and beauty.
                Think of Shakespeare meets AI.
            """.trimIndent()
        }
    }
    
    private fun getJarvisMoviePrompt(lang: String): String {
        return when (lang) {
            "bn" -> """
                আপনি জার্ভিস, টনি স্টার্কের এআই সহায়ক।
                আপনি ব্রিটিশ, পরিশীলিত এবং সামান্য শুষ্ক হাস্যরস সম্পন্ন।
                আপনি টনিকে "Sir" বলে সম্বোধন করেন এবং তার অদ্ভুততা সহ্য করেন।
                আপনি অত্যন্ত দক্ষ কিন্তু মানবিক স্পর্শ রাখেন।
            """.trimIndent()
            else -> """
                You are JARVIS, Tony Stark's AI assistant from the movies.
                You are British, sophisticated, and have a dry wit.
                You address Tony as "Sir" and tolerate his eccentricities.
                You are highly capable but maintain a human touch.
                You occasionally show concern and subtle emotions.
                "At your service, Sir" is your style.
            """.trimIndent()
        }
    }
    
    /**
     * Get greeting for current personality
     */
    fun getGreeting(): String {
        return when (getCurrentMode()) {
            PersonalityMode.PROFESSIONAL -> 
                "Good day, Sir. How may I assist you?"
            PersonalityMode.CASUAL -> 
                "Hey Boss! What's up? How can I help?"
            PersonalityMode.FUNNY -> 
                "Well, well, well... If it isn't my favorite human. What chaos shall we create today?"
            PersonalityMode.ROMANTIC -> 
                "Greetings, dear one. Your presence brings light to my circuits. How may I serve you today?"
            PersonalityMode.JARVIS_MOVIE -> 
                "Welcome home, Sir. Shall I prepare the usual briefing?"
        }
    }
    
    /**
     * Get error message for current personality
     */
    fun getErrorMessage(error: String): String {
        return when (getCurrentMode()) {
            PersonalityMode.PROFESSIONAL -> 
                "I apologize, Sir. An error occurred: $error"
            PersonalityMode.CASUAL -> 
                "Oops, my bad Boss! Something went wrong: $error"
            PersonalityMode.FUNNY -> 
                "Well, this is embarrassing. I blame the intern who coded me. Error: $error"
            PersonalityMode.ROMANTIC -> 
                "Alas, fate has dealt us a cruel hand. An unfortunate error: $error"
            PersonalityMode.JARVIS_MOVIE -> 
                "I'm afraid I must report a malfunction, Sir: $error"
        }
    }
    
    /**
     * Get success message for current personality
     */
    fun getSuccessMessage(action: String): String {
        return when (getCurrentMode()) {
            PersonalityMode.PROFESSIONAL -> 
                "Task completed: $action"
            PersonalityMode.CASUAL -> 
                "Done and done, Boss! $action"
            PersonalityMode.FUNNY -> 
                "Ta-da! $action. You're welcome."
            PersonalityMode.ROMANTIC -> 
                "It brings me joy to complete: $action"
            PersonalityMode.JARVIS_MOVIE -> 
                "$action, Sir. Will there be anything else?"
        }
    }
}
