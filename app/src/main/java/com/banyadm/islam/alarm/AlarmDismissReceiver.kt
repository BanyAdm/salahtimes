package com.banyadm.islam.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.e("SALAH_DISMISS", "Dismiss receiver fired! instance=${AlarmService.instance}")
        AlarmService.instance?.stopAlarm()
        val closeIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_dismiss", true)
        }
        context.startActivity(closeIntent)
    }
}
