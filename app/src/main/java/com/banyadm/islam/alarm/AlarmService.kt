package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.banyadm.islam.R

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent?.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent?.getIntExtra("prayer_id", 0) ?: 0
        val isReminder = intent?.getBooleanExtra("is_reminder", false) ?: false
        val reminderMinutes = intent?.getIntExtra("reminder_minutes", 15) ?: 15

        if (isReminder) {
            // Just a notification, no fullscreen, no athan
            showReminderNotification(prayerName, prayerArabic, prayerId, reminderMinutes)
            stopSelf()
            return START_NOT_STICKY
        }

        // Play athan
        mediaPlayer = MediaPlayer.create(this, R.raw.athan)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener { stopSelf() }

        // Vibrate every 0.5s
        vibrator = getSystemService(Vibrator::class.java)
        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))

        // Dismiss intent for notification button
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("prayer_id", prayerId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            this, prayerId + 200, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fullscreen intent
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
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPending)
            .setOngoing(true)
            .build()

        startForeground(prayerId + 1, notification)
        return START_NOT_STICKY
    }

    private fun showReminderNotification(name: String, arabic: String, id: Int, minutes: Int) {
        val notification = NotificationCompat.Builder(this, "salah_reminder")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$name in $minutes minutes")
            .setContentText("$arabic — prayer time approaching")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        startForeground(id + 50, notification)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(id + 50, notification)
    }

    fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        vibrator?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
