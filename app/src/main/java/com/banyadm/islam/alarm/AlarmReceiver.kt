package com.banyadm.islam.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent.getIntExtra("prayer_id", 0)

        // Wake the screen
        val pm = context.getSystemService(PowerManager::class.java)
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SalahTimes::AlarmWake"
        )
        wl.acquire(60_000L)

        // Launch fullscreen alarm activity
        val alarmIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prayer_name", prayerName)
            putExtra("prayer_arabic", prayerArabic)
            putExtra("prayer_id", prayerId)
        }
        context.startActivity(alarmIntent)
    }
}
