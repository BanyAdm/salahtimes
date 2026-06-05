package com.banyadm.islam.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.banyadm.islam.data.Prayer
import com.banyadm.islam.data.PrayerTimes
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {

    fun scheduleAll(
        context: Context,
        times: PrayerTimes,
        toggles: Map<Prayer, Boolean>
    ) {
        cancelAll(context)
        val prayers = mapOf(
            Prayer.FAJR to times.fajr,
            Prayer.DHUHR to times.dhuhr,
            Prayer.ASR to times.asr,
            Prayer.MAGHRIB to times.maghrib,
            Prayer.ISHA to times.isha
        )
        prayers.forEach { (prayer, time) ->
            if (toggles[prayer] == true) {
                scheduleOne(context, prayer, time)
            }
        }
    }

    private fun scheduleOne(context: Context, prayer: Prayer, time: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayer.displayName)
            putExtra("prayer_arabic", prayer.arabic)
            putExtra("prayer_id", prayer.ordinal)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = getTimeMillis(time)
        if (triggerTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        Prayer.entries.forEach { prayer ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayer.ordinal,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun getTimeMillis(time: String): Long {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val parsed = sdf.parse(time) ?: return 0L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.hours)
            set(Calendar.MINUTE, parsed.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
