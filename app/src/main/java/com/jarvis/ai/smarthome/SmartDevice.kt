package com.jarvis.ai.smarthome

/**
 * SmartDevice - Base model for smart home devices
 */
data class SmartDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val protocol: Protocol,
    val address: String,  // IP address or MQTT topic
    val port: Int = 80,
    var state: DeviceState = DeviceState.OFF,
    var properties: MutableMap<String, Any> = mutableMapOf()
) {
    enum class DeviceType {
        LIGHT,
        AC,
        TV,
        FAN,
        PLUG,
        SWITCH,
        CURTAIN,
        CAMERA,
        SENSOR,
        CUSTOM
    }
    
    enum class Protocol {
        HTTP,
        MQTT,
        CUSTOM
    }
    
    enum class DeviceState {
        ON,
        OFF,
        UNKNOWN
    }
    
    // Common properties
    fun getBrightness(): Int = properties["brightness"] as? Int ?: 0
    fun setBrightness(value: Int) { properties["brightness"] = value }
    
    fun getTemperature(): Int = properties["temperature"] as? Int ?: 24
    fun setTemperature(value: Int) { properties["temperature"] = value }
    
    fun getColor(): String = properties["color"] as? String ?: "#FFFFFF"
    fun setColor(value: String) { properties["color"] = value }
    
    fun getVolume(): Int = properties["volume"] as? Int ?: 50
    fun setVolume(value: Int) { properties["volume"] = value }
}
