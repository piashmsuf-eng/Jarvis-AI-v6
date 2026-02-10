package com.jarvis.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScheduledTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getStringExtra(EXTRA_TASK) ?: return
        Log.i(TAG, "Scheduled task trigger: $task")

        if (LiveVoiceAgent.isActive) {
            LiveVoiceAgent.instance?.handleScheduledTask(task)
        } else {
            val serviceIntent = Intent(context, LiveVoiceAgent::class.java).apply {
                putExtra(LiveVoiceAgent.EXTRA_SCHEDULED_TASK, task)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "ScheduledTaskReceiver"
        const val EXTRA_TASK = "extra_task"
    }
}
