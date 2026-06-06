package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        var vibrator: Vibrator? = null

        fun stopAlarm() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
            vibrator = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent.getIntExtra("prayer_id", 0)
        val isReminder = intent.getBooleanExtra("is_reminder", false)

        if (isReminder) {
            showReminderNotification(context, prayerName, prayerArabic, prayerId,
                intent.getIntExtra("reminder_minutes", 15))
            return
        }

        // Play athan
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                setDataSource(context.resources.openRawResourceFd(
                    context.resources.getIdentifier("athan", "raw", context.packageName)
                        .also { if (it == 0) return@apply })
                )
                isLooping = false
                prepare()
                start()
                setOnCompletionListener {
                    stopAlarm()
                    context.getSystemService(NotificationManager::class.java)
                        .cancel(prayerId + 1)
                }
            }
        } catch (e: Exception) { }

        // Vibrate
        vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))

        // Dismiss pending intent
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            putExtra("prayer_id", prayerId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, prayerId + 200, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fullscreen intent
        val fullScreenIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPending)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(prayerId + 1, notification)
    }

    private fun showReminderNotification(context: Context, name: String, arabic: String,
                                          id: Int, minutes: Int) {
        val notification = NotificationCompat.Builder(context, "salah_reminder")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$name in $minutes minutes")
            .setContentText("$arabic — prayer time approaching")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id + 50, notification)
    }
}
