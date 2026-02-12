package com.jarvis.ai.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * DeepLinkSkills — Smart app delegation system.
 * 
 * For certain user intents, instead of manually automating,
 * we directly jump to AI-capable apps via DeepLink.
 * 
 * Inspired by Roubao's Delegation Skills.
 * Modded by Piash
 */
object DeepLinkSkills {
    private const val TAG = "DeepLinkSkills"

    data class Skill(
        val keywords: List<String>,
        val description: String,
        val deepLink: String,
        val fallbackPackage: String? = null
    )

    private val skills = listOf(
        // AI Image Generation (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "image banao", "picture create", "draw koro", "pik banao", "photo create",
                // Bengali
                "ছবি বানাও", "ছবি তৈরি করো", "ছবি আঁকো", "ফটো বানাও", "পিকচার বানাও"
            ),
            description = "Create AI images",
            deepLink = "xiaomiai://ai/image",
            fallbackPackage = null
        ),
        
        // Navigation (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "navigate", "rasta dekha", "direction", "map", "location jao",
                // Bengali
                "রাস্তা দেখাও", "দিক নির্দেশনা", "ম্যাপ", "লোকেশন যাও", "নেভিগেট করো"
            ),
            description = "Navigate to location",
            deepLink = "google.navigation:q=",
            fallbackPackage = "com.google.android.apps.maps"
        ),
        
        // Music - Spotify (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "play music", "gan bajao", "song play", "music chalao",
                // Bengali
                "গান বাজাও", "গান চালাও", "মিউজিক বাজাও", "স্পটিফাই", "গান শুনাও"
            ),
            description = "Play music on Spotify",
            deepLink = "spotify:search:",
            fallbackPackage = "com.spotify.music"
        ),
        
        // Food delivery (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "khawabo", "food order", "khabar", "delivery",
                // Bengali
                "খাবার অর্ডার", "খাবার আনাও", "ডেলিভারি", "খাবো"
            ),
            description = "Order food",
            deepLink = "imeituan://www.meituan.com/home",
            fallbackPackage = null
        ),
        
        // YouTube (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "youtube", "youtube kholo", "video dekh", "youtube open",
                // Bengali
                "ইউটিউব", "ইউটিউব খোলো", "ভিডিও দেখ", "ইউটিউব চালাও"
            ),
            description = "Open YouTube",
            deepLink = "vnd.youtube://",
            fallbackPackage = "com.google.android.youtube"
        ),
        
        // WhatsApp (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "whatsapp", "whatsapp kholo", "whatsapp open",
                // Bengali
                "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ খোলো", "হোয়াটসঅ্যাপ চালু করো"
            ),
            description = "Open WhatsApp",
            deepLink = "whatsapp://",
            fallbackPackage = "com.whatsapp"
        ),
        
        // Camera (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "camera", "camera kholo", "photo le", "picture le",
                // Bengali
                "ক্যামেরা", "ক্যামেরা খোলো", "ফটো তোলো", "ছবি তোলো"
            ),
            description = "Open camera",
            deepLink = "android.media.action.IMAGE_CAPTURE",
            fallbackPackage = null
        ),
        
        // Settings (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "settings", "settings kholo", "setting open",
                // Bengali
                "সেটিংস", "সেটিংস খোলো", "সেটিং খোলো"
            ),
            description = "Open settings",
            deepLink = "android.settings.SETTINGS",
            fallbackPackage = null
        ),
        
        // Chrome Browser (Bengali + English)
        Skill(
            keywords = listOf(
                // English
                "chrome", "browser", "chrome kholo", "web browser",
                // Bengali
                "ক্রোম", "ব্রাউজার", "ক্রোম খোলো", "ওয়েব ব্রাউজার"
            ),
            description = "Open Chrome",
            deepLink = "googlechrome://",
            fallbackPackage = "com.android.chrome"
        )
    )

    /**
     * Try to match user query to a DeepLink skill.
     * Returns the deepLink URI if matched, null otherwise.
     */
    fun matchSkill(query: String): Pair<String, String>? {
        val queryLower = query.lowercase()
        
        for (skill in skills) {
            if (skill.keywords.any { queryLower.contains(it) }) {
                Log.i(TAG, "Matched skill: ${skill.description}")
                return skill.deepLink to (skill.fallbackPackage ?: "")
            }
        }
        return null
    }

    /**
     * Execute a DeepLink skill.
     */
    fun execute(context: Context, deepLink: String, fallbackPackage: String, query: String): Boolean {
        return try {
            // Try DeepLink first
            var finalLink = deepLink
            
            // Append query if the deepLink is parameterized
            if (deepLink.endsWith("=") || deepLink.endsWith(":")) {
                finalLink = deepLink + Uri.encode(query)
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "DeepLink executed: $finalLink")
            true
        } catch (e: Exception) {
            Log.w(TAG, "DeepLink failed, trying fallback package", e)
            
            // Fallback to opening the app
            if (fallbackPackage.isNotBlank()) {
                try {
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(fallbackPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        Log.i(TAG, "Fallback package opened: $fallbackPackage")
                        return true
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback also failed", e2)
                }
            }
            false
        }
    }
}
