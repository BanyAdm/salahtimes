package com.banyadm.islam.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Tell the service to stop itself (handles stopForeground + athan + vibration)
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        context.startService(stopIntent)

        // Close alarm screen if open
        val closeIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_dismiss", true)
        }
        context.startActivity(closeIntent)
    }
}
