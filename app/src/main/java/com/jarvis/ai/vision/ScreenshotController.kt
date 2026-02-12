package com.jarvis.ai.vision

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * ScreenshotController - Captures screenshots
 * 
 * Takes screenshots via accessibility service or root commands.
 */
class ScreenshotController(private val context: Context) {
    
    companion object {
        private const val TAG = "ScreenshotController"
    }
    
    /**
     * Take screenshot via accessibility service (Android 9+)
     */
    fun takeScreenshot(): ScreenshotResult {
        return try {
            val a11y = JarvisAccessibilityService.instance
            if (a11y == null) {
                Log.w(TAG, "Accessibility service not available")
                return ScreenshotResult(
                    success = false,
                    error = "Accessibility service not enabled"
                )
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // Use accessibility service screenshot (API 28+)
                val result = a11y.takeScreenshot(
                    JarvisAccessibilityService.TAKE_SCREENSHOT_FULL_DISPLAY,
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R,
                    { executor, callback -> executor.execute { callback.run() } },
                    object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                            Log.i(TAG, "Screenshot taken successfully")
                        }
                        
                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "Screenshot failed: $errorCode")
                        }
                    }
                )
                
                return ScreenshotResult(
                    success = result,
                    message = if (result) "Screenshot taken" else "Screenshot failed"
                )
            } else {
                // Fall back to root method
                return takeScreenshotViaRoot()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot", e)
            return ScreenshotResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Take screenshot via root command
     */
    private fun takeScreenshotViaRoot(): ScreenshotResult {
        return try {
            if (!Shell.getShell().isRoot) {
                return ScreenshotResult(
                    success = false,
                    error = "Root access required for screenshots on Android < 9"
                )
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "jarvis_screenshot_$timestamp.png"
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val jarvisDir = File(picturesDir, "Jarvis")
            jarvisDir.mkdirs()
            
            val filePath = File(jarvisDir, fileName).absolutePath
            
            // Execute screencap command
            val result = Shell.cmd("screencap -p $filePath").exec()
            
            if (result.isSuccess) {
                // Notify media scanner
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filePath),
                    arrayOf("image/png"),
                    null
                )
                
                Log.i(TAG, "Screenshot saved: $filePath")
                return ScreenshotResult(
                    success = true,
                    filePath = filePath,
                    message = "Screenshot saved to $fileName"
                )
            } else {
                return ScreenshotResult(
                    success = false,
                    error = "Screencap command failed"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot via root", e)
            return ScreenshotResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Save bitmap to file
     */
    fun saveBitmap(bitmap: Bitmap): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "jarvis_screenshot_$timestamp.png"
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val jarvisDir = File(picturesDir, "Jarvis")
            jarvisDir.mkdirs()
            
            val file = File(jarvisDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Notify media scanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
            
            Log.i(TAG, "Bitmap saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            null
        }
    }
    
    data class ScreenshotResult(
        val success: Boolean,
        val filePath: String? = null,
        val message: String? = null,
        val error: String? = null
    )
}
