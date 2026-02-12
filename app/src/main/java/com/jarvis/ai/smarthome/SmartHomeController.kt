package com.jarvis.ai.smarthome

import android.content.Context
import android.util.Log
import com.jarvis.ai.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SmartHomeController - Manage smart home devices
 * 
 * Supports HTTP and MQTT protocols for device control.
 * Devices configured in preferences.
 */
class SmartHomeController(
    private val context: Context,
    private val prefManager: PreferenceManager
) {
    
    companion object {
        private const val TAG = "SmartHomeController"
        private const val REQUEST_TIMEOUT = 5000L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    
    // In-memory device registry (would load from preferences in production)
    private val devices = mutableMapOf<String, SmartDevice>()
    
    /**
     * Register a device
     */
    fun registerDevice(device: SmartDevice) {
        devices[device.id] = device
        Log.i(TAG, "Registered device: ${device.name} (${device.type})")
    }
    
    /**
     * Get all devices
     */
    fun getDevices(): List<SmartDevice> = devices.values.toList()
    
    /**
     * Get device by name (fuzzy match)
     */
    fun findDevice(name: String): SmartDevice? {
        val query = name.lowercase()
        return devices.values.firstOrNull { 
            it.name.lowercase().contains(query)
        }
    }
    
    /**
     * Turn device on
     */
    suspend fun turnOn(deviceName: String): ControlResult {
        val device = findDevice(deviceName)
        if (device == null) {
            return ControlResult(
                success = false,
                message = "Device not found: $deviceName"
            )
        }
        
        return executeCommand(device, "on")
    }
    
    /**
     * Turn device off
     */
    suspend fun turnOff(deviceName: String): ControlResult {
        val device = findDevice(deviceName)
        if (device == null) {
            return ControlResult(
                success = false,
                message = "Device not found: $deviceName"
            )
        }
        
        return executeCommand(device, "off")
    }
    
    /**
     * Set device property (brightness, temperature, etc.)
     */
    suspend fun setProperty(
        deviceName: String,
        property: String,
        value: Any
    ): ControlResult {
        val device = findDevice(deviceName)
        if (device == null) {
            return ControlResult(
                success = false,
                message = "Device not found: $deviceName"
            )
        }
        
        device.properties[property] = value
        return executeCommand(device, "set", mapOf(property to value))
    }
    
    /**
     * Execute command on device
     */
    private suspend fun executeCommand(
        device: SmartDevice,
        command: String,
        params: Map<String, Any>? = null
    ): ControlResult = withContext(Dispatchers.IO) {
        return@withContext when (device.protocol) {
            SmartDevice.Protocol.HTTP -> executeHttpCommand(device, command, params)
            SmartDevice.Protocol.MQTT -> executeMqttCommand(device, command, params)
            SmartDevice.Protocol.CUSTOM -> ControlResult(
                success = false,
                message = "Custom protocol not implemented"
            )
        }
    }
    
    /**
     * Execute HTTP command
     */
    private fun executeHttpCommand(
        device: SmartDevice,
        command: String,
        params: Map<String, Any>?
    ): ControlResult {
        return try {
            val url = "http://${device.address}:${device.port}/api/command"
            
            val jsonBody = JSONObject().apply {
                put("command", command)
                put("device", device.id)
                params?.forEach { (key, value) ->
                    put(key, value)
                }
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                // Update device state
                when (command) {
                    "on" -> device.state = SmartDevice.DeviceState.ON
                    "off" -> device.state = SmartDevice.DeviceState.OFF
                }
                
                Log.i(TAG, "Command executed: ${device.name} -> $command")
                ControlResult(
                    success = true,
                    message = "${device.name} $command"
                )
            } else {
                Log.w(TAG, "Command failed: ${response.code}")
                ControlResult(
                    success = false,
                    message = "HTTP error: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing HTTP command", e)
            ControlResult(
                success = false,
                message = "Error: ${e.message}"
            )
        }
    }
    
    /**
     * Execute MQTT command
     */
    private fun executeMqttCommand(
        device: SmartDevice,
        command: String,
        params: Map<String, Any>?
    ): ControlResult {
        // MQTT implementation would go here
        // Requires MQTT client library (not included in dependencies yet)
        return ControlResult(
            success = false,
            message = "MQTT not implemented yet"
        )
    }
    
    /**
     * Quick device actions (common scenarios)
     */
    
    suspend fun lightsOn(): ControlResult {
        val lights = devices.values.filter { it.type == SmartDevice.DeviceType.LIGHT }
        var successCount = 0
        
        lights.forEach { light ->
            val result = executeCommand(light, "on")
            if (result.success) successCount++
        }
        
        return ControlResult(
            success = successCount > 0,
            message = "Turned on $successCount/${lights.size} lights"
        )
    }
    
    suspend fun lightsOff(): ControlResult {
        val lights = devices.values.filter { it.type == SmartDevice.DeviceType.LIGHT }
        var successCount = 0
        
        lights.forEach { light ->
            val result = executeCommand(light, "off")
            if (result.success) successCount++
        }
        
        return ControlResult(
            success = successCount > 0,
            message = "Turned off $successCount/${lights.size} lights"
        )
    }
    
    suspend fun setACTemperature(temp: Int): ControlResult {
        val acs = devices.values.filter { it.type == SmartDevice.DeviceType.AC }
        if (acs.isEmpty()) {
            return ControlResult(
                success = false,
                message = "No AC devices found"
            )
        }
        
        val ac = acs.first()
        return setProperty(ac.name, "temperature", temp)
    }
    
    data class ControlResult(
        val success: Boolean,
        val message: String
    )
}
