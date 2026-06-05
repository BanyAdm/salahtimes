package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent.getIntExtra("prayer_id", 0)

        val wm = context.getSystemService(PowerManager::class.java)
        val wl = wm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SalahTimes::AlarmWake"
        )
        wl.acquire(60_000L)

        val fullScreenIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prayer_name", prayerName)
            putExtra("prayer_arabic", prayerArabic)
            putExtra("prayer_id", prayerId)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, prayerId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "salah_alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(prayerName)
            .setContentText("Time to pray $prayerName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(prayerId, notification)
    }
}
