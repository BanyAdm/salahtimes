package com.banyadm.islam.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the service which stops athan + vibration
        context.stopService(Intent(context, AlarmService::class.java))
        // Close alarm screen if open
        val closeIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("auto_dismiss", true)
        }
        context.startActivity(closeIntent)
    }
}
