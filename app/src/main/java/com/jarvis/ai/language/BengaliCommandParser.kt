package com.jarvis.ai.language

/**
 * BengaliCommandParser - Translates Bengali voice commands to English equivalents
 * 
 * Handles mixed Bengali-English input common in Bangladesh/West Bengal.
 * Example: "জার্ভিস, মা কে call করো" → "Jarvis, call mom"
 */
object BengaliCommandParser {
    
    /**
     * Parse Bengali command to English
     */
    fun parse(input: String): String {
        var parsed = input.lowercase()
        
        // Apply all translation mappings
        parsed = translateVerbs(parsed)
        parsed = translateNouns(parsed)
        parsed = translateActions(parsed)
        parsed = translateQuestions(parsed)
        parsed = translateNumbers(parsed)
        parsed = translateCommon(parsed)
        
        return parsed.trim()
    }
    
    /**
     * Translate Bengali verbs to English
     */
    private fun translateVerbs(text: String): String {
        var result = text
        
        val verbMap = mapOf(
            // Basic actions
            "করো" to "do",
            "কর" to "do", 
            "করে দাও" to "do",
            "কোরো" to "do",
            "দাও" to "give",
            "দে" to "give",
            "দেও" to "give",
            "বল" to "tell",
            "বলো" to "tell",
            "বলে দাও" to "tell",
            "দেখা" to "show",
            "দেখাও" to "show",
            "দেখাo" to "show",
            "নাও" to "take",
            "নে" to "take",
            "নেও" to "take",
            "খোল" to "open",
            "খোলো" to "open",
            "খুলে দাও" to "open",
            "বন্ধ কর" to "close",
            "বন্ধ করো" to "close",
            "চালাও" to "play",
            "চালু করো" to "start",
            "চালা" to "play",
            "বাজাও" to "play",
            "বাজা" to "play",
            "বন্ধ করে দাও" to "close",
            "বন্ধ কোরো" to "close",
            "শুনাও" to "play",
            "পাঠাও" to "send",
            "পাঠা" to "send",
            "লিখ" to "write",
            "লিখো" to "write",
            "পড়" to "read",
            "পড়ো" to "read",
            "পড়ে দাও" to "read",
            "পড়ে শোনাও" to "read",
            "যাও" to "go",
            "চল" to "go",
            "চলো" to "go",
            "আসো" to "come",
            "আস" to "come",
            "খুঁজ" to "search",
            "খুঁজো" to "search",
            "খোঁজ" to "search",
            "খোঁজো" to "search",
            "ডাক" to "call",
            "ডাকো" to "call",
            "কল করো" to "call",
            "কল কর" to "call",
            "ফোন করো" to "call",
            "ফোন কর" to "call",
            "শেয়ার করো" to "share",
            "শেয়ার কর" to "share",
            "মুছ" to "delete",
            "মুছো" to "delete",
            "ডিলিট করো" to "delete",
            "ডিলিট কর" to "delete",
            "ক্লিন করো" to "clean",
            "ক্লিন কর" to "clean",
            "সাফ করো" to "clean",
            "সাফ কর" to "clean",
            "অর্গানাইজ করো" to "organize",
            "গুছাও" to "organize",
            "সাজাও" to "organize",
            "বাড়াও" to "increase",
            "বাড়া" to "increase",
            "কমাও" to "decrease",
            "কমা" to "decrease",
            "জালাও" to "turn on",
            "জালা" to "turn on",
            "জ্বালাও" to "turn on",
            "জ্বালা" to "turn on",
            "নিভাও" to "turn off",
            "নিভা" to "turn off",
            "অন করো" to "turn on",
            "অফ করো" to "turn off",
            "সাইলেন্ট করো" to "silence",
            "সাইলেন্ট কর" to "silence",
            "মিউট করো" to "mute"
        )
        
        verbMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Translate Bengali nouns to English
     */
    private fun translateNouns(text: String): String {
        var result = text
        
        val nounMap = mapOf(
            // Family & relationships
            "মা" to "mom",
            "বাবা" to "dad",
            "ভাই" to "brother",
            "বোন" to "sister",
            "দাদা" to "elder brother",
            "দিদি" to "elder sister",
            
            // Communication
            "এসএমএস" to "sms",
            "মেসেজ" to "message",
            "ম্যাসেজ" to "message",
            "কল" to "call",
            "ফোন" to "phone",
            "হোয়াটসঅ্যাপ" to "whatsapp",
            "ইমেইল" to "email",
            
            // Media
            "গান" to "song",
            "মিউজিক" to "music",
            "ভিডিও" to "video",
            "ছবি" to "photo",
            "পিকচার" to "picture",
            "ফটো" to "photo",
            "মুভি" to "movie",
            "সিনেমা" to "movie",
            
            // Apps & Tech
            "স্পটিফাই" to "spotify",
            "ইউটিউব" to "youtube",
            "ফেসবুক" to "facebook",
            "ইনস্টাগ্রাম" to "instagram",
            "টেলিগ্রাম" to "telegram",
            "ক্রোম" to "chrome",
            "ব্রাউজার" to "browser",
            "ক্যামেরা" to "camera",
            "গ্যালারি" to "gallery",
            
            // Device & Settings
            "ফোন" to "phone",
            "স্ক্রিন" to "screen",
            "ভলিউম" to "volume",
            "ব্রাইটনেস" to "brightness",
            "ব্যাটারি" to "battery",
            "ওয়াইফাই" to "wifi",
            "ব্লুটুথ" to "bluetooth",
            "নেটওয়ার্ক" to "network",
            "লাইট" to "light",
            "এসি" to "ac",
            "টিভি" to "tv",
            
            // Time & Date
            "আজ" to "today",
            "কাল" to "tomorrow",
            "গতকাল" to "yesterday",
            "এখন" to "now",
            "সময়" to "time",
            "তারিখ" to "date",
            "সপ্তাহ" to "week",
            "মাস" to "month",
            
            // Files & Data
            "ফাইল" to "file",
            "ফোল্ডার" to "folder",
            "ডাউনলোড" to "download",
            "ডকুমেন্ট" to "document",
            "টেম্প" to "temp",
            "ক্যাশ" to "cache",
            
            // Locations
            "বাড়ি" to "home",
            "অফিস" to "office",
            "স্কুল" to "school"
        )
        
        nounMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Translate action phrases
     */
    private fun translateActions(text: String): String {
        var result = text
        
        val actionMap = mapOf(
            // SMS actions
            "শেষ ৫টি এসএমএস" to "last 5 sms",
            "লাস্ট ৫টি মেসেজ" to "last 5 messages",
            "এসএমএস পড়" to "read sms",
            "মেসেজ পড়" to "read messages",
            "কে এসএমএস পাঠা" to "send sms to",
            
            // Call actions
            "কে কল করো" to "call",
            "কে ফোন করো" to "call",
            "কল কাট" to "end call",
            "কল রিসিভ করো" to "answer call",
            
            // WhatsApp
            "কে হোয়াটসঅ্যাপ করো" to "whatsapp",
            "হোয়াটসঅ্যাপ মেসেজ" to "whatsapp messages",
            
            // Phone control
            "ফোন সাইলেন্ট" to "silence phone",
            "সাইলেন্ট মোড" to "silent mode",
            
            // Media
            "স্পটিফাইতে বাজা" to "play on spotify",
            "স্পটিফাইতে চালা" to "play on spotify",
            
            // Device info
            "ব্যাটারি কেমন" to "battery status",
            "ব্যাটারি কত" to "battery level",
            
            // File operations
            "সাম্প্রতিক ডাউনলোড" to "recent downloads",
            "ফটো গুছা" to "organize photos",
            "টেম্প ফাইল ক্লিন" to "clean temp files",
            "ক্যাশ ক্লিয়ার" to "clear cache",
            
            // Screenshot
            "স্ক্রিনশট নাও" to "take screenshot",
            "স্ক্রিনে কি আছে" to "what's on screen",
            
            // Smart home
            "লাইট জালা" to "turn on light",
            "লাইট নিভা" to "turn off light",
            "এসি চালা" to "turn on ac",
            "টিভি বন্ধ" to "turn off tv",
            
            // Routines
            "মর্নিং রুটিন" to "morning routine",
            "নাইট মোড" to "night mode",
            
            // Questions
            "কী আছে" to "what is",
            "কেমন আছে" to "how is",
            "কোথায়" to "where is",
            "কখন" to "when is",
            "কেন" to "why is"
        )
        
        actionMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Translate question words
     */
    private fun translateQuestions(text: String): String {
        var result = text
        
        val questionMap = mapOf(
            "কি" to "what",
            "কী" to "what",
            "কেমন" to "how",
            "কোথায়" to "where",
            "কখন" to "when",
            "কেন" to "why",
            "কার" to "whose",
            "কত" to "how much",
            "কতটা" to "how much",
            "কতক্ষণ" to "how long",
            "কিভাবে" to "how"
        )
        
        questionMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Translate Bengali numbers to English
     */
    private fun translateNumbers(text: String): String {
        var result = text
        
        val numberMap = mapOf(
            "০" to "0", "১" to "1", "২" to "2", "৩" to "3", "৪" to "4",
            "৫" to "5", "৬" to "6", "৭" to "7", "৮" to "8", "৯" to "9",
            "শূন্য" to "0", "এক" to "1", "দুই" to "2", "তিন" to "3",
            "চার" to "4", "পাঁচ" to "5", "ছয়" to "6", "সাত" to "7",
            "আট" to "8", "নয়" to "9", "দশ" to "10",
            "বিশ" to "20", "ত্রিশ" to "30", "চল্লিশ" to "40", "পঞ্চাশ" to "50"
        )
        
        numberMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Translate common phrases
     */
    private fun translateCommon(text: String): String {
        var result = text
        
        val commonMap = mapOf(
            "জার্ভিস" to "jarvis",
            "হ্যালো" to "hello",
            "থ্যাংক ইউ" to "thank you",
            "ধন্যবাদ" to "thank you",
            "দয়া করে" to "please",
            "প্লিজ" to "please",
            "হ্যাঁ" to "yes",
            "না" to "no",
            "ঠিক আছে" to "ok",
            "ওকে" to "ok",
            "স্যার" to "sir",
            "বস" to "boss",
            "কে" to "to",  // Bengali "ke" (to someone)
            "থেকে" to "from",
            "এ" to "at",
            "তে" to "at",
            "র" to "'s",  // Possessive
            "এর" to "of"
        )
        
        commonMap.forEach { (bengali, english) ->
            result = result.replace(bengali, english)
        }
        
        return result
    }
    
    /**
     * Get all Bengali keywords (for command recognition)
     */
    fun getAllBengaliKeywords(): List<String> {
        return listOf(
            // Actions
            "করো", "কর", "দাও", "বল", "বলো", "দেখাও", "নাও", "খোল",
            "বন্ধ", "চালাও", "বাজাও", "পাঠাও", "লিখ", "পড়", "যাও",
            "খুঁজ", "ডাক", "শেয়ার", "মুছ", "ক্লিন", "গুছাও",
            
            // Nouns
            "মা", "বাবা", "ভাই", "বোন", "এসএমএস", "মেসেজ", "কল",
            "গান", "ছবি", "ফোন", "লাইট", "ব্যাটারি",
            
            // Questions
            "কি", "কেমন", "কোথায়", "কত"
        )
    }
}
