package com.jarvis.ai.phone

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService

/**
 * CallController - Phone call management
 * 
 * Handles making calls, ending calls, and call history.
 * From Maya AI - adapted for Jarvis AI.
 */
class CallController(private val context: Context) {
    
    companion object {
        private const val TAG = "CallController"
    }
    
    private val contentResolver: ContentResolver = context.contentResolver
    private val telecomManager = context.getSystemService<TelecomManager>()
    private val contactManager = ContactManager(context)
    
    data class CallInfo(
        val number: String,
        val type: CallType,
        val timestamp: Long,
        val duration: Int,
        val contactName: String? = null
    )
    
    enum class CallType {
        INCOMING,
        OUTGOING,
        MISSED,
        REJECTED,
        BLOCKED,
        UNKNOWN
    }
    
    /**
     * Make a phone call
     */
    fun makeCall(phoneNumber: String): Boolean {
        if (!hasCallPermission()) {
            Log.w(TAG, "No call permission")
            return false
        }
        
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Call initiated to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make call", e)
            false
        }
    }
    
    /**
     * Make call by contact name
     */
    fun makeCallByName(name: String): Boolean {
        val contact = contactManager.findByName(name)
        if (contact == null) {
            Log.w(TAG, "Contact not found: $name")
            return false
        }
        
        return makeCall(contact.phoneNumber)
    }
    
    /**
     * End current call (requires Android 9+ or root)
     */
    fun endCall(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "End call requires Android 9+")
            return false
        }
        
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ANSWER_PHONE_CALLS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                telecomManager?.endCall()
                Log.i(TAG, "Call ended")
                true
            } else {
                Log.w(TAG, "No answer phone calls permission")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            false
        }
    }
    
    /**
     * Get call history
     */
    fun getCallHistory(limit: Int = 10, type: CallType? = null): List<CallInfo> {
        val calls = mutableListOf<CallInfo>()
        
        if (!hasCallLogPermission()) {
            Log.w(TAG, "No call log permission")
            return calls
        }
        
        try {
            val selection = when (type) {
                CallType.INCOMING -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.INCOMING_TYPE}"
                CallType.OUTGOING -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
                CallType.MISSED -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
                CallType.REJECTED -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.REJECTED_TYPE}"
                CallType.BLOCKED -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.BLOCKED_TYPE}"
                else -> null
            }
            
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                selection,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(0) ?: "Unknown"
                    val callType = it.getInt(1)
                    val date = it.getLong(2)
                    val duration = it.getInt(3)
                    
                    val contactName = contactManager.getNameFromNumber(number)
                    
                    calls.add(
                        CallInfo(
                            number = number,
                            type = mapCallType(callType),
                            timestamp = date,
                            duration = duration,
                            contactName = contactName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting call history", e)
        }
        
        return calls
    }
    
    /**
     * Get missed calls count
     */
    fun getMissedCallsCount(): Int {
        if (!hasCallLogPermission()) return 0
        
        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.NEW} = 1",
                null,
                null
            )
            
            cursor?.use {
                return it.count
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting missed calls count", e)
        }
        
        return 0
    }
    
    /**
     * Get last call info
     */
    fun getLastCall(): CallInfo? {
        val history = getCallHistory(limit = 1)
        return history.firstOrNull()
    }
    
    /**
     * Format call history for voice output
     */
    fun formatForVoice(calls: List<CallInfo>): String {
        if (calls.isEmpty()) {
            return "কোন কল হিস্ট্রি নেই" // No call history
        }
        
        return calls.joinToString("\n") { call ->
            val name = call.contactName ?: call.number
            val typeStr = when (call.type) {
                CallType.INCOMING -> "ইনকামিং"
                CallType.OUTGOING -> "আউটগোয়িং"
                CallType.MISSED -> "মিস্ড"
                CallType.REJECTED -> "রিজেক্টেড"
                CallType.BLOCKED -> "ব্লকড"
                CallType.UNKNOWN -> "অজানা"
            }
            val durationMin = call.duration / 60
            val durationSec = call.duration % 60
            
            if (call.duration > 0) {
                "$typeStr কল $name, সময়: $durationMin মিনিট $durationSec সেকেন্ড"
            } else {
                "$typeStr কল $name"
            }
        }
    }
    
    /**
     * Map Android call type to our enum
     */
    private fun mapCallType(androidType: Int): CallType {
        return when (androidType) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
            else -> CallType.UNKNOWN
        }
    }
    
    /**
     * Check if call permission granted
     */
    private fun hasCallPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if call log permission granted
     */
    private fun hasCallLogPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
}
