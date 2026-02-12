package com.jarvis.ai.phone

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * SmsController - SMS management and automation
 * 
 * Handles SMS sending, reading, and management.
 * From Maya AI - adapted for Jarvis AI.
 */
class SmsController(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsController"
    }
    
    private val contentResolver: ContentResolver = context.contentResolver
    private val contactManager = ContactManager(context)
    
    data class SmsMessage(
        val id: String,
        val address: String,
        val body: String,
        val timestamp: Long,
        val isIncoming: Boolean,
        val isRead: Boolean,
        val contactName: String? = null
    )
    
    /**
     * Send SMS by phone number
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        if (!hasSmsPermission()) {
            Log.w(TAG, "No SMS permission")
            return false
        }
        
        return try {
            val smsManager = SmsManager.getDefault()
            
            // Split long messages
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }
            
            Log.i(TAG, "SMS sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            false
        }
    }
    
    /**
     * Send SMS by contact name
     */
    fun sendSmsByName(name: String, message: String): Boolean {
        val contact = contactManager.findByName(name)
        if (contact == null) {
            Log.w(TAG, "Contact not found: $name")
            return false
        }
        
        return sendSms(contact.phoneNumber, message)
    }
    
    /**
     * Get recent SMS messages
     */
    fun getRecentSms(limit: Int = 10, type: SmsType = SmsType.ALL): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        if (!hasSmsPermission()) {
            Log.w(TAG, "No SMS permission")
            return messages
        }
        
        try {
            val selection = when (type) {
                SmsType.INBOX -> "${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}"
                SmsType.SENT -> "${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_SENT}"
                SmsType.ALL -> null
            }
            
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ
                ),
                selection,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val address = it.getString(1)
                    val body = it.getString(2)
                    val date = it.getLong(3)
                    val msgType = it.getInt(4)
                    val isRead = it.getInt(5) == 1
                    
                    val isIncoming = msgType == Telephony.Sms.MESSAGE_TYPE_INBOX
                    val contactName = contactManager.getNameFromNumber(address)
                    
                    messages.add(
                        SmsMessage(
                            id = id,
                            address = address,
                            body = body,
                            timestamp = date,
                            isIncoming = isIncoming,
                            isRead = isRead,
                            contactName = contactName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SMS messages", e)
        }
        
        return messages
    }
    
    /**
     * Get SMS from specific contact
     */
    fun getSmsFromContact(contactName: String, limit: Int = 10): List<SmsMessage> {
        val contact = contactManager.findByName(contactName)
        if (contact == null) {
            Log.w(TAG, "Contact not found: $contactName")
            return emptyList()
        }
        
        val messages = mutableListOf<SmsMessage>()
        
        if (!hasSmsPermission()) {
            Log.w(TAG, "No SMS permission")
            return messages
        }
        
        try {
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ
                ),
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(contact.phoneNumber),
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val address = it.getString(1)
                    val body = it.getString(2)
                    val date = it.getLong(3)
                    val type = it.getInt(4)
                    val isRead = it.getInt(5) == 1
                    
                    messages.add(
                        SmsMessage(
                            id = id,
                            address = address,
                            body = body,
                            timestamp = date,
                            isIncoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX,
                            isRead = isRead,
                            contactName = contactName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SMS from contact", e)
        }
        
        return messages
    }
    
    /**
     * Mark SMS as read
     */
    fun markAsRead(messageId: String): Boolean {
        if (!hasSmsPermission()) return false
        
        return try {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            
            contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId)
            )
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking SMS as read", e)
            false
        }
    }
    
    /**
     * Delete SMS
     */
    fun deleteSms(messageId: String): Boolean {
        if (!hasSmsPermission()) return false
        
        return try {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId)
            ) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting SMS", e)
            false
        }
    }
    
    /**
     * Get unread SMS count
     */
    fun getUnreadCount(): Int {
        if (!hasSmsPermission()) return 0
        
        try {
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}",
                null,
                null
            )
            
            cursor?.use {
                return it.count
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
        }
        
        return 0
    }
    
    /**
     * Format messages for voice output
     */
    fun formatForVoice(messages: List<SmsMessage>): String {
        if (messages.isEmpty()) {
            return "কোন মেসেজ নেই" // No messages
        }
        
        return messages.joinToString("\n") { msg ->
            val sender = msg.contactName ?: msg.address
            val direction = if (msg.isIncoming) "থেকে" else "কে"
            "$sender $direction: ${msg.body}"
        }
    }
    
    enum class SmsType {
        ALL,
        INBOX,
        SENT
    }
    
    /**
     * Check if SMS permissions granted
     */
    private fun hasSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
