package com.jarvis.ai.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.jarvis.ai.util.PreferenceManager
import com.jarvis.ai.voice.VoiceEngine

/**
 * SmsReceiver - Listens for incoming SMS messages
 * 
 * Announces SMS arrival via voice.
 * From Maya AI - adapted for Jarvis AI.
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages.forEach { smsMessage ->
                handleIncomingSms(context, smsMessage)
            }
        }
    }
    
    private fun handleIncomingSms(context: Context, smsMessage: SmsMessage) {
        try {
            val sender = smsMessage.originatingAddress ?: "Unknown"
            val content = smsMessage.messageBody ?: ""
            
            Log.i(TAG, "SMS received from $sender: $content")
            
            // Get contact name if available
            val contactManager = ContactManager(context)
            val contactName = contactManager.getNameFromNumber(sender) ?: sender
            
            // Check if voice announcement is enabled
            val prefManager = PreferenceManager(context)
            val autoReadEnabled = prefManager.getBoolean("auto_read_sms", false)
            
            if (autoReadEnabled) {
                // Announce via voice
                val voiceEngine = VoiceEngine(context, prefManager)
                val message = if (contactName == sender) {
                    "Sir, ekta SMS esheche $sender theke. Message: $content"
                } else {
                    "Sir, ekta SMS esheche $contactName theke. Message: $content"
                }
                
                voiceEngine.speak(message)
                Log.d(TAG, "SMS announced via voice")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming SMS", e)
        }
    }
    
    /**
     * Helper for PreferenceManager
     */
    private fun PreferenceManager.getBoolean(key: String, default: Boolean): Boolean {
        // Use reflection or add method to PreferenceManager
        return default // For now, default disabled
    }
}
