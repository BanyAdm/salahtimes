package com.banyadm.islam.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getIntExtra("prayer_id", 0)

        // Stop vibration directly
        context.getSystemService(Vibrator::class.java)?.cancel()

        // Stop audio directly
        context.getSystemService(AudioManager::class.java)
            ?.abandonAudioFocus(null)

        // Cancel notification
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(prayerId + 1)
        nm.cancelAll()

        // Stop service
        context.stopService(Intent(context, AlarmService::class.java))

        // Close alarm screen
        val closeIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_dismiss", true)
        }
        context.startActivity(closeIntent)
    }
}
