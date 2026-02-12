package com.jarvis.ai.language

/**
 * BengaliVoiceConfig - Configuration for Bengali TTS voices
 * 
 * Cartesia supports multiple Bengali voices.
 * This provides voice IDs and configurations.
 */
object BengaliVoiceConfig {
    
    /**
     * Bengali voice presets for Cartesia TTS
     */
    enum class BengaliVoice(
        val voiceId: String,
        val displayName: String,
        val description: String,
        val gender: Gender,
        val style: Style
    ) {
        // Professional Bengali voices
        BENGALI_MALE_PROFESSIONAL(
            voiceId = "bengali-male-professional",
            displayName = "পুরুষ (পেশাদার)",
            description = "Professional Bengali male voice",
            gender = Gender.MALE,
            style = Style.PROFESSIONAL
        ),
        
        BENGALI_FEMALE_PROFESSIONAL(
            voiceId = "bengali-female-professional",
            displayName = "মহিলা (পেশাদার)",
            description = "Professional Bengali female voice",
            gender = Gender.FEMALE,
            style = Style.PROFESSIONAL
        ),
        
        // Friendly Bengali voices
        BENGALI_MALE_FRIENDLY(
            voiceId = "bengali-male-friendly",
            displayName = "পুরুষ (বন্ধুত্বপূর্ণ)",
            description = "Friendly Bengali male voice",
            gender = Gender.MALE,
            style = Style.FRIENDLY
        ),
        
        BENGALI_FEMALE_FRIENDLY(
            voiceId = "bengali-female-friendly",
            displayName = "মহিলা (বন্ধুত্বপূর্ণ)",
            description = "Friendly Bengali female voice",
            gender = Gender.FEMALE,
            style = Style.FRIENDLY
        ),
        
        // Formal Bengali voices
        BENGALI_MALE_FORMAL(
            voiceId = "bengali-male-formal",
            displayName = "পুরুষ (আনুষ্ঠানিক)",
            description = "Formal Bengali male voice",
            gender = Gender.MALE,
            style = Style.FORMAL
        ),
        
        BENGALI_FEMALE_FORMAL(
            voiceId = "bengali-female-formal",
            displayName = "মহিলা (আনুষ্ঠানিক)",
            description = "Formal Bengali female voice",
            gender = Gender.FEMALE,
            style = Style.FORMAL
        ),
        
        // Casual Bengali voices
        BENGALI_MALE_CASUAL(
            voiceId = "bengali-male-casual",
            displayName = "পুরুষ (নৈমিত্তিক)",
            description = "Casual Bengali male voice",
            gender = Gender.MALE,
            style = Style.CASUAL
        ),
        
        BENGALI_FEMALE_CASUAL(
            voiceId = "bengali-female-casual",
            displayName = "মহিলা (নৈমিত্তিক)",
            description = "Casual Bengali female voice",
            gender = Gender.FEMALE,
            style = Style.CASUAL
        );
        
        companion object {
            /**
             * Get voice by ID
             */
            fun fromId(id: String): BengaliVoice? {
                return values().firstOrNull { it.voiceId == id }
            }
            
            /**
             * Get default Bengali voice (male professional)
             */
            fun default(): BengaliVoice = BENGALI_MALE_PROFESSIONAL
            
            /**
             * Get all voice IDs
             */
            fun allVoiceIds(): List<String> {
                return values().map { it.voiceId }
            }
            
            /**
             * Get voices by gender
             */
            fun byGender(gender: Gender): List<BengaliVoice> {
                return values().filter { it.gender == gender }
            }
            
            /**
             * Get voices by style
             */
            fun byStyle(style: Style): List<BengaliVoice> {
                return values().filter { it.style == style }
            }
        }
    }
    
    enum class Gender {
        MALE,
        FEMALE
    }
    
    enum class Style {
        PROFESSIONAL,
        FRIENDLY,
        FORMAL,
        CASUAL
    }
    
    /**
     * Bengali-specific TTS settings
     */
    data class BengaliTtsSettings(
        val voice: BengaliVoice = BengaliVoice.default(),
        val speed: Float = 1.0f,              // 0.5 - 2.0
        val pitch: Float = 1.0f,              // 0.5 - 2.0
        val volume: Float = 1.0f,             // 0.0 - 1.0
        val emotion: Emotion = Emotion.NEUTRAL
    )
    
    enum class Emotion {
        NEUTRAL,
        HAPPY,
        SAD,
        ANGRY,
        EXCITED,
        CALM
    }
    
    /**
     * Get Cartesia voice parameters for Bengali
     */
    fun getCartesiaParams(
        voice: BengaliVoice,
        emotion: Emotion = Emotion.NEUTRAL
    ): Map<String, Any> {
        return mapOf(
            "voice_id" to voice.voiceId,
            "language" to "bn-BD",
            "model" to "sonic-multilingual",
            "emotion" to emotion.name.lowercase(),
            "output_format" to mapOf(
                "container" to "raw",
                "encoding" to "pcm_f32le",
                "sample_rate" to 24000
            )
        )
    }
    
    /**
     * Bengali language code for STT/TTS
     */
    const val BENGALI_LANGUAGE_CODE = "bn-BD"
    const val BENGALI_LANGUAGE_CODE_ALT = "bn-IN"  // For Indian Bengali
    
    /**
     * Common Bengali phrases for Jarvis
     */
    object CommonPhrases {
        const val GREETING = "হ্যালো স্যার, আমি জার্ভিস। আমি কিভাবে সাহায্য করতে পারি?"
        const val READY = "আমি প্রস্তুত, স্যার।"
        const val LISTENING = "আমি শুনছি..."
        const val PROCESSING = "প্রসেসিং করছি..."
        const val DONE = "কাজ সম্পন্ন, স্যার।"
        const val ERROR = "দুঃখিত স্যার, একটি সমস্যা হয়েছে।"
        const val SMS_NOTIFICATION = "স্যার, একটি এসএমএস এসেছে %s থেকে।"
        const val CALL_NOTIFICATION = "স্যার, একটি কল আসছে %s থেকে।"
        const val NO_MATCH = "দুঃখিত, আমি বুঝতে পারলাম না।"
        const val PERMISSION_NEEDED = "স্যার, এই কাজের জন্য অনুমতি প্রয়োজন।"
    }
}
