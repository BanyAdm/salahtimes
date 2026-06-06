package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent?.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent?.getIntExtra("prayer_id", 0) ?: 0

        val wm = getSystemService(PowerManager::class.java)
        wakeLock = wm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SalahTimes::AlarmWake"
        ).also { it.acquire(60_000L) }

        val fullScreenIntent = Intent(this, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("prayer_name", prayerName)
            putExtra("prayer_arabic", prayerArabic)
            putExtra("prayer_id", prayerId)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, prayerId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "salah_alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(prayerName)
            .setContentText("Time to pray $prayerName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setAutoCancel(true)
            .build()

        startForeground(prayerId + 1, notification)

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(prayerId, notification)

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
