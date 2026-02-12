package com.jarvis.ai.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.jarvis.ai.util.PreferenceManager
import com.jarvis.ai.voice.VoiceEngine

/**
 * PhoneStateReceiver - Listens for phone call state changes
 * 
 * Announces incoming calls via voice.
 * From Maya AI - adapted for Jarvis AI.
 */
class PhoneStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PhoneStateReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        try {
            when (intent.action) {
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    handlePhoneStateChange(context, intent)
                }
                Intent.ACTION_NEW_OUTGOING_CALL -> {
                    handleOutgoingCall(context, intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling phone state", e)
        }
    }
    
    private fun handlePhoneStateChange(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                handleIncomingCall(context, incomingNumber ?: "Unknown")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered
                Log.d(TAG, "Call answered")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                Log.d(TAG, "Call ended")
            }
        }
    }
    
    private fun handleIncomingCall(context: Context, number: String) {
        try {
            Log.i(TAG, "Incoming call from $number")
            
            // Get contact name if available
            val contactManager = ContactManager(context)
            val contactName = contactManager.getNameFromNumber(number) ?: number
            
            // Check if voice announcement is enabled
            val prefManager = PreferenceManager(context)
            val autoAnnounceEnabled = false // TODO: Add preference setting
            
            if (autoAnnounceEnabled) {
                // Announce via voice
                val voiceEngine = VoiceEngine(context, prefManager)
                val message = if (contactName == number) {
                    "Sir, ekta call asche $number theke"
                } else {
                    "Sir, ekta call asche $contactName theke"
                }
                
                voiceEngine.speak(message)
                Log.d(TAG, "Incoming call announced via voice")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming call", e)
        }
    }
    
    private fun handleOutgoingCall(context: Context, intent: Intent) {
        val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Outgoing call to $outgoingNumber")
        // Can add logic here if needed
    }
    
}
