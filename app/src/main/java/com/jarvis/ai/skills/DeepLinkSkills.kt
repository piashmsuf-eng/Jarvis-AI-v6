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
        // AI Image Generation
        Skill(
            keywords = listOf("image banao", "picture create", "draw koro", "pik banao", "photo create"),
            description = "Create AI images",
            deepLink = "xiaomiai://ai/image",  // Xiaomi AI
            fallbackPackage = null
        ),
        
        // Navigation
        Skill(
            keywords = listOf("navigate", "rasta dekha", "direction", "map", "location jao"),
            description = "Navigate to location",
            deepLink = "google.navigation:q=",  // Google Maps
            fallbackPackage = "com.google.android.apps.maps"
        ),
        
        // Music
        Skill(
            keywords = listOf("play music", "gan bajao", "song play", "music chalao"),
            description = "Play music",
            deepLink = "spotify:search:",  // Spotify
            fallbackPackage = "com.spotify.music"
        ),
        
        // Food delivery (delegation to Meituan AI if available)
        Skill(
            keywords = listOf("khawabo", "food order", "khabar", "delivery"),
            description = "Order food",
            deepLink = "imeituan://www.meituan.com/home",
            fallbackPackage = null
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
