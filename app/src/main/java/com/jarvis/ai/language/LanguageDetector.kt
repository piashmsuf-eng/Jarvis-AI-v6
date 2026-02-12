package com.jarvis.ai.language

/**
 * LanguageDetector - Detects if input is Bengali or English
 * 
 * Uses Unicode ranges to identify Bengali characters.
 * Bengali Unicode range: \u0980-\u09FF
 */
object LanguageDetector {
    
    enum class Language {
        BENGALI,
        ENGLISH,
        MIXED
    }
    
    /**
     * Detect language of input text
     */
    fun detect(text: String): Language {
        if (text.isBlank()) return Language.ENGLISH
        
        val bengaliChars = text.count { isBengaliChar(it) }
        val englishChars = text.count { it.isLetter() && !isBengaliChar(it) }
        val totalLetters = bengaliChars + englishChars
        
        if (totalLetters == 0) return Language.ENGLISH
        
        val bengaliPercent = (bengaliChars.toFloat() / totalLetters) * 100
        
        return when {
            bengaliPercent >= 70 -> Language.BENGALI
            bengaliPercent >= 20 -> Language.MIXED
            else -> Language.ENGLISH
        }
    }
    
    /**
     * Check if character is Bengali (Unicode range: \u0980-\u09FF)
     */
    fun isBengaliChar(char: Char): Boolean {
        return char.code in 0x0980..0x09FF
    }
    
    /**
     * Check if text contains any Bengali characters
     */
    fun containsBengali(text: String): Boolean {
        return text.any { isBengaliChar(it) }
    }
    
    /**
     * Extract Bengali text from mixed input
     */
    fun extractBengali(text: String): String {
        return text.filter { isBengaliChar(it) || it.isWhitespace() }.trim()
    }
    
    /**
     * Extract English text from mixed input
     */
    fun extractEnglish(text: String): String {
        return text.filter { !isBengaliChar(it) }.trim()
    }
}
