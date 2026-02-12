package com.jarvis.ai.phone

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * ContactManager - Manages phone contacts
 * 
 * Provides contact lookup, search, and management functionality.
 */
class ContactManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ContactManager"
    }
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    data class Contact(
        val id: String,
        val name: String,
        val phoneNumber: String,
        val displayNumber: String = phoneNumber
    )
    
    /**
     * Find contact by name (fuzzy search)
     */
    fun findByName(name: String): Contact? {
        if (!hasContactsPermission()) {
            Log.w(TAG, "No contacts permission")
            return null
        }
        
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getString(0)
                    val contactName = it.getString(1)
                    val phoneNumber = it.getString(2)
                    return Contact(
                        id = id,
                        name = contactName,
                        phoneNumber = phoneNumber.replace(Regex("[^0-9+]"), ""),
                        displayNumber = phoneNumber
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding contact", e)
        }
        
        return null
    }
    
    /**
     * Get all contacts
     */
    fun getAllContacts(limit: Int = 100): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        if (!hasContactsPermission()) {
            Log.w(TAG, "No contacts permission")
            return contacts
        }
        
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val name = it.getString(1)
                    val number = it.getString(2)
                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phoneNumber = number.replace(Regex("[^0-9+]"), ""),
                            displayNumber = number
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contacts", e)
        }
        
        return contacts
    }
    
    /**
     * Search contacts by query
     */
    fun search(query: String, limit: Int = 10): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        if (!hasContactsPermission()) {
            Log.w(TAG, "No contacts permission")
            return contacts
        }
        
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf("%$query%", "%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val name = it.getString(1)
                    val number = it.getString(2)
                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phoneNumber = number.replace(Regex("[^0-9+]"), ""),
                            displayNumber = number
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching contacts", e)
        }
        
        return contacts
    }
    
    /**
     * Get contact name from phone number
     */
    fun getNameFromNumber(phoneNumber: String): String? {
        if (!hasContactsPermission()) return null
        
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(cleanNumber),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting name from number", e)
        }
        
        return null
    }
    
    /**
     * Check if contacts permission granted
     */
    private fun hasContactsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
