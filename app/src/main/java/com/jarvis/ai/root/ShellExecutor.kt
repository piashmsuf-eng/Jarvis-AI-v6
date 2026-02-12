package com.jarvis.ai.root

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ShellExecutor - Root command execution module
 * 
 * Boss-mandated architecture for Runtime.getRuntime().exec("su") capabilities.
 * This prepares the foundation for Gaming Mode, Kernel Tweaks, and System-level automation.
 * 
 * Uses LibSU for robust root access management.
 */
object ShellExecutor {
    
    private const val TAG = "ShellExecutor"
    
    /**
     * Check if device has root access
     */
    fun isRootAvailable(): Boolean {
        return Shell.isAppGrantedRoot() ?: false
    }
    
    /**
     * Request root permissions
     * Returns true if granted
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell() // This triggers the SU request dialog
            val granted = Shell.isAppGrantedRoot() ?: false
            Log.i(TAG, "Root access: ${if (granted) "GRANTED" else "DENIED"}")
            granted
        } catch (e: Exception) {
            Log.e(TAG, "Root request failed", e)
            false
        }
    }
    
    /**
     * Execute single root command
     * Returns command output
     */
    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            if (!isRootAvailable()) {
                return@withContext CommandResult(
                    success = false,
                    output = "",
                    error = "Root access not granted"
                )
            }
            
            val result = Shell.cmd(command).exec()
            
            CommandResult(
                success = result.isSuccess,
                output = result.out.joinToString("\n"),
                error = result.err.joinToString("\n"),
                exitCode = result.code
            )
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: $command", e)
            CommandResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Execute multiple root commands
     * Returns combined results
     */
    suspend fun executeCommands(commands: List<String>): List<CommandResult> = withContext(Dispatchers.IO) {
        commands.map { executeCommand(it) }
    }
    
    /**
     * Gaming Mode Optimization
     * Boss Priority: Prepare device for maximum gaming performance
     */
    suspend fun enableGamingMode(): CommandResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Boss: Enabling Gaming Mode")
        
        val commands = listOf(
            // Kill background processes
            "am kill-all",
            
            // Set CPU governor to performance
            "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
            "echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor",
            
            // Disable thermal throttling (temporary)
            "echo 0 > /sys/class/thermal/thermal_zone0/mode",
            
            // Set GPU to max frequency
            "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
            "echo 10000000 > /sys/class/kgsl/kgsl-3d0/idle_timer",
            
            // Disable swap to reduce latency
            "swapoff /dev/block/zram0",
            
            // Increase I/O scheduler priority
            "echo noop > /sys/block/sda/queue/scheduler"
        )
        
        val results = executeCommands(commands)
        val allSuccess = results.all { it.success }
        
        CommandResult(
            success = allSuccess,
            output = "Gaming Mode: ${if (allSuccess) "ENABLED" else "PARTIAL"}",
            error = results.filter { !it.success }.joinToString("\n") { it.error }
        )
    }
    
    /**
     * Restore Normal Mode
     * Reset system to balanced configuration
     */
    suspend fun disableGamingMode(): CommandResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Boss: Disabling Gaming Mode")
        
        val commands = listOf(
            // Restore CPU governor to schedutil (balanced)
            "echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
            "echo schedutil > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor",
            
            // Re-enable thermal protection
            "echo 1 > /sys/class/thermal/thermal_zone0/mode",
            
            // Restore GPU to auto
            "echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
            
            // Re-enable swap
            "swapon /dev/block/zram0",
            
            // Restore I/O scheduler to cfq (default)
            "echo cfq > /sys/block/sda/queue/scheduler"
        )
        
        val results = executeCommands(commands)
        val allSuccess = results.all { it.success }
        
        CommandResult(
            success = allSuccess,
            output = "Normal Mode: ${if (allSuccess) "RESTORED" else "PARTIAL"}",
            error = results.filter { !it.success }.joinToString("\n") { it.error }
        )
    }
    
    /**
     * Kernel Parameter Tweak
     * Advanced: Modify kernel parameters via sysctl
     */
    suspend fun setKernelParameter(parameter: String, value: String): CommandResult {
        Log.i(TAG, "Boss: Setting kernel parameter: $parameter = $value")
        return executeCommand("sysctl -w $parameter=$value")
    }
    
    /**
     * System Partition Mount (for MT Manager-style operations)
     */
    suspend fun remountSystemRW(): CommandResult {
        return executeCommand("mount -o remount,rw /system")
    }
    
    suspend fun remountSystemRO(): CommandResult {
        return executeCommand("mount -o remount,ro /system")
    }
}

/**
 * Command execution result
 */
data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String = "",
    val exitCode: Int = if (success) 0 else 1
)
