package com.banyadm.islam.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("prayer_name", intent.getStringExtra("prayer_name"))
            putExtra("prayer_arabic", intent.getStringExtra("prayer_arabic"))
            putExtra("prayer_id", intent.getIntExtra("prayer_id", 0))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
