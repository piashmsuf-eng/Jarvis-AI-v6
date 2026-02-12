package com.jarvis.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * VisionAnalyzer - Image analysis and OCR
 * 
 * Uses ML Kit for text recognition and image analysis.
 */
class VisionAnalyzer(private val context: Context) {
    
    companion object {
        private const val TAG = "VisionAnalyzer"
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extract text from image file
     */
    suspend fun extractText(imagePath: String): TextResult {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                return TextResult(
                    success = false,
                    error = "Image file not found"
                )
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath)
            extractTextFromBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text", e)
            TextResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Extract text from bitmap
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): TextResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = textRecognizer.process(image).await()
            
            val extractedText = visionText.text
            val blocks = visionText.textBlocks.size
            val lines = visionText.textBlocks.sumOf { it.lines.size }
            
            Log.i(TAG, "Extracted $lines lines of text from $blocks blocks")
            
            TextResult(
                success = true,
                text = extractedText,
                blocks = blocks,
                lines = lines,
                message = "Extracted text from image"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from bitmap", e)
            TextResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Extract text from URI
     */
    suspend fun extractTextFromUri(uri: Uri): TextResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = textRecognizer.process(image).await()
            
            val extractedText = visionText.text
            val blocks = visionText.textBlocks.size
            
            TextResult(
                success = true,
                text = extractedText,
                blocks = blocks,
                message = "Extracted text from image"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from URI", e)
            TextResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Analyze screen content (from accessibility service)
     */
    fun analyzeScreenText(screenText: String): ScreenAnalysis {
        return try {
            // Simple heuristic analysis
            val words = screenText.split(Regex("\\s+"))
            val sentences = screenText.split(Regex("[.!?]+"))
            
            // Detect potential UI elements
            val hasButtons = screenText.contains(Regex("(?i)(button|submit|ok|cancel|yes|no|done)"))
            val hasInputs = screenText.contains(Regex("(?i)(enter|type|input|field|search)"))
            val hasLinks = screenText.contains(Regex("(?i)(http|www|link|tap|click)"))
            
            // Detect apps by common keywords
            val detectedApp = detectApp(screenText)
            
            ScreenAnalysis(
                wordCount = words.size,
                sentenceCount = sentences.size,
                hasButtons = hasButtons,
                hasInputFields = hasInputs,
                hasLinks = hasLinks,
                detectedApp = detectedApp,
                summary = generateSummary(screenText, detectedApp)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing screen text", e)
            ScreenAnalysis(
                wordCount = 0,
                sentenceCount = 0,
                summary = "Error analyzing screen"
            )
        }
    }
    
    /**
     * Detect which app is likely being viewed
     */
    private fun detectApp(text: String): String? {
        val lowercaseText = text.lowercase()
        
        return when {
            lowercaseText.contains("whatsapp") -> "WhatsApp"
            lowercaseText.contains("instagram") -> "Instagram"
            lowercaseText.contains("facebook") -> "Facebook"
            lowercaseText.contains("youtube") -> "YouTube"
            lowercaseText.contains("gmail") || lowercaseText.contains("inbox") -> "Gmail"
            lowercaseText.contains("chrome") || lowercaseText.contains("browser") -> "Chrome"
            lowercaseText.contains("settings") -> "Settings"
            else -> null
        }
    }
    
    /**
     * Generate summary of screen content
     */
    private fun generateSummary(text: String, app: String?): String {
        val wordCount = text.split(Regex("\\s+")).size
        
        return buildString {
            if (app != null) {
                append("You appear to be in $app. ")
            }
            
            append("The screen contains $wordCount words")
            
            if (text.contains(Regex("(?i)(message|chat|conversation)"))) {
                append(", likely a conversation or messaging screen")
            } else if (text.contains(Regex("(?i)(settings|preferences|options)"))) {
                append(", likely a settings or configuration screen")
            } else if (text.contains(Regex("(?i)(login|sign in|password)"))) {
                append(", likely a login or authentication screen")
            }
            
            append(".")
        }
    }
    
    /**
     * Format text result for voice output
     */
    fun formatForVoice(textResult: TextResult): String {
        return if (textResult.success && textResult.text != null) {
            if (textResult.text.length > 200) {
                "Extracted text: ${textResult.text.take(200)}... and more"
            } else {
                "Extracted text: ${textResult.text}"
            }
        } else {
            "Could not extract text. ${textResult.error ?: ""}"
        }
    }
    
    /**
     * Close resources
     */
    fun close() {
        textRecognizer.close()
    }
    
    data class TextResult(
        val success: Boolean,
        val text: String? = null,
        val blocks: Int = 0,
        val lines: Int = 0,
        val message: String? = null,
        val error: String? = null
    )
    
    data class ScreenAnalysis(
        val wordCount: Int,
        val sentenceCount: Int,
        val hasButtons: Boolean = false,
        val hasInputFields: Boolean = false,
        val hasLinks: Boolean = false,
        val detectedApp: String? = null,
        val summary: String
    )
}
