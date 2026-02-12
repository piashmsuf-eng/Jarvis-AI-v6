package com.jarvis.ai.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.jarvis.ai.accessibility.JarvisAccessibilityService
import com.jarvis.ai.phone.ContactManager
import kotlinx.coroutines.delay

/**
 * WhatsAppController - WhatsApp automation via Accessibility Service
 * 
 * Sends messages, reads messages, and navigates WhatsApp UI.
 */
class WhatsAppController(private val context: Context) {
    
    companion object {
        private const val TAG = "WhatsAppController"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        
        // WhatsApp UI element IDs
        private const val WA_MESSAGE_LIST = "com.whatsapp:id/list"
        private const val WA_INPUT_FIELD = "com.whatsapp:id/entry"
        private const val WA_SEND_BUTTON = "com.whatsapp:id/send"
        private const val WA_CONTACT_NAME = "com.whatsapp:id/conversation_contact_name"
    }
    
    private val contactManager = ContactManager(context)
    
    /**
     * Send WhatsApp message by contact name
     */
    suspend fun sendMessage(contactName: String, message: String): Boolean {
        return try {
            // Find contact
            val contact = contactManager.findByName(contactName)
            if (contact == null) {
                Log.w(TAG, "Contact not found: $contactName")
                return false
            }
            
            // Open WhatsApp chat via intent
            val phoneNumber = contact.phoneNumber.replace("+", "").replace(" ", "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
                `package` = WHATSAPP_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Log.d(TAG, "Opened WhatsApp chat with $contactName")
            
            // Wait for WhatsApp to open
            delay(2000)
            
            // Use accessibility service to type and send
            val a11y = JarvisAccessibilityService.instance
            if (a11y != null) {
                // Type message
                val typed = a11y.typeText(message)
                if (!typed) {
                    Log.w(TAG, "Failed to type message")
                    return false
                }
                
                delay(500)
                
                // Click send button
                val sent = a11y.clickNodeByText("Send") || 
                          a11y.clickNodeById(WA_SEND_BUTTON)
                
                if (sent) {
                    Log.i(TAG, "WhatsApp message sent to $contactName")
                    return true
                } else {
                    Log.w(TAG, "Failed to click send button")
                    return false
                }
            } else {
                Log.w(TAG, "Accessibility service not available")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message", e)
            false
        }
    }
    
    /**
     * Read recent WhatsApp messages from screen
     */
    fun readRecentMessages(count: Int = 5): List<String> {
        val messages = mutableListOf<String>()
        
        try {
            val a11y = JarvisAccessibilityService.instance
            if (a11y == null) {
                Log.w(TAG, "Accessibility service not available")
                return messages
            }
            
            // Check if we're in WhatsApp
            if (a11y.currentPackage != WHATSAPP_PACKAGE && 
                a11y.currentPackage != WHATSAPP_BUSINESS) {
                Log.w(TAG, "Not in WhatsApp")
                return messages
            }
            
            // Read screen text (messages visible)
            val screenText = a11y.readScreenTextFlat()
            val lines = screenText.split("\n").filter { it.isNotBlank() }
            
            // Take last N lines as messages
            messages.addAll(lines.takeLast(count))
            
            Log.d(TAG, "Read ${messages.size} WhatsApp messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WhatsApp messages", e)
        }
        
        return messages
    }
    
    /**
     * Open WhatsApp chat with contact
     */
    fun openChat(contactName: String): Boolean {
        return try {
            val contact = contactManager.findByName(contactName)
            if (contact == null) {
                Log.w(TAG, "Contact not found: $contactName")
                return false
            }
            
            val phoneNumber = contact.phoneNumber.replace("+", "").replace(" ", "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
                `package` = WHATSAPP_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Log.i(TAG, "Opened WhatsApp chat with $contactName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp chat", e)
            false
        }
    }
    
    /**
     * Check if WhatsApp is installed
     */
    fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo(WHATSAPP_BUSINESS, 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    /**
     * Format messages for voice output
     */
    fun formatForVoice(messages: List<String>): String {
        if (messages.isEmpty()) {
            return "কোন হোয়াটসঅ্যাপ মেসেজ নেই" // No WhatsApp messages
        }
        
        return messages.joinToString("\n")
    }
}
