package com.jarvis.ai.automation

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * FileController - File management and organization
 * 
 * Lists, moves, copies, deletes, and organizes files.
 */
class FileController(private val context: Context) {
    
    companion object {
        private const val TAG = "FileController"
    }
    
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean,
        val extension: String = ""
    )
    
    /**
     * Get downloads directory
     */
    fun getDownloadsDir(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }
    
    /**
     * Get recent downloads
     */
    fun getRecentDownloads(limit: Int = 10): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        
        try {
            val downloadsDir = getDownloadsDir()
            if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
                Log.w(TAG, "Downloads directory not found")
                return files
            }
            
            val fileList = downloadsDir.listFiles() ?: emptyArray()
            
            files.addAll(
                fileList
                    .filter { it.isFile }
                    .sortedByDescending { it.lastModified() }
                    .take(limit)
                    .map { file ->
                        FileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            isDirectory = false,
                            extension = file.extension
                        )
                    }
            )
            
            Log.d(TAG, "Found ${files.size} recent downloads")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent downloads", e)
        }
        
        return files
    }
    
    /**
     * List files in directory
     */
    fun listFiles(directoryPath: String): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        
        try {
            val dir = File(directoryPath)
            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "Directory not found: $directoryPath")
                return files
            }
            
            val fileList = dir.listFiles() ?: emptyArray()
            
            files.addAll(
                fileList.map { file ->
                    FileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = file.isDirectory,
                        extension = if (file.isFile) file.extension else ""
                    )
                }
            )
            
            Log.d(TAG, "Listed ${files.size} files in $directoryPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
        }
        
        return files
    }
    
    /**
     * Organize downloads by type
     */
    fun organizeDownloads(): OrganizeResult {
        val result = OrganizeResult()
        
        try {
            val downloadsDir = getDownloadsDir()
            if (!downloadsDir.exists()) {
                Log.w(TAG, "Downloads directory not found")
                return result
            }
            
            val fileList = downloadsDir.listFiles() ?: emptyArray()
            
            fileList.filter { it.isFile }.forEach { file ->
                val targetDir = when (file.extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif", "webp", "bmp" -> {
                        File(downloadsDir, "Images")
                    }
                    "mp4", "mkv", "avi", "mov", "wmv", "flv" -> {
                        File(downloadsDir, "Videos")
                    }
                    "mp3", "wav", "flac", "aac", "ogg", "m4a" -> {
                        File(downloadsDir, "Audio")
                    }
                    "pdf" -> {
                        File(downloadsDir, "PDFs")
                    }
                    "doc", "docx", "txt", "rtf", "odt" -> {
                        File(downloadsDir, "Documents")
                    }
                    "zip", "rar", "7z", "tar", "gz" -> {
                        File(downloadsDir, "Archives")
                    }
                    "apk" -> {
                        File(downloadsDir, "APKs")
                    }
                    else -> null
                }
                
                if (targetDir != null) {
                    targetDir.mkdirs()
                    val targetFile = File(targetDir, file.name)
                    
                    if (file.renameTo(targetFile)) {
                        result.organized++
                        Log.d(TAG, "Moved ${file.name} to ${targetDir.name}")
                    } else {
                        result.failed++
                        Log.w(TAG, "Failed to move ${file.name}")
                    }
                }
            }
            
            Log.i(TAG, "Organization complete: ${result.organized} organized, ${result.failed} failed")
        } catch (e: Exception) {
            Log.e(TAG, "Error organizing downloads", e)
        }
        
        return result
    }
    
    /**
     * Clean temp files
     */
    fun cleanTempFiles(): CleanResult {
        val result = CleanResult()
        
        try {
            // Clean app cache
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                result.deletedSize += deleteRecursive(cacheDir, result)
            }
            
            // Clean external cache
            val externalCache = context.externalCacheDir
            if (externalCache != null && externalCache.exists()) {
                result.deletedSize += deleteRecursive(externalCache, result)
            }
            
            Log.i(TAG, "Cleaned ${result.deletedFiles} temp files, freed ${result.deletedSize / 1024 / 1024}MB")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning temp files", e)
        }
        
        return result
    }
    
    /**
     * Delete file or directory recursively
     */
    private fun deleteRecursive(file: File, result: CleanResult): Long {
        var deletedSize = 0L
        
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deletedSize += deleteRecursive(child, result)
                }
            }
            
            val size = if (file.isFile) file.length() else 0
            if (file.delete()) {
                result.deletedFiles++
                deletedSize += size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${file.name}", e)
        }
        
        return deletedSize
    }
    
    /**
     * Get file size in human-readable format
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024} MB"
            else -> "${bytes / 1024 / 1024 / 1024} GB"
        }
    }
    
    /**
     * Format file info for voice output
     */
    fun formatForVoice(files: List<FileInfo>): String {
        if (files.isEmpty()) {
            return "কোন ফাইল নেই" // No files
        }
        
        return files.take(5).joinToString("\n") { file ->
            val sizeStr = formatSize(file.size)
            val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault())
                .format(Date(file.lastModified))
            "${file.name}, $sizeStr, $dateStr"
        }
    }
    
    data class OrganizeResult(
        var organized: Int = 0,
        var failed: Int = 0
    )
    
    data class CleanResult(
        var deletedFiles: Int = 0,
        var deletedSize: Long = 0
    )
}
