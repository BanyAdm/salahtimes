package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getIntExtra("prayer_id", 0)
        AlarmReceiver.stopAlarm()
        context.getSystemService(NotificationManager::class.java).cancel(prayerId + 1)
        val closeIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_dismiss", true)
        }
        context.startActivity(closeIntent)
    }
}
